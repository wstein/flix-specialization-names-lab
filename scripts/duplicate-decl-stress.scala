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
  * Checks that duplicate top-level declarations stay individually diagnosable even when
  * they are byte-for-byte identical -- not just differently named or differently typed.
  *
  * `scripts/collision-stress` checks the back end: that two different specializations never
  * silently share a generated class name. This checks the front end, and a different
  * property: an incorrect program -- one with two declarations of the same name -- must
  * still report every occurrence at its own location, with its own symbol, even when the
  * declarations are otherwise identical text. A frontend that identified declarations by a
  * hash of their content rather than by where they are written would see `count` identical
  * `def probe(): Int32 = 1` lines as indistinguishable and could merge, drop, or misattribute
  * some of them -- which would silently break not just error messages but LSP-facing
  * services built on the same symbols (go-to-definition, hover, semantic tokens): those need
  * to tell "the declaration on line 5" from "the declaration on line 9" apart even when the
  * two read identically.
  *
  * Run with:
  *
  * {{{
  *   ./scripts/duplicate-decl-stress
  *   ./scripts/duplicate-decl-stress --count 500
  *   ./scripts/duplicate-decl-stress --flix-jar path/to/flix.jar
  * }}}
  *
  * `scripts/duplicate-decl-stress` resolves a Flix compiler jar the same way
  * `scripts/edit-resistance` and `scripts/collision-stress` do -- from `--flix-jar`,
  * `$FLIX_JAR`, `./flix.jar`, or `flix` on `$PATH`, never through `flixw` -- then hands it to
  * `scala-cli` as `--jar`, for the same reason: this script alone has no classpath.
  */
object DuplicateDeclStress {

  private case class Config(count: Int, compilerVersion: Option[String])

  def main(args: Array[String]): Unit = {
    val config = parseArgs(args.toList)
    val count = config.count
    val expectedLines = (1 to count).toSet

    println(s"Compiler: ${config.compilerVersion.getOrElse("unspecified")}")
    println(s"Duplicate declarations: $count (all byte-identical: 'def probe(): Int32 = 1')")
    println()

    val source = generateSource(count)

    implicit val sctx: SecurityContext = SecurityContext.Unrestricted
    val flix = new Flix()
    flix.setOptions(Options.Default.copy(progress = false, incremental = false, threads = 1))
    flix.addVirtualPath(CompilerConstants.VirtualTestFile, source)

    val start = System.nanoTime()
    try {
      flix.compile() match {
        case Validation.Success(_) =>
          println("BUG: compiled successfully. A program with duplicate declarations must never compile.")
          System.exit(1)
        case Validation.Failure(errors) =>
          val elapsedMs = (System.nanoTime() - start) / 1000000
          val messages = errors.toList
          val citedLines = messages.flatMap(m => LineRef.findAllMatchIn(m.toString).map(_.group(1).toInt)).toSet
          val missing = expectedLines -- citedLines
          println(s"Compile failed with ${messages.size} error(s) in ${elapsedMs}ms, as expected.")
          println(s"  duplicate lines expected: $count")
          println(s"  distinct lines cited across all errors: ${citedLines.size}")
          if (missing.nonEmpty) {
            println()
            println(s"BUG: ${missing.size} declaration(s) never cited in any error: ${missing.toList.sorted.mkString(", ")}")
            println("These occurrences are undiagnosed -- an editor built on these symbols would have no")
            println("location to attach a squiggly underline, hover, or semantic token to for them.")
            System.exit(1)
          } else {
            println()
            println("Every duplicate declaration was cited at its own line in at least one error.")
            println(f"(observed error count follows 2*(count-1) at every N tried so far: ${2 * (count - 1)}%d expected here)")
          }
      }
    } catch {
      case e: Throwable =>
        val elapsedMs = (System.nanoTime() - start) / 1000000
        println(s"BUG: compile threw after ${elapsedMs}ms instead of reporting a graceful error:")
        println(s"  ${e.getClass.getName}: ${e.getMessage}")
        println("A program with duplicate declarations is invalid, but well-formed enough that the")
        println("compiler should diagnose it, not crash on it.")
        System.exit(1)
    }
  }

  private val LineRef = ":(\\d+):\\d+".r

  /**
    * `count` copies of the exact same top-level def, one per line, so line number alone
    * identifies which occurrence a citation refers to. The body is a literal, not just the
    * signature, so the declarations are identical in every respect a hash of "name plus
    * type plus body" could see -- the only thing that could ever tell them apart is where
    * they are.
    */
  private def generateSource(count: Int): String = {
    val sb = new StringBuilder
    for (_ <- 1 to count) sb.append("def probe(): Int32 = 1\n")
    sb.append("def main(): Unit \\ IO = println(probe())\n")
    sb.toString()
  }

  private def parseArgs(args: List[String]): Config = {
    var count = 50
    var compilerVersion: Option[String] = None
    var remaining = args
    while (remaining.nonEmpty) remaining match {
      case "--count" :: value :: tail => count = value.toInt; remaining = tail
      case "--compiler-version" :: value :: tail => compilerVersion = Some(value); remaining = tail
      case other :: _ =>
        Console.err.println(s"unrecognized argument: $other")
        sys.exit(2)
      case Nil => ()
    }
    Config(count, compilerVersion)
  }
}
