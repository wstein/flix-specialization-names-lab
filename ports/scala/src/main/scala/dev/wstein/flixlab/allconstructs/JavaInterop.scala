package dev.wstein.flixlab.allconstructs

/** Section 7: Java Interoperability & Exception Handling. Scala runs on the JVM and calls Java APIs
  * directly, with `java.lang` in scope by default -- unlike Flix, which needs an explicit `import
  * java.lang.Math`. No approximation needed.
  */
def javaInteropDemo(): String =
  val _absVal = Math.abs(-42)
  val sb = new StringBuilder()
  sb.append("Hello from ")
  sb.append("Java Interop!")
  sb.toString()

def javaExceptionDemo(): String =
  try
    val sb = new StringBuilder()
    sb.append("Try/Catch demo")
    sb.toString()
  catch case _: Exception => "Caught Java Exception"
