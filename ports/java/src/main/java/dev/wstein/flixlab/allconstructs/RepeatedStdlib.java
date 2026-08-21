package dev.wstein.flixlab.allconstructs;

import java.util.List;

/**
 * Section 16: Specialization Coverage -- repeated stdlib calls. Applies {@code Stream.map}/{@code
 * filter} at many distinct element types. Java has one {@code List} type (no separate Vector/List
 * distinction the way Flix and Scala have), so both of Flix's {@code List.map} and {@code
 * Vector.map} calls become {@code List} calls here. Under erasure, {@code Stream<A>.map} is one
 * generic method for every {@code A}; Flix's specializer instead mints one copy of the underlying
 * library def per element type.
 */
public final class RepeatedStdlib {
  private RepeatedStdlib() {}

  public static int repeatedStdlibDemo() {
    int a = (int) List.of(1, 2, 3).stream().map(x -> x + 1).count();
    int b = (int) List.of(1L, 2L).stream().map(x -> x + 1L).count();
    int c = (int) List.of("p", "q").stream().map(x -> x + "!").count();
    int d = (int) List.of(true, false).stream().map(x -> !x).count();
    int e = (int) List.of(1.0, 2.0).stream().map(x -> x * 2.0).count();
    int f = (int) List.of(1, 2, 3).stream().filter(x -> x > 1).count();
    int g = (int) List.of("y", "zz").stream().filter(x -> x.length() > 1).count();
    int h = (int) List.of(1, 2, 3).stream().map(x -> x + 1).count();
    int i = (int) List.of('a', 'b').stream().map(String::valueOf).count();
    return a + b + c + d + e + f + g + h + i;
  }
}
