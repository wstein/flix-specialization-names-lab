package dev.wstein.flixlab.allconstructs

/**
 * Section 7: Java Interoperability & Exception Handling. Kotlin runs on the JVM and calls Java
 * APIs directly, with `java.lang` in scope by default -- unlike Flix, which needs an explicit
 * `import java.lang.Math`. No approximation needed.
 */
fun javaInteropDemo(): String {
    val absVal = Math.abs(-42)
    val sb = StringBuilder()
    sb.append("Hello from ")
    sb.append("Java Interop!")
    return sb.toString()
}

fun javaExceptionDemo(): String =
    try {
        val sb = StringBuilder()
        sb.append("Try/Catch demo")
        sb.toString()
    } catch (e: Exception) {
        "Caught Java Exception"
    }
