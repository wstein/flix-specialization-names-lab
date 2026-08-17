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
import ca.uwaterloo.flix.util.{Options, Validation}

/**
  * Tries to actually trigger a stable-name collision, rather than reason about one from the
  * birthday-bound math alone.
  *
  * At the default 12-character width, a collision needs on the order of `sqrt(36^12)` ≈
  * 2.6 billion specializations -- not reachable here. `--Xstable-name-length` (this lab's
  * ADR documents it in `docs/adr/0001-content-addressed-specialization-names.md`) narrows
  * that width, and at a narrow enough width the birthday bound comes down to a few hundred
  * specializations, which is reachable: generate that many distinct specializations of one
  * generic def, compile at that width, and see what actually happens.
  *
  * The ADR's Collision policy section documents a full-symbol collision guard that should
  * fail the build rather than let two different specializations silently share a class. This
  * script exists to check that claim empirically rather than take it on faith -- and it is
  * exactly what closes the open question the ADR's own `--Xstable-name-length` section
  * leaves: "whether the full-symbol collision guard ... still catches two different ids that
  * happen to truncate to the same short name is not established here."
  *
  * Run with:
  *
  * {{{
  *   ./scripts/collision-stress
  *   ./scripts/collision-stress --width 2 --count 200
  *   ./scripts/collision-stress --flix-jar path/to/flix.jar
  * }}}
  *
  * `scripts/collision-stress` resolves a Flix compiler jar the same way `scripts/edit-resistance`
  * does -- from `--flix-jar`, `$FLIX_JAR`, `./flix.jar`, or `flix` on `$PATH`, never through
  * `flixw` -- then hands it to `scala-cli` as `--jar`, for the same reason: this script alone
  * has no classpath, and running it with `scala-cli run` directly fails to compile. The
  * wrapper also passes `-J -Xss128m`: `main()`'s generated body chains every specialization's
  * result into one expression, so a large `--count` becomes a deeply nested AST that some
  * recursive compiler phase walks non-tail-recursively -- a default JVM thread stack
  * overflows well before `--count` reaches the hundreds a real run needs. Running
  * `scala-cli run scripts/collision-stress.scala` directly skips that flag too.
  *
  * This covers only the def-specialization family (`SpecializationKey`) directly, though a
  * collision on a derived def (`Deriver`, e.g. a generated `ToString.toString`) can surface
  * as a side effect of specializing the marker types this generates, and has been observed
  * to. It generates one polymorphic `collisionStressProbe` def and calls it at `count`
  * distinct nominal types, each specialization getting its own class named
  * `Def$collisionStressProbe$<suffix>`; a collision shows up as fewer distinct suffixes than
  * specializations requested, whether or not the compiler also reports it.
  */
object CollisionStress {

  private case class Config(width: Int, count: Option[Int], compilerVersion: Option[String])

  def main(args: Array[String]): Unit = {
    val config = parseArgs(args.toList)
    val width = config.width
    // Birthday bound for 99.9% collision probability at this width: n such that
    // 1 - exp(-n^2 / (2 * 36^width)) >= 0.999, i.e. n >= sqrt(ln(1000) * 2 * 36^width).
    val defaultCount = math.ceil(math.sqrt(math.log(1000) * 2 * math.pow(36, width))).toInt
    val count = config.count.getOrElse(defaultCount)
    val expectedP = 1.0 - math.exp(-(count.toDouble * count) / (2.0 * math.pow(36, width)))

    println(s"Compiler: ${config.compilerVersion.getOrElse("unspecified")}")
    println(s"Stable-id width: $width")
    println(s"Distinct specializations: $count")
    println(f"Birthday-bound collision probability at this width and count: $expectedP%.4f")
    println()

    val source = generateSource(count)

    implicit val sctx: SecurityContext = SecurityContext.Unrestricted
    val flix = new Flix()
    // threads = 1, same reason as scripts/edit-resistance: parallel specialization can
    // itself introduce nondeterminism unrelated to the collision this is trying to measure.
    flix.setOptions(Options.Default.copy(progress = false, incremental = false, threads = 1, xstableNameLength = width))
    flix.addVirtualPath(CompilerConstants.VirtualTestFile, source)

    try {
      flix.compile() match {
        case Validation.Success(result) =>
          val suffixes = result.getClasses.keys.flatMap { name =>
            val full = render(name.pkg, name.name)
            if (full.startsWith("Def$collisionStressProbe$")) Some(full.stripPrefix("Def$collisionStressProbe$")) else None
          }.toSet
          println("Compiled successfully.")
          println(s"  specializations requested: $count")
          println(s"  distinct class names found:  ${suffixes.size}")
          println()
          if (suffixes.size < count) {
            println(s"COLLISION, NOT CAUGHT: ${count - suffixes.size} specialization(s) share a class name")
            println("with another. The compile succeeded anyway, so one silently overwrote another in the")
            println("compiler's own output map -- this answers the ADR's open question: at this width, the")
            println("full-symbol collision guard did not catch it.")
            System.exit(1)
          } else {
            println("No collision this run. The birthday bound is a probability, not a guarantee -- rerun,")
            println("or pass a narrower --width or a larger --count, to try again.")
          }
        case Validation.Failure(errors) =>
          println("Compile FAILED.")
          println(s"First error: ${errors.head}")
          println()
          println("This may be the full-symbol collision guard firing (see the ADR's Collision policy")
          println("section) -- or an unrelated compile error. Read the message above to tell which.")
          System.exit(1)
      }
    } catch {
      case e: Throwable =>
        println(s"Compile THREW: ${e.getClass.getName}: ${e.getMessage}")
        println()
        println("This is likely the collision guard firing as an InternalCompilerException (per the")
        println("ADR's Collision policy section) rather than a graceful Validation.Failure -- the ADR")
        println("describes it as thrown, not returned as a compile error.")
        System.exit(1)
    }
  }

  /**
    * A source with one polymorphic `collisionStressProbe` def called at `count` distinct
    * nominal types, so that each call site forces its own specialization. `Mi` are trivial
    * single-case enums, distinct only in name -- what matters is that they are `count`
    * genuinely different types, not what they contain.
    *
    * The probe formats its argument via a `with ToString[a]` constraint rather than just
    * returning it (`x: a): a = x` was the first attempt here): a plain identity function is
    * trivial enough that the optimizer inlines it away entirely, before it ever reaches a
    * class name to collide on. `AllConstructs.flix`'s own `describeShape` already proves this
    * `with ToString[a]` shape survives to a distinct class per instantiation; this mirrors it.
    */
  private def generateSource(count: Int): String = {
    val sb = new StringBuilder
    sb.append("""def collisionStressProbe(x: a, label: String): String with ToString[a] = "${label}: ${x}"""" + "\n\n")
    for (i <- 0 until count) {
      sb.append(s"enum M$i with ToString { case X }\n")
      sb.append(s"""def use$i(): String = collisionStressProbe(M$i.X, "$i")""" + "\n\n")
    }
    sb.append("def main(): Unit \\ IO =\n")
    sb.append("    println(")
    sb.append((0 until count).map(i => s"use$i()").mkString(" + "))
    sb.append(")\n")
    sb.toString()
  }

  private def render(pkg: List[String], name: String): String =
    if (pkg.isEmpty) name else pkg.mkString("/") + "/" + name

  private def parseArgs(args: List[String]): Config = {
    var width = 2
    var count: Option[Int] = None
    var compilerVersion: Option[String] = None
    var remaining = args
    while (remaining.nonEmpty) remaining match {
      case "--width" :: value :: tail => width = value.toInt; remaining = tail
      case "--count" :: value :: tail => count = Some(value.toInt); remaining = tail
      case "--compiler-version" :: value :: tail => compilerVersion = Some(value); remaining = tail
      case other :: _ =>
        Console.err.println(s"unrecognized argument: $other")
        sys.exit(2)
      case Nil => ()
    }
    Config(width, count, compilerVersion)
  }
}
