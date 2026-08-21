package dev.wstein.flixlab.allconstructs;

/**
 * Section 7: Java Interoperability &amp; Exception Handling. This port *is* Java, so this section
 * needs no approximation at all -- Flix's explicit {@code import java.lang.Math} becomes nothing,
 * since {@code java.lang} is already implicitly in scope.
 */
public final class JavaInterop {
  private JavaInterop() {}

  public static String javaInteropDemo() {
    int absVal = Math.abs(-42);
    StringBuilder sb = new StringBuilder();
    sb.append("Hello from ");
    sb.append("Java Interop!");
    return sb.toString();
  }

  public static String javaExceptionDemo() {
    try {
      StringBuilder sb = new StringBuilder();
      sb.append("Try/Catch demo");
      return sb.toString();
    } catch (Exception e) {
      return "Caught Java Exception";
    }
  }
}
