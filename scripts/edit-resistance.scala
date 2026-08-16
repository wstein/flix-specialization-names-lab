#!/usr/bin/env -S scala-cli shebang
//> using scala "2.13.16"

/*
 * Copyright 2026 Werner Stein
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ca.uwaterloo.flix.tools

import ca.uwaterloo.flix.api.{CompilerConstants, Flix}
import ca.uwaterloo.flix.language.ast.shared.SecurityContext
import ca.uwaterloo.flix.util.{Build, Options, Validation}

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path, Paths}
import java.security.MessageDigest

/**
  * Measures how many generated class names, and how many generated classes, survive an
  * edit to the source.
  *
  * A name that survives an edit can be cached, diffed, and reused across builds; one that
  * does not forces downstream work even though nothing it describes has changed. Both are
  * reported, because a path can survive while the class at it changes: with names keyed on
  * an occurrence index, inserting a lambda can make a different lambda inherit an earlier
  * path. Name survival alone would read that reassignment as reuse.
  *
  * The two columns answer different questions. Names measure whether the naming scheme is
  * edit-resistant. Bytes measure whether an artifact could be reused as is, and are moved
  * by the `LineNumberTable` as well as by code: an edit that shifts lines changes the bytes
  * of everything generated below it, which is why a comment at the top of a file moves far
  * more of them than a definition added at the end.
  *
  * Run with:
  *
  * {{{
  *   ./scripts/edit-resistance [source.flix]
  *   ./scripts/edit-resistance --flix-jar path/to/flix.jar [source.flix]
  * }}}
  *
  * `scripts/edit-resistance` resolves a Flix compiler jar -- from `--flix-jar`, `$FLIX_JAR`,
  * `./flix.jar`, or `flix` on `$PATH`, deliberately never through `flixw` (which holds only
  * one pinned jar at a time; this needs to compare arbitrary builds against each other) --
  * then hands it to `scala-cli` as `--jar`. A `using jar` directive cannot be computed at
  * build time, so this script alone has no classpath. Running
  * `scala-cli run scripts/edit-resistance.scala` directly fails to compile with "object api
  * is not a member of package ca.uwaterloo.flix" for that reason.
  *
  * The compiler runs in this process rather than as a subprocess, so no assembled jar and
  * no scratch project are needed, and the whole sweep costs one compile per perturbation.
  *
  * `--compiler-version <string>` labels the report with what produced it. Nothing here can
  * read that back from the jar itself (verified by hand: neither a jar's manifest nor its
  * class files record it), so the wrapper script fills it in from the resolved jar's
  * filename or path when not given explicitly.
  */
object EditResistance {

  /**
    * A named edit to the source.
    *
    * @param label  how the edit is described in the report.
    * @param apply  rewrites the source, or returns `None` if its anchor is missing.
    */
  private case class Perturbation(label: String, apply: String => Option[String])

  /**
    * The default program to measure.
    */
  private val DefaultSource: Path = Paths.get("src/Main.flix")

  def main(args: Array[String]): Unit = {
    val (compilerVersion, positional) = parseArgs(args.toList)
    val path = positional.headOption.map(Paths.get(_)).getOrElse(DefaultSource)
    if (!Files.isRegularFile(path)) {
      Console.err.println(s"Error: no such file: $path")
      System.exit(1)
    }
    val source = new String(Files.readAllBytes(path), StandardCharsets.UTF_8)

    println(s"Compiler: ${compilerVersion.getOrElse("unspecified")}")
    println("Baseline")
    val base = compile(source)
    val again = compile(source)
    report("rebuild, no change", base, again)
    if (base != again) {
      Console.err.println("Error: clean builds differ; edit-resistance results would be inconclusive.")
      System.exit(1)
    }

    println()
    println("Perturbations")
    for (p <- perturbations) {
      p.apply(source) match {
        case None => println(f"  ${p.label}%-34s anchor missing, skipped")
        case Some(edited) => report(p.label, base, compile(edited))
      }
    }

    println()
    println("A surviving name can be reused only when its class bytes also survive; both are shown.")
    println()
    println("Bytecode carries a LineNumberTable, so an edit that shifts lines changes the bytes of")
    println("every class generated from below it while renaming none. That is why adding a comment")
    println("at the top moves far more bytes than adding a definition at the end, which moves none.")
    println("Read the bytes column as 'would a content-addressed cache hit', not as instability.")
  }

  /**
    * Returns the generated classes of `source`, each mapped to a digest of its bytecode.
    */
  private def compile(source: String): Map[String, String] = {
    implicit val sctx: SecurityContext = SecurityContext.Unrestricted
    val flix = new Flix()
    // threads = 1: Options.Default runs specialization across every available core, and
    // sequential-counter IDs are handed out in whatever order threads reach
    // genSym.freshId() -- so two clean compiles of the same byte-identical source can
    // already disagree on names before any perturbation is applied. Forcing one thread
    // makes id assignment order deterministic, so a later diff reflects the edit, not
    // scheduling noise.
    //
    // build = Production: Options.Default is Development, but neither stock flix/flix
    // nor this lab's fork actually switch build-jar/build-fatjar to Production -- both
    // hardcode Development regardless of command. Measuring under Production here is a
    // deliberate choice, not a match to what those commands do today: it is the build a
    // shipped artifact would actually use, and is the mode this lab cares about.
    flix.setOptions(Options.Default.copy(progress = false, incremental = false, threads = 1, build = Build.Development))
    flix.addVirtualPath(CompilerConstants.VirtualTestFile, source)
    flix.compile() match {
      case Validation.Success(result) =>
        result.getClasses.map { case (name, clazz) => render(name.pkg, name.name) -> digest(clazz.bytecode) }
      case Validation.Failure(errors) =>
        Console.err.println(s"Error: the program does not compile: ${errors.head}")
        System.exit(1)
        Map.empty
    }
  }

  /**
    * Returns the fully qualified name of a generated class.
    */
  private def render(pkg: List[String], name: String): String =
    if (pkg.isEmpty) name else pkg.mkString("/") + "/" + name

  /**
    * Returns a digest of `bytes`, so that two classes are compared by content rather than
    * by holding every class file of every run in memory.
    */
  private def digest(bytes: Array[Byte]): String = {
    val d = MessageDigest.getInstance("SHA-256").digest(bytes)
    d.take(8).map(b => f"${b & 0xff}%02x").mkString
  }

  /**
    * Reports what survived from `before` to `after`.
    */
  private def report(label: String, before: Map[String, String], after: Map[String, String]): Unit = {
    val shared = before.keySet.intersect(after.keySet)
    val sameBytes = shared.count(k => before(k) == after(k))
    val added = after.keySet.diff(before.keySet).size
    val removed = before.keySet.diff(after.keySet).size
    val namePct = if (before.isEmpty) 100.0 else 100.0 * shared.size / before.size
    val bytePct = if (before.isEmpty) 100.0 else 100.0 * sameBytes / before.size
    val effect =
      if (before == after) "output unchanged"
      else s"+$added -$removed classes, ${shared.size - sameBytes} changed at same name"
    println(f"  $label%-34s names $namePct%6.2f%%  bytes $bytePct%6.2f%%   ($effect)")
  }

  /**
    * Splits `args` into an optional --compiler-version value and the remaining positional
    * arguments. Compiled classes never record which compiler produced them (neither a
    * jar's manifest nor its class files do -- verified by hand), so this is caller-supplied
    * labeling only; the wrapper script fills it in from the resolved jar's own filename or
    * path when not given explicitly.
    */
  private def parseArgs(args: List[String]): (Option[String], List[String]) = {
    var compilerVersion: Option[String] = None
    val positional = List.newBuilder[String]
    var remaining = args
    while (remaining.nonEmpty) remaining match {
      case "--compiler-version" :: value :: tail => compilerVersion = Some(value); remaining = tail
      case other :: tail => positional += other; remaining = tail
      case Nil => ()
    }
    (compilerVersion, positional.result())
  }

  /**
    * Replaces the first occurrence of `target` in `s`, or returns `None` if it is absent.
    *
    * An edit whose anchor has drifted would otherwise silently measure nothing, and score
    * a perfect result for doing nothing at all.
    */
  private def replaceFirst(s: String, target: String, replacement: String): Option[String] =
    if (s.contains(target)) Some(s.replaceFirst(java.util.regex.Pattern.quote(target), java.util.regex.Matcher.quoteReplacement(replacement))) else None

  /**
    * The edits to measure.
    */
  private def perturbations: List[Perturbation] = List(
    Perturbation("add a comment", s => Some("// an added comment\n" + s)),

    Perturbation("add a blank line inside a def", s =>
      replaceFirst(s, "def boxDemo(): String = region rc {", "def boxDemo(): String = region rc {\n")),

    Perturbation("rename a local variable", s =>
      replaceFirst(s, "let pipeRes =", "let pipeResult =")
        .flatMap(replaceFirst(_, "${pipeRes}", "${pipeResult}"))),

    Perturbation("add an unrelated def at the end", s =>
      Some(s + "\ndef unrelatedAddition(x: Int32): Int32 = x + 1\n")),

    // The direct test of renumbering: `liftedClosures` has two lambdas lifted out of it,
    // and this adds a third ahead of both. Getting a lambda to survive that far takes some
    // care — it must capture, so it becomes a closure, and escape, so it is not inlined
    // into its only use. Returning it does both.
    Perturbation("insert a lifted closure ahead of two", s =>
      replaceFirst(s,
        """def liftedClosures(n: Int32): (Int32 -> Int32, Int32 -> Int32) =
    let f = x -> x + n;
    let g = y -> y * n;
    (f, g)""",
        """def liftedClosures(n: Int32): (Int32 -> Int32, Int32 -> Int32, Int32 -> Int32) =
    let h = z -> z - n;
    let f = x -> x + n;
    let g = y -> y * n;
    (h, f, g)""")
        .flatMap(replaceFirst(_, "let (f, g) = liftedClosures(3);", "let (h, f, g) = liftedClosures(3);"))
        .flatMap(replaceFirst(_, "f(10) + g(10)", "h(10) + f(10) + g(10)"))),

    // The inserted lambda has to reach code generation, so its result is used. An unused
    // binding is optimized away and would leave the output untouched, measuring nothing.
    Perturbation("insert a lambda earlier in a def", s =>
      replaceFirst(s, "def repeatedStdlibDemo(): Int32 = {",
        "def repeatedStdlibDemo(): Int32 = {\n    let z = List.map(x -> x + 100, 1 :: Nil) |> List.length;")
        .flatMap(replaceFirst(_, "    a + b + c + d + e + f + g + h + i\n",
          "    a + b + c + d + e + f + g + h + i + z\n"))),

    // This changes the specialization worklist directly: `List.map` already has several
    // concrete instantiations, and this adds one more at Int8. Existing specializations
    // should keep both their names and their bytecode.
    Perturbation("add a List.map specialization", s =>
      replaceFirst(s, "    a + b + c + d + e + f + g + h + i\n",
        "    let j = List.map(x -> x + 1i8, 1i8 :: 2i8 :: Nil) |> List.length;\n    a + b + c + d + e + f + g + h + i + j\n"))
  )

}
