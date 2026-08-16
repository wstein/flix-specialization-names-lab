#!/usr/bin/env -S scala-cli shebang
//> using scala "2.13.16"

import java.io.{DataInputStream, BufferedInputStream}
import java.nio.file.{Files, Path, Paths}
import scala.collection.JavaConverters._
import scala.util.Using

/**
 * Extracts generated IDs and JVM symbols from an existing class directory.
 *
 * Usage: ./trace-fresh-ids.scala [class-directory] [--output-dir directory]
 *
 * The script never invokes the Flix compiler. It supports both decimal GenSym
 * suffixes from older output and fixed-width base-36 SHA-256 suffixes from newer output.
 */
object TraceFreshIds extends App {
  val (classDir, outputDir) = Arguments.parse(args.toList)
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
  val symbolsFile = outputDir.resolve("generated-class-symbols.csv")
  val idsFile = outputDir.resolve("generated-class-ids.txt")

  Files.writeString(symbolsFile, Csv.header + System.lineSeparator + rows.map(_.csv).mkString(System.lineSeparator()) + (if (rows.nonEmpty) System.lineSeparator() else ""))
  Files.writeString(idsFile, ids.toList.sortBy(Ids.order).mkString("", System.lineSeparator(), if (ids.nonEmpty) System.lineSeparator() else ""))

  val counters = ids.count(Ids.isCounter)
  println(s"Generated-class ID census")
  println(s"  directory: $classDir")
  println(s"  class files: ${classFiles.size}")
  println(s"  symbols: ${rows.size}")
  println(s"  sequential counter IDs: $counters")
  println(s"  SHA-256 hash-derived IDs: ${ids.size - counters}")
  println(s"  symbols CSV: $symbolsFile")
  println(s"  IDs list: $idsFile")
}

object Arguments {
  def parse(args: List[String]): (Path, Path) = args match {
    case Nil => (Paths.get("build/class"), Paths.get("."))
    case classDir :: Nil if !classDir.startsWith("-") => (Paths.get(classDir), Paths.get("."))
    case classDir :: "--output-dir" :: outputDir :: Nil => (Paths.get(classDir), Paths.get(outputDir))
    case classDir :: "-o" :: outputDir :: Nil => (Paths.get(classDir), Paths.get(outputDir))
    case _ =>
      Console.err.println("Usage: trace-fresh-ids.scala [class-directory] [--output-dir directory]")
      sys.exit(2)
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
