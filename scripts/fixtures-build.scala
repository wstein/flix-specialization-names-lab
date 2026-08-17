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
  * Compiles every file under `fixtures/positive/` for real and writes each one's generated
  * classes to its own directory under `build/fixtures/`, so a single fixture's classes can be
  * censused with `scripts/class-id-report` in isolation -- without `AllConstructs.flix`'s
  * hundreds of other generated names in the way.
  *
  * `fixtures/negative/` is skipped: every file there is built to fail, by design, so there is
  * nothing to write.
  *
  * This is the disk-writing counterpart to `scripts/fixtures-check`, which compiles the same
  * files in memory and only asks pass/fail. Reuse that one to verify a fixture still behaves
  * as documented; use this one to look at what it actually produced.
  *
  * Run with:
  *
  * {{{
  *   ./scripts/fixtures-build
  *   ./scripts/fixtures-build --flix-jar path/to/flix.jar
  *   ./scripts/class-id-report build/fixtures/enum-case
  * }}}
  *
  * `scripts/fixtures-build` resolves a Flix compiler jar the same way the other lab scripts
  * do -- from `--flix-jar`, `$FLIX_JAR`, `./flix.jar`, or `flix` on `$PATH`, never through
  * `flixw` -- then hands it to `scala-cli` as `--jar`, for the same reason: this script alone
  * has no classpath.
  */
object FixturesBuild {

  private case class Config(compilerVersion: Option[String], out: Option[String])

  def main(args: Array[String]): Unit = {
    val config = parseArgs(args.toList)
    println(s"Compiler: ${config.compilerVersion.getOrElse("unspecified")}")

    val root = Paths.get(sys.props.getOrElse("user.dir", "."))
    val positiveDir = root.resolve("fixtures/positive")
    val outDir = root.resolve(config.out.getOrElse("build/fixtures"))

    if (!Files.isDirectory(positiveDir)) {
      Console.err.println(s"$positiveDir: not a directory")
      System.exit(1)
    }

    // Scoped to this tool's own output, not all of build/: unlike scripts/edit-resistance
    // (which compiles in-process and never touches disk itself), this writes real class
    // files and a stale one left behind by a fixture that used to exist would otherwise
    // linger and read as still current.
    deleteRecursively(outDir)
    Files.createDirectories(outDir)

    val files = Files.list(positiveDir).iterator().asScala.toList
      .filter(p => Files.isRegularFile(p) && p.toString.endsWith(".flix"))
      .sortBy(_.getFileName.toString)

    println(s"positive (${files.size} fixture(s)):")
    val results = files.map(f => build(f, outDir))

    println()
    val failures = results.count(!_)
    if (failures == 0) {
      println(s"Wrote ${results.size} fixture(s) under $outDir.")
      println("Point scripts/class-id-report at one of its subdirectories to census it alone.")
    } else {
      println(s"$failures of ${results.size} fixture(s) failed to build.")
      System.exit(1)
    }
  }

  private def build(file: Path, outDir: Path): Boolean = {
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
          val fixtureDir = outDir.resolve(stem)
          result.getClasses.foreach { case (jvmName, clazz) =>
            val dir = jvmName.pkg.foldLeft(fixtureDir)(_.resolve(_))
            Files.createDirectories(dir)
            Files.write(dir.resolve(s"${jvmName.name}.class"), clazz.bytecode)
          }
          println(f"  OK    $name%-32s ${result.getClasses.size} class(es) -> ${outDir.relativize(fixtureDir)}")
          true
        case Validation.Failure(errors) =>
          println(f"  FAIL  $name%-32s did not compile: ${errors.head}")
          false
      }
    } catch {
      case e: Throwable =>
        println(f"  FAIL  $name%-32s threw ${e.getClass.getSimpleName}: ${e.getMessage}")
        false
    }
  }

  private def deleteRecursively(dir: Path): Unit = {
    if (Files.exists(dir)) {
      Files.walk(dir).iterator().asScala.toList.sortBy(_.toString).reverse.foreach(Files.deleteIfExists)
    }
  }

  private def parseArgs(args: List[String]): Config = {
    var compilerVersion: Option[String] = None
    var out: Option[String] = None
    var remaining = args
    while (remaining.nonEmpty) remaining match {
      case "--compiler-version" :: value :: tail => compilerVersion = Some(value); remaining = tail
      case "--out" :: value :: tail => out = Some(value); remaining = tail
      case other :: _ =>
        Console.err.println(s"unrecognized argument: $other")
        sys.exit(2)
      case Nil => ()
    }
    Config(compilerVersion, out)
  }
}
