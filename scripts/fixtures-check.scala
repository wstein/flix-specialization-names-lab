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
  * Verifies every file under `fixtures/`: each one under `fixtures/pass/` must
  * compile; each one under `fixtures/fail/` must fail to compile, gracefully -- a
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
  *   ./scripts/fixtures-check -v   # print every reported error, not just the count
  * }}}
  *
  * `scripts/fixtures-check` resolves a Flix compiler jar the same way the other three lab
  * scripts do -- from `--flix-jar`, `$FLIX_JAR`, `./flix.jar`, or `flix` on `$PATH`, never
  * through `flixw` -- then hands it to `scala-cli` as `--jar`, for the same reason: this
  * script alone has no classpath.
  */
object FixturesCheck {

  private case class Config(compilerVersion: Option[String], verbose: Boolean, updateSnapshots: Boolean)

  def main(args: Array[String]): Unit = {
    val config = parseArgs(args.toList)
    val versionStr = config.compilerVersion.getOrElse("unspecified")
    println(s"Compiler: $versionStr")
    println()

    val root = Paths.get(sys.props.getOrElse("user.dir", "."))
    val passDir = root.resolve("fixtures/pass")
    val failDir = root.resolve("fixtures/fail")

    val isStable = isStableNamesCompiler(versionStr)

    val passResults = checkAll(passDir, expectSuccess = true, isStable, config)
    val failResults = checkAll(failDir, expectSuccess = false, isStable, config)

    val allConstructsFile = root.resolve("src/AllConstructs.flix")
    val allConstructsResult = if (Files.exists(allConstructsFile)) {
      println(s"src (integration demo):")
      val namesFile = root.resolve("test/AllConstructs.names")
      List(checkSingle(allConstructsFile, namesFileOpt = Some(namesFile), expectSuccess = true, isStable, config))
    } else Nil

    val allResults = passResults ++ failResults ++ allConstructsResult

    println()
    val failures = allResults.count(!_)
    if (failures == 0) {
      println(s"All ${allResults.size} fixture(s) behaved as expected.")
    } else {
      println(s"$failures of ${allResults.size} fixture(s) did NOT behave as expected.")
      System.exit(1)
    }
  }

  private def isStableNamesCompiler(version: String): Boolean = {
    version.contains("stable.names")
  }

  /**
    * Compiles every `.flix` file directly under `dir` and reports PASS/FAIL against
    * `expectSuccess`. Returns one boolean per file, in the order checked.
    */
  private def checkAll(dir: Path, expectSuccess: Boolean, isStable: Boolean, config: Config): List[Boolean] = {
    if (!Files.isDirectory(dir)) {
      Console.err.println(s"$dir: not a directory")
      return Nil
    }
    val files = Files.list(dir).iterator().asScala.toList
      .filter(p => Files.isRegularFile(p) && p.toString.endsWith(".flix"))
      .sortBy(_.getFileName.toString)
    val label = if (expectSuccess) "must compile & match snapshots" else "must fail gracefully & match .expect"
    println(s"${dir.getFileName} ($label):")
    files.map(f => check(f, expectSuccess, isStable, config))
  }

  private def check(file: Path, expectSuccess: Boolean, isStable: Boolean, config: Config): Boolean = {
    checkSingle(file, namesFileOpt = None, expectSuccess, isStable, config)
  }

  private def checkSingle(file: Path, namesFileOpt: Option[Path], expectSuccess: Boolean, isStable: Boolean, config: Config): Boolean = {
    val name = file.getFileName.toString
    val stem = name.stripSuffix(".flix")
    val source = new String(Files.readAllBytes(file), java.nio.charset.StandardCharsets.UTF_8)

    implicit val sctx: SecurityContext = SecurityContext.Unrestricted
    val flix = new Flix()
    flix.setOptions(Options.Default.copy(progress = false, incremental = false, threads = 1))
    flix.addVirtualPath(CompilerConstants.VirtualTestFile, source)

    try {
      flix.compile() match {
        case Validation.Success(result) =>
          if (!expectSuccess) {
            println(f"  FAIL  $name%-32s compiled, but was expected to fail")
            return false
          }

          val fixtureClasses = result.getClasses.keys.map { jvmName =>
            (jvmName.pkg :+ s"${jvmName.name}.class").mkString("/")
          }.filter(isFixtureClass).toList.sorted

          val isCounterId = fixtureClasses.exists(c => c.matches(".*\\$[0-9]+(\\..*)?$"))
          val hasStableNames = isStable || (!isCounterId && fixtureClasses.exists(c => c.matches(".*\\$[a-z0-9]{8,}.*")))

          val namesFile = namesFileOpt.getOrElse(file.getParent.resolve(s"$stem.names"))
          val namesFileName = namesFile.getFileName.toString
          if (config.updateSnapshots && hasStableNames) {
            Files.write(namesFile, (fixtureClasses.mkString("\n") + "\n").getBytes(java.nio.charset.StandardCharsets.UTF_8))
            println(f"  PASS  $name%-32s compiled, updated $namesFileName (${fixtureClasses.size} class(es))")
            true
          } else if (hasStableNames && Files.exists(namesFile)) {
            val expected = new String(Files.readAllBytes(namesFile), java.nio.charset.StandardCharsets.UTF_8).trim.linesIterator.toList.filter(_.nonEmpty)
            if (fixtureClasses == expected) {
              println(f"  PASS  $name%-32s compiled, matched $namesFileName (${fixtureClasses.size} stable class(es))")
              true
            } else {
              println(f"  FAIL  $name%-32s generated classes do not match $namesFileName")
              println(s"        expected: $expected")
              println(s"        actual:   $fixtureClasses")
              false
            }
          } else if (!hasStableNames) {
            println(f"  PASS  $name%-32s compiled (stable.names check skipped on stock compiler)")
            true
          } else {
            println(f"  PASS  $name%-32s compiled (${fixtureClasses.size} class(es))")
            true
          }

        case Validation.Failure(errors) =>
          if (expectSuccess) {
            println(f"  FAIL  $name%-32s did not compile: ${errors.head}")
            if (config.verbose) printErrors(errors.toList)
            return false
          }

          val actualErrors = errors.toList.map(_.toString).sorted
          val expectFile = file.getParent.resolve(s"$stem.expect")

          if (config.updateSnapshots) {
            Files.write(expectFile, (actualErrors.mkString("\n") + "\n").getBytes(java.nio.charset.StandardCharsets.UTF_8))
            println(f"  PASS  $name%-32s failed gracefully, updated $stem.expect (${actualErrors.size} error(s))")
            true
          } else if (Files.exists(expectFile)) {
            val expectedErrors = new String(Files.readAllBytes(expectFile), java.nio.charset.StandardCharsets.UTF_8).trim.linesIterator.toList.filter(_.nonEmpty).sorted
            if (actualErrors == expectedErrors) {
              println(f"  PASS  $name%-32s failed gracefully, matched $stem.expect (${actualErrors.size} error(s))")
              if (config.verbose) printErrors(errors.toList)
              true
            } else {
              println(f"  FAIL  $name%-32s diagnostics do not match $stem.expect")
              println(s"        expected (${expectedErrors.size}):")
              expectedErrors.foreach(e => println(s"          - $e"))
              println(s"        actual (${actualErrors.size}):")
              actualErrors.foreach(e => println(s"          - $e"))
              false
            }
          } else {
            println(f"  PASS  $name%-32s failed gracefully (${actualErrors.size} error(s))")
            if (config.verbose) printErrors(errors.toList)
            true
          }
      }
    } catch {
      case e: Throwable =>
        println(f"  FAIL  $name%-32s threw ${e.getClass.getSimpleName}: ${e.getMessage}")
        false
    }
  }

  private def isFixtureClass(path: String): Boolean = {
    val userPrefixes = List(
      "Anon$", "DemoBox/", "Person/", "Describable/", "Wrapper$", "Clo$liftedClosures$",
      "Def$describeShape$", "Def$roundTrip$"
    )
    val userDefPrefixes = List(
      "Def$add$", "Def$anonClassDemo$", "Def$binaryPipelineDemo$", "Def$boxDemo$", "Def$collectionsDemo$",
      "Def$concurrencyDemo$", "Def$curriedMultiply$", "Def$datalogDemo$", "Def$describeItem$", "Def$describeShape$",
      "Def$factorial$", "Def$genericIdentity$", "Def$getX$", "Def$higherOrderDemo$", "Def$imperativeDemo$",
      "Def$incrementState$", "Def$javaExceptionDemo$", "Def$javaInteropDemo$", "Def$lazyAndCastsDemo$",
      "Def$liftedClosures$", "Def$liftedClosuresDemo$", "Def$literalsDemo$", "Def$manyInstantiations$",
      "Def$parYieldDemo$", "Def$patternMatchDemo$", "Def$pipelineDemo$", "Def$recordDemo$",
      "Def$repeatedStdlibDemo$", "Def$runEffectDemo$", "Def$selectDemo$", "Def$shapeArea$",
      "Def$structDemo$", "Def$tuplePatternMatch$",
      "Clo$anonClassDemo$", "Clo$collectionsDemo$", "Clo$concurrencyDemo$", "Clo$curriedMultiply$",
      "Clo$datalogDemo$", "Clo$higherOrderDemo$", "Clo$imperativeDemo$", "Clo$lazyAndCastsDemo$",
      "Clo$liftedClosures$", "Clo$parYieldDemo$", "Clo$selectDemo$"
    )
    userPrefixes.exists(p => path.startsWith(p)) || userDefPrefixes.exists(p => path.startsWith(p))
  }

  private def printErrors(errors: List[_]): Unit = {
    for (e <- errors) {
      println(s"        - $e")
    }
  }

  private def parseArgs(args: List[String]): Config = {
    var compilerVersion: Option[String] = None
    var verbose = false
    var updateSnapshots = false
    var remaining = args
    while (remaining.nonEmpty) remaining match {
      case "--compiler-version" :: value :: tail => compilerVersion = Some(value); remaining = tail
      case ("-v" | "--verbose") :: tail => verbose = true; remaining = tail
      case ("-u" | "--update-snapshots") :: tail => updateSnapshots = true; remaining = tail
      case other :: _ =>
        Console.err.println(s"unrecognized argument: $other")
        sys.exit(2)
      case Nil => ()
    }
    Config(compilerVersion, verbose, updateSnapshots)
  }
}
