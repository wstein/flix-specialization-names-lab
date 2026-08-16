#!/usr/bin/env -S scala-cli shebang
//> using scala "2.13.16"

import java.io.{DataInputStream, BufferedInputStream}
import java.nio.file.{Files, Path, Paths}
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import scala.collection.JavaConverters._
import scala.util.Using

/**
 * Extracts generated IDs and JVM symbols from an existing class directory.
 *
 * Usage: ./trace-fresh-ids.scala [class-directory] [--output-dir directory] [--json | --markdown]
 *
 * The script never invokes the Flix compiler. It supports both decimal GenSym
 * suffixes from older output and fixed-width base-36 SHA-256 suffixes from newer output.
 */
object TraceFreshIds extends App {
  val config = Arguments.parse(args.toList)
  val classDir = config.classDir
  val outputDir = config.outputDir
  if (!Files.isDirectory(classDir)) {
    Console.err.println(s"Class directory not found: $classDir")
    sys.exit(1)
  }
  Files.createDirectories(outputDir)

  val classFiles = Using.resource(Files.walk(classDir)) { paths =>
    paths.iterator.asScala.filter(path => Files.isRegularFile(path) && path.toString.endsWith(".class")).toList.sortBy(_.toString)
  }

  val rows = classFiles.flatMap { path =>
    ClassFile.read(path).toList.flatMap { info =>
      val relative = classDir.relativize(path).toString
      ClassFile.symbols(info, relative)
    }
  }

  val ids = rows.iterator.flatMap(row => Ids.in(row.symbol)).toSet
  val timestamp = DateTimeFormatter.ofPattern("yyMMdd-HHmm").format(LocalDateTime.now())
  val symbolsFile = outputDir.resolve(s"symbols-$timestamp.csv")
  val idsFile = outputDir.resolve(s"ids-$timestamp.txt")

  Files.writeString(symbolsFile, Csv.header + System.lineSeparator + rows.map(_.csv).mkString(System.lineSeparator()) + (if (rows.nonEmpty) System.lineSeparator() else ""))
  Files.writeString(idsFile, ids.toList.sortBy(Ids.order).mkString("", System.lineSeparator(), if (ids.nonEmpty) System.lineSeparator() else ""))

  val report = Report(classDir, classFiles.size, rows.size, ids.size, ids.count(Ids.isCounter), symbolsFile, idsFile)
  println(Reports.render(report, config.format))
}

object Arguments {
  private val Usage = "Usage: trace-fresh-ids.scala [class-directory] [--output-dir directory] [--json | --markdown]"

  def parse(args: List[String]): Config = {
    var classDir = Paths.get("build/class")
    var classDirSet = false
    var outputDir = Paths.get(".")
    var format: ReportFormat = ReportFormat.Text
    var remaining = args

    def select(next: ReportFormat): Unit = {
      if (format != ReportFormat.Text) fail()
      format = next
    }

    while (remaining.nonEmpty) remaining match {
      case "--json" :: tail => select(ReportFormat.Json); remaining = tail
      case "--markdown" :: tail => select(ReportFormat.Markdown); remaining = tail
      case ("--output-dir" | "-o") :: directory :: tail => outputDir = Paths.get(directory); remaining = tail
      case "--help" :: _ => println(Usage); sys.exit(0)
      case directory :: tail if !directory.startsWith("-") && !classDirSet =>
        classDir = Paths.get(directory); classDirSet = true; remaining = tail
      case _ => fail()
    }
    Config(classDir, outputDir, format)
  }

  private def fail(): Nothing = {
    Console.err.println(Usage)
    sys.exit(2)
  }
}

sealed trait ReportFormat
object ReportFormat {
  case object Text extends ReportFormat
  case object Json extends ReportFormat
  case object Markdown extends ReportFormat
}

case class Config(classDir: Path, outputDir: Path, format: ReportFormat)
case class Report(classDir: Path, classFiles: Int, symbols: Int, uniqueIds: Int, counters: Int, symbolsFile: Path, idsFile: Path) {
  def hashes: Int = uniqueIds - counters
}

object Reports {
  def render(report: Report, format: ReportFormat): String = format match {
    case ReportFormat.Text => text(report)
    case ReportFormat.Json => json(report)
    case ReportFormat.Markdown => markdown(report)
  }

  private def text(report: Report): String = List(
    "Generated-class ID census",
    s"  directory: ${report.classDir}",
    s"  class files: ${report.classFiles}",
    s"  symbols: ${report.symbols}",
    s"  unique generated IDs: ${report.uniqueIds}",
    s"  sequential counter IDs: ${report.counters}",
    s"  SHA-256 hash-derived IDs: ${report.hashes}",
    s"  symbols CSV: ${report.symbolsFile}",
    s"  IDs list: ${report.idsFile}"
  ).mkString(System.lineSeparator())

  private def json(report: Report): String =
    s"""{"class_directory":"${escapeJson(report.classDir.toString)}","class_files":${report.classFiles},"symbols":${report.symbols},"unique_generated_ids":${report.uniqueIds},"id_kinds":{"sequential_counter":${report.counters},"sha256_hash_derived":${report.hashes}},"outputs":{"symbols_csv":"${escapeJson(report.symbolsFile.toString)}","ids_txt":"${escapeJson(report.idsFile.toString)}"}}"""

  private def markdown(report: Report): String = List(
    "# Generated-class ID census",
    "",
    "| Metric | Value |",
    "| --- | ---: |",
    s"| Class directory | `${report.classDir}` |",
    s"| Class files | ${report.classFiles} |",
    s"| Symbols extracted | ${report.symbols} |",
    s"| Unique generated IDs | ${report.uniqueIds} |",
    s"| Sequential counter IDs | ${report.counters} |",
    s"| SHA-256 hash-derived IDs | ${report.hashes} |",
    "",
    s"Symbols: `${report.symbolsFile}`  ",
    s"IDs: `${report.idsFile}`"
  ).mkString(System.lineSeparator())

  private def escapeJson(value: String): String = value.flatMap {
    case '"' => "\\\""
    case '\\' => "\\\\"
    case '\b' => "\\b"
    case '\f' => "\\f"
    case '\n' => "\\n"
    case '\r' => "\\r"
    case '\t' => "\\t"
    case c if c < ' ' => f"\\u${c.toInt}%04x"
    case c => c.toString
  }
}

object Ids {
  private val Stable = "\\$([0-9a-z]{13})(?![0-9a-z])".r
  private val Counter = "\\$(\\d+)(?![0-9a-z])".r

  def in(text: String): List[String] = {
    val stable = Stable.findAllMatchIn(text).map(_.group(1)).toList
    val counter = Counter.findAllMatchIn(text).map(_.group(1)).filter(_.length != 13).toList
    stable ++ counter
  }

  def isCounter(id: String): Boolean = id.forall(_.isDigit) && id.length != 13

  def order(id: String): (Int, BigInt, String) =
    if (isCounter(id)) (0, BigInt(id), "") else (1, BigInt(0), id)
}

case class SymbolRow(kind: String, clazz: String, symbol: String, id: String, descriptor: String, signature: String, file: String) {
  def csv: String = List(kind, clazz, symbol, id, descriptor, signature, file).map(Csv.escape).mkString(",")
}

object Csv {
  val header: String = "kind,class,symbol,id,descriptor,signature,file"

  def escape(value: String): String =
    if (value.exists(ch => ch == ',' || ch == '"' || ch == '\n' || ch == '\r')) s"\"${value.replace("\"", "\"\"")}\"" else value
}

object ClassFile {
  private sealed trait Cp
  private case class Utf(value: String) extends Cp
  private case class Clazz(nameIndex: Int) extends Cp
  private case object Other extends Cp

  case class Member(name: String, descriptor: String)
  case class Info(name: String, superName: String, fields: List[Member], methods: List[Member], strings: List[String])

  def read(path: Path): Option[Info] =
    try Using.resource(new DataInputStream(new BufferedInputStream(Files.newInputStream(path)))) { in =>
      if (in.readInt() != 0xcafebabe) return None
      in.readUnsignedShort()
      in.readUnsignedShort()
      val cp = Array.fill[Option[Cp]](in.readUnsignedShort())(None)
      var i = 1
      while (i < cp.length) {
        in.readUnsignedByte() match {
          case 1 => cp(i) = Some(Utf(in.readUTF()))
          case 3 | 4 => in.skipBytes(4)
          case 5 | 6 => in.skipBytes(8); i += 1
          case 7 => cp(i) = Some(Clazz(in.readUnsignedShort()))
          case 8 | 16 | 19 | 20 => in.skipBytes(2)
          case 9 | 10 | 11 | 12 | 17 | 18 => in.skipBytes(4)
          case 15 => in.skipBytes(3)
          case tag => throw new IllegalArgumentException(s"unsupported class-file constant-pool tag: $tag")
        }
        i += 1
      }

      def utf(index: Int): String = cp(index).collect { case Utf(value) => value }.getOrElse("")
      def className(index: Int): String = cp(index).collect { case Clazz(name) => utf(name) }.getOrElse("")
      def skipAttributes(): Unit = {
        val count = in.readUnsignedShort()
        (0 until count).foreach { _ =>
          in.readUnsignedShort()
          in.skipNBytes(in.readInt().toLong)
        }
      }
      def members(): List[Member] = {
        val count = in.readUnsignedShort()
        List.fill(count) {
          in.readUnsignedShort()
          val member = Member(utf(in.readUnsignedShort()), utf(in.readUnsignedShort()))
          skipAttributes()
          member
        }
      }

      in.readUnsignedShort()
      val name = className(in.readUnsignedShort())
      val superName = className(in.readUnsignedShort())
      val interfaceCount = in.readUnsignedShort()
      (0 until interfaceCount).foreach(_ => in.readUnsignedShort())
      val fields = members()
      val methods = members()
      Some(Info(name.replace('/', '.'), superName.replace('/', '.'), fields, methods, cp.collect { case Some(Utf(value)) => value }.toList))
    }
    catch {
      case error: Exception =>
        Console.err.println(s"Warning: unable to read $path: ${error.getMessage}")
        None
    }

  def symbols(info: Info, file: String): List[SymbolRow] = {
    val classId = Ids.in(info.name).mkString(",")
    val classRow = SymbolRow("class", info.name, info.name, classId, "", s"class ${info.name}" + (if (info.superName.nonEmpty) s" extends ${info.superName}" else ""), file)
    val fieldRows = info.fields.map { field =>
      val symbol = s"${info.name}.${field.name}"
      SymbolRow("field", info.name, symbol, Ids.in(symbol).mkString(","), field.descriptor, s"${descriptorType(field.descriptor)} ${field.name}", file)
    }
    val methodRows = info.methods.map { method =>
      val symbol = s"${info.name}::${method.name}"
      val (parameters, result) = methodDescriptor(method.descriptor)
      SymbolRow("method", info.name, symbol, Ids.in(symbol).mkString(","), method.descriptor, s"${method.name}(${parameters.mkString(", ")}): $result", file)
    }
    classRow :: fieldRows ::: methodRows
  }

  private def descriptorType(descriptor: String): String = {
    val (_, result) = methodDescriptor(s"()$descriptor")
    result
  }

  private def methodDescriptor(descriptor: String): (List[String], String) = {
    def readType(start: Int): (String, Int) = {
      val primitives = Map('B' -> "byte", 'C' -> "char", 'D' -> "double", 'F' -> "float", 'I' -> "int", 'J' -> "long", 'S' -> "short", 'Z' -> "boolean", 'V' -> "void")
      descriptor(start) match {
        case '[' =>
          val (element, next) = readType(start + 1)
          (s"$element[]", next)
        case 'L' =>
          val end = descriptor.indexOf(';', start)
          (descriptor.substring(start + 1, end).replace('/', '.'), end + 1)
        case code => (primitives.getOrElse(code, code.toString), start + 1)
      }
    }

    if (!descriptor.startsWith("(")) return (Nil, descriptor)
    val end = descriptor.indexOf(')')
    if (end < 0) return (Nil, descriptor)
    var cursor = 1
    var parameters = List.empty[String]
    while (cursor < end) {
      val (parameter, next) = readType(cursor)
      parameters = parameters :+ parameter
      cursor = next
    }
    val (result, _) = readType(end + 1)
    (parameters, result)
  }
}
