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

import java.nio.file.{Files, Path, Paths}
import scala.jdk.CollectionConverters._

/**
  * Verifies every file under `fixtures/`: each one under `fixtures/positive/` must
  * compile; each one under `fixtures/negative/` must fail to compile, gracefully -- a
  * reported error, not a thrown exception.
  *
  * This is deliberately small, not a peer to `scripts/collision-stress` or
  * `scripts/duplicate-decl-stress` in scope: those generate their own source and stress
  * a scale question ("how many specializations until a collision happens"). This checks
  * a fixed, hand-written, one-claim-per-file set instead, and only ever asks yes/no
  * questions of each file ("does this compile", "does this fail without crashing") --
  * see each fixture's own header comment for what it demonstrates and why it is built
  * the way it is.
  *
  * Run with:
  *
  * {{{
  *   ./scripts/fixtures-check
  *   ./scripts/fixtures-check --flix-jar path/to/flix.jar
  * }}}
  *
  * `scripts/fixtures-check` resolves a Flix compiler jar the same way the other three lab
  * scripts do -- from `--flix-jar`, `$FLIX_JAR`, `./flix.jar`, or `flix` on `$PATH`, never
  * through `flixw` -- then hands it to `scala-cli` as `--jar`, for the same reason: this
  * script alone has no classpath.
  */
object FixturesCheck {

  private case class Config(compilerVersion: Option[String])

  def main(args: Array[String]): Unit = {
    val config = parseArgs(args.toList)
    println(s"Compiler: ${config.compilerVersion.getOrElse("unspecified")}")
    println()

    val root = Paths.get(sys.props.getOrElse("user.dir", "."))
    val positiveDir = root.resolve("fixtures/positive")
    val negativeDir = root.resolve("fixtures/negative")

    val positiveResults = checkAll(positiveDir, expectSuccess = true)
    val negativeResults = checkAll(negativeDir, expectSuccess = false)
    val allResults = positiveResults ++ negativeResults

    println()
    val failures = allResults.count(!_)
    if (failures == 0) {
      println(s"All ${allResults.size} fixture(s) behaved as expected.")
    } else {
      println(s"$failures of ${allResults.size} fixture(s) did NOT behave as expected.")
      System.exit(1)
    }
  }

  /**
    * Compiles every `.flix` file directly under `dir` and reports PASS/FAIL against
    * `expectSuccess`. Returns one boolean per file, in the order checked.
    */
  private def checkAll(dir: Path, expectSuccess: Boolean): List[Boolean] = {
    if (!Files.isDirectory(dir)) {
      Console.err.println(s"$dir: not a directory")
      return Nil
    }
    val files = Files.list(dir).iterator().asScala.toList
      .filter(p => Files.isRegularFile(p) && p.toString.endsWith(".flix"))
      .sortBy(_.getFileName.toString)
    val label = if (expectSuccess) "must compile" else "must fail gracefully"
    println(s"${dir.getFileName} ($label):")
    files.map(f => check(f, expectSuccess))
  }

  private def check(file: Path, expectSuccess: Boolean): Boolean = {
    val name = file.getFileName.toString
    val source = new String(Files.readAllBytes(file), java.nio.charset.StandardCharsets.UTF_8)

    implicit val sctx: SecurityContext = SecurityContext.Unrestricted
    val flix = new Flix()
    flix.setOptions(Options.Default.copy(progress = false, incremental = false, threads = 1))
    flix.addVirtualPath(CompilerConstants.VirtualTestFile, source)

    try {
      flix.compile() match {
        case Validation.Success(_) =>
          if (expectSuccess) {
            println(f"  PASS  $name%-32s compiled")
            true
          } else {
            println(f"  FAIL  $name%-32s compiled, but was expected to fail")
            false
          }
        case Validation.Failure(errors) =>
          if (expectSuccess) {
            println(f"  FAIL  $name%-32s did not compile: ${errors.head}")
            false
          } else {
            println(f"  PASS  $name%-32s failed gracefully (${errors.toList.size} error(s))")
            true
          }
      }
    } catch {
      case e: Throwable =>
        println(f"  FAIL  $name%-32s threw ${e.getClass.getSimpleName}: ${e.getMessage}")
        false
    }
  }

  private def parseArgs(args: List[String]): Config = {
    var compilerVersion: Option[String] = None
    var remaining = args
    while (remaining.nonEmpty) remaining match {
      case "--compiler-version" :: value :: tail => compilerVersion = Some(value); remaining = tail
      case other :: _ =>
        Console.err.println(s"unrecognized argument: $other")
        sys.exit(2)
      case Nil => ()
    }
    Config(compilerVersion)
  }
}
