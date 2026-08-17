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
import ca.uwaterloo.flix.language.ast.{SourceLocation, TypedAst}
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
  * questions of each file ("does this compile", "does this fail without crashing", and for
  * `fixtures/pass/`, "does its non-stdlib symbol table -- and every generated id among it
  * still mint a content-addressed hash, never a classic counter -- still match its golden
  * `.sym` snapshot") -- see each fixture's own header comment for what it demonstrates and
  * why it is built the way it is.
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
      val symFile = root.resolve("test/AllConstructs.sym")
      val hashesFile = root.resolve("test/AllConstructs.hashes")
      List(checkSingle(allConstructsFile, symFileOpt = Some(symFile), hashesFileOpt = Some(hashesFile), expectSuccess = true, isStable, config))
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
    checkSingle(file, symFileOpt = None, hashesFileOpt = None, expectSuccess, isStable, config)
  }

  private def checkSingle(file: Path, symFileOpt: Option[Path], hashesFileOpt: Option[Path], expectSuccess: Boolean, isStable: Boolean, config: Config): Boolean = {
    val name = file.getFileName.toString
    val stem = name.stripSuffix(".flix")
    val source = new String(Files.readAllBytes(file), java.nio.charset.StandardCharsets.UTF_8)

    def newFlix(): Flix = {
      implicit val sctx: SecurityContext = SecurityContext.Unrestricted
      val flix = new Flix()
      flix.setOptions(Options.Default.copy(progress = false, incremental = false, threads = 1))
      flix.addVirtualPath(CompilerConstants.VirtualTestFile, source)
      flix
    }

    try {
      newFlix().compile() match {
        case Validation.Success(result) =>
          if (!expectSuccess) {
            println(f"  FAIL  $name%-32s compiled, but was expected to fail")
            return false
          }

          // Two views, merged: `check()` gives the pre-specialization symbol table --
          // every mod/trait/sig/fun/var/enum/case/struct/field/instance/effect/op/alias the
          // user actually wrote, not filtered to only the ones that happen to mint an id --
          // but it runs before monomorphization, so specialized-def, lifted-lambda,
          // anonymous-class, and enum-case ids don't exist yet there; only instance
          // members and derived instance defs (Namer/Deriver) mint theirs early enough to
          // show up, already embedded in the symbol's own name (e.g. `toString$<id>`).
          // `compile()`'s generated classes are still needed for the other four families'
          // ids, which only exist post-codegen.
          val (rootOpt, _) = newFlix().check()
          val sourceEntries = rootOpt.map(sourceSymbols).getOrElse(Nil)
          val instanceDefIds = rootOpt.map(sourceInstanceDefIds).getOrElse(Nil)
          val sourceCounters = instanceDefIds.filter(_.isCounter).map(_.qualified)
          val sourceHashes = instanceDefIds.filterNot(_.isCounter).map(_.id)

          val classEntries = result.getClasses.keys.flatMap { jvmName =>
            val path = (jvmName.pkg :+ s"${jvmName.name}.class").mkString("/")
            matchedPrefix(path).map { prefix =>
              val (kind, id) = classify(path, prefix)
              (path, kind, id)
            }
          }.toList
          val classCounters = classEntries.collect { case (path, "counter", _) => path }
          val classHashes = classEntries.collect { case (_, "hash", id) => id }
          // "none" (no id at all, e.g. an unspecialized enum case tag) is dropped here: the
          // source-level `case`/`field` entry above already records that the symbol exists,
          // so a bytecode entry with nothing else to add is pure duplication.
          val classLines = classEntries.collect { case (path, k, _) if k != "none" => s"class $path" }

          val renderedSym = (sourceEntries ++ classLines).sorted
          val renderedHashes = (sourceHashes ++ classHashes).distinct.sorted
          val counters = sourceCounters ++ classCounters

          val symFile = symFileOpt.getOrElse(file.getParent.resolve(s"$stem.sym"))
          val hashesFile = hashesFileOpt.getOrElse(file.getParent.resolve(s"$stem.hashes"))

          // `isStable` alone cannot gate this: `java -jar ... --version` never echoes the
          // "+stable.names.N" build metadata that lives only in .flixw/lock.toml, so it is
          // false on every ordinary wrapper-driven run against this project's own pinned
          // fork -- relying on it here would make the whole check silently inert. A prior
          // golden snapshot recording at least one `$`-id-bearing entry is the more durable
          // signal: once a fixture has demonstrated it mints a content-addressed id, that id
          // going away is a regression to fail on, not license to fall back to "stock
          // compiler, skip".
          val hasHash = renderedHashes.nonEmpty
          val hadStableSnapshot = Files.exists(symFile) &&
            new String(Files.readAllBytes(symFile), java.nio.charset.StandardCharsets.UTF_8).linesIterator.exists(_.contains("$"))
          val hasStableNames = isStable || hasHash || hadStableSnapshot

          // A counter-shaped id anywhere a hash was expected -- this run producing one, or
          // any prior snapshot ever having recorded one -- is a regression worth failing
          // loudly on, independent of whether it happens to match (or gets baked into, via
          // -u) a stale snapshot: the whole point of these fixtures is that every
          // id-minting family they exercise mints a content-addressed hash, never a classic
          // incrementing counter.
          if (hasStableNames && counters.nonEmpty) {
            println(f"  FAIL  $name%-32s minted a counter-shaped id where a content-addressed hash was expected: ${counters.mkString(", ")}")
            false
          } else if (!hasStableNames) {
            println(f"  PASS  $name%-32s compiled (stable.names check skipped on stock compiler)")
            true
          } else if (config.updateSnapshots) {
            Files.write(symFile, (renderedSym.mkString("\n") + "\n").getBytes(java.nio.charset.StandardCharsets.UTF_8))
            Files.write(hashesFile, (renderedHashes.mkString("\n") + "\n").getBytes(java.nio.charset.StandardCharsets.UTF_8))
            println(f"  PASS  $name%-32s compiled, updated ${symFile.getFileName} and ${hashesFile.getFileName} (${renderedSym.size} symbol(s), ${renderedHashes.size} hash(es))")
            true
          } else if (Files.exists(symFile) && Files.exists(hashesFile)) {
            val expectedSym = new String(Files.readAllBytes(symFile), java.nio.charset.StandardCharsets.UTF_8).trim.linesIterator.toList.filter(_.nonEmpty)
            val expectedHashes = new String(Files.readAllBytes(hashesFile), java.nio.charset.StandardCharsets.UTF_8).trim.linesIterator.toList.filter(_.nonEmpty)
            if (renderedSym == expectedSym && renderedHashes == expectedHashes) {
              println(f"  PASS  $name%-32s compiled, matched ${symFile.getFileName} and ${hashesFile.getFileName} (${renderedSym.size} symbol(s), ${renderedHashes.size} hash(es))")
              true
            } else {
              println(f"  FAIL  $name%-32s golden file(s) do not match")
              if (renderedSym != expectedSym) {
                println(s"        ${symFile.getFileName} expected: $expectedSym")
                println(s"        ${symFile.getFileName} actual:   $renderedSym")
              }
              if (renderedHashes != expectedHashes) {
                println(s"        ${hashesFile.getFileName} expected: $expectedHashes")
                println(s"        ${hashesFile.getFileName} actual:   $renderedHashes")
              }
              false
            }
          } else if (Files.exists(symFile) || Files.exists(hashesFile)) {
            println(f"  FAIL  $name%-32s only one of ${symFile.getFileName}/${hashesFile.getFileName} exists -- run with -u to create the other")
            false
          } else {
            println(f"  PASS  $name%-32s compiled (${renderedSym.size} symbol(s), ${renderedHashes.size} hash(es))")
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

  // Precise, not just discriminating: for classification (see `classify` below), a prefix
  // must reach all the way to where an id or case tag begins, not merely far enough to tell
  // a fixture class apart from stdlib/runtime noise. "DemoBox/", "Person/", and
  // "Describable/" alone stop at a package boundary, one def short of that -- combined with
  // their def below into a single entry each.
  private val Prefixes = List(
    "Anon$", "DemoBox/Def$roundTrip$", "Person/Def$makeAndAge$", "Describable/Def$describeTwice$",
    "Wrapper$", "Clo$liftedClosures$", "Def$describeShape$", "Def$greet$", "Eq/Def$eq$", "Color$",
    "Def$add$", "Def$anonClassDemo$", "Def$binaryPipelineDemo$", "Def$boxDemo$", "Def$collectionsDemo$",
    "Def$concurrencyDemo$", "Def$curriedMultiply$", "Def$datalogDemo$", "Def$describeItem$",
    "Def$factorial$", "Def$genericIdentity$", "Def$getX$", "Def$higherOrderDemo$", "Def$imperativeDemo$",
    "Def$incrementState$", "Def$javaExceptionDemo$", "Def$javaInteropDemo$", "Def$lazyAndCastsDemo$",
    "Def$liftedClosures$", "Def$liftedClosuresDemo$", "Def$literalsDemo$", "Def$manyInstantiations$",
    "Def$parYieldDemo$", "Def$patternMatchDemo$", "Def$pipelineDemo$", "Def$recordDemo$",
    "Def$repeatedStdlibDemo$", "Def$runEffectDemo$", "Def$selectDemo$", "Def$shapeArea$",
    "Def$structDemo$", "Def$tuplePatternMatch$",
    "Clo$anonClassDemo$", "Clo$collectionsDemo$", "Clo$concurrencyDemo$", "Clo$curriedMultiply$",
    "Clo$datalogDemo$", "Clo$higherOrderDemo$", "Clo$imperativeDemo$", "Clo$lazyAndCastsDemo$",
    "Clo$parYieldDemo$", "Clo$selectDemo$"
  )

  private def matchedPrefix(path: String): Option[String] = Prefixes.find(path.startsWith)

  // The id (if any) is whatever run of lowercase-base-36 characters immediately follows the
  // matched prefix -- a case tag or further literal name segment always starts with an
  // uppercase letter by Flix's own naming convention, so it naturally stops the run rather
  // than needing a length heuristic to tell the two apart. Exact, not a shape guess: this
  // tool knows precisely which literal text precedes an id, unlike a blind stdlib-scale
  // census that has to infer it (see scripts/class-id-report.scala's Ids.isPlausibleId).
  private def classify(path: String, prefix: String): (String, String) = {
    val rest = path.stripPrefix(prefix).stripSuffix(".class")
    val idRun = rest.takeWhile(c => c.isDigit || (c >= 'a' && c <= 'z'))
    val kind =
      if (idRun.isEmpty) "none"
      else if (idRun.forall(_.isDigit) && idRun.length < 8) "counter"
      else "hash"
    (kind, idRun)
  }

  // The fixture's own source, whatever its filename, always compiles under this fixed
  // virtual name -- see CompilerConstants.VirtualTestFile and every .expect file's
  // "__TEST__.flix:line:col" citations.
  private val TestFileName = "__TEST__.flix"

  private def isUserCode(loc: SourceLocation): Boolean = loc.source.name == TestFileName

  private def qualify(namespace: List[String], name: String): String = (namespace :+ name).mkString(".")

  /**
    * Every non-stdlib symbol the fixture's own source declares -- not just the ones that
    * happen to mint a generated id -- one `<kind> <qualified name>` line each: `mod`,
    * `trait`, `sig`, `fun` (top-level defs), `var` (formal parameters), `enum`, `case`,
    * `struct`, `field`, `instance`, `instance-def` (an instance's or derived instance's own
    * member, whose name already embeds its id -- see checkSingle's doc comment), `effect`,
    * `op`, `alias`. Deliberately stops at formal parameters: recursing into expression
    * bodies to list every let-bound and match-bound local would mostly surface compiler-
    * introduced desugaring artifacts, not symbols a reader of the fixture would recognize.
    */
  private def sourceSymbols(root: TypedAst.Root): List[String] = {
    def fparamVars(qualifiedOwner: String, fparams: Iterable[TypedAst.FormalParam]): Iterator[String] =
      fparams.iterator.filter(fp => isUserCode(fp.loc)).map(fp => s"var $qualifiedOwner/${fp.bnd.sym.text}")

    val mods = root.modules.values.iterator.filter(m => isUserCode(m.loc) && !m.sym.isRoot)
      .map(m => s"mod ${m.sym.ns.mkString(".")}")

    val traits = root.traits.values.iterator.filter(t => isUserCode(t.loc))
      .map(t => s"trait ${qualify(t.sym.namespace, t.sym.name)}")

    val sigs = root.sigs.values.iterator.filter(s => isUserCode(s.loc))
    val sigLines = sigs.map(s => s"sig ${qualify(s.sym.namespace, s.sym.name)}")
    val sigVars = root.sigs.values.iterator.filter(s => isUserCode(s.loc))
      .flatMap(s => fparamVars(qualify(s.sym.namespace, s.sym.name), s.spec.fparams.toList))

    val defs = root.defs.values.iterator.filter(d => isUserCode(d.loc))
    val defLines = defs.map(d => s"fun ${qualify(d.sym.namespace, d.sym.name)}")
    val defVars = root.defs.values.iterator.filter(d => isUserCode(d.loc))
      .flatMap(d => fparamVars(qualify(d.sym.namespace, d.sym.name), d.spec.fparams.toList))

    val enums = root.enums.values.iterator.filter(e => isUserCode(e.loc))
    val enumLines = enums.map(e => s"enum ${qualify(e.sym.namespace, e.sym.name)}")
    val caseLines = root.enums.values.iterator.filter(e => isUserCode(e.loc)).flatMap(_.cases.values)
      .filter(c => isUserCode(c.loc)).map(c => s"case ${qualify(c.sym.namespace, c.sym.name)}")

    val structs = root.structs.values.iterator.filter(s => isUserCode(s.loc))
    val structLines = structs.map(s => s"struct ${qualify(s.sym.namespace, s.sym.name)}")
    val fieldLines = root.structs.values.iterator.filter(s => isUserCode(s.loc)).flatMap(_.fields.values)
      .filter(f => isUserCode(f.loc)).map(f => s"field ${qualify(f.sym.namespace, f.sym.name)}")

    val instances = root.instances.values.iterator.filter(i => isUserCode(i.loc))
    val instanceLines = instances.map(i => s"instance ${i.trt.sym.name}[${i.tpe}]")
    val instanceDefLines = root.instances.values.iterator.filter(i => isUserCode(i.loc)).flatMap(_.defs)
      .filter(d => isUserCode(d.loc)).map(d => s"instance-def ${qualify(d.sym.namespace, d.sym.name)}")
    val instanceDefVars = root.instances.values.iterator.filter(i => isUserCode(i.loc)).flatMap(_.defs)
      .filter(d => isUserCode(d.loc))
      .flatMap(d => fparamVars(qualify(d.sym.namespace, d.sym.name), d.spec.fparams.toList))

    val effects = root.effects.values.iterator.filter(e => isUserCode(e.loc))
    val effectLines = effects.map(e => s"effect ${qualify(e.sym.namespace, e.sym.name)}")
    val opLines = root.effects.values.iterator.filter(e => isUserCode(e.loc)).flatMap(_.ops)
      .filter(o => isUserCode(o.loc)).map(o => s"op ${qualify(o.sym.namespace, o.sym.name)}")

    val aliases = root.typeAliases.values.iterator.filter(a => isUserCode(a.loc))
      .map(a => s"alias ${qualify(a.sym.namespace, a.sym.name)}")

    (mods ++ traits ++ sigLines ++ sigVars ++ defLines ++ defVars ++ enumLines ++ caseLines ++
      structLines ++ fieldLines ++ instanceLines ++ instanceDefLines ++ instanceDefVars ++
      effectLines ++ opLines ++ aliases).toList
  }

  // Instance members and derived instance defs (Namer/Deriver) mint their id early enough
  // that it is already part of the symbol's own name by check()-time -- e.g.
  // `toString$awemhsldalu5` -- unlike the four families that only get theirs at codegen.
  // The id is always the trailing `$`-segment for this family (unlike the bytecode-level
  // enum-case family, where a case tag can follow it), so a plain last-`$`-split suffices.
  //
  // Verified against a build forced to --Xstable-name-length 0 that this name stays
  // hash-shaped regardless -- the fallback to classic incrementing ids for these two
  // families happens later, only in the rendered bytecode class name (which is what
  // `classify` below catches). So this function is not a second, redundant path to the
  // same failure: it exists for a distinct, still-plausible regression -- Namer or Deriver
  // itself minting a non-content-addressed id at the AST level -- that a bytecode-only
  // check could miss if it ever happened to render into a fixed-width-looking string.
  private case class InstanceDefId(qualified: String, id: String, isCounter: Boolean)

  private def sourceInstanceDefIds(root: TypedAst.Root): List[InstanceDefId] = {
    root.instances.values.filter(i => isUserCode(i.loc)).flatMap(_.defs).filter(d => isUserCode(d.loc))
      .flatMap { d =>
        val idx = d.sym.name.lastIndexOf('$')
        if (idx < 0) None
        else {
          val id = d.sym.name.substring(idx + 1)
          Some(InstanceDefId(qualify(d.sym.namespace, d.sym.name), id, id.forall(_.isDigit) && id.length < 8))
        }
      }.toList
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
