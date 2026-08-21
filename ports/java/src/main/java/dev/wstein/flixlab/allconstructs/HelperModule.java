package dev.wstein.flixlab.allconstructs;

/**
 * Section 1: Modules, Visibility &amp; Imports.
 *
 * <p>Flix's {@code mod} gives a nested, independently importable namespace; a Java class with a
 * private constructor and static members is the nearest native equivalent -- reached by qualified
 * access, same as {@code use HelperModule.helper} on the Flix side.
 */
public final class HelperModule {
  private HelperModule() {}

  public static String helper() {
    return "HelperModule.helper";
  }

  public static int moduleValue() {
    return 42;
  }
}
