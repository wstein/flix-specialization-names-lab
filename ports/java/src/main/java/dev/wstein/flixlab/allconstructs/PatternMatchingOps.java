package dev.wstein.flixlab.allconstructs;

/**
 * Section 11: Pattern Matching &amp; Guards. Java 21's pattern-matching {@code switch} with record
 * deconstruction and {@code when} guards (JEP 440/441) is at least as expressive as Flix's own here
 * -- no approximation needed.
 */
public final class PatternMatchingOps {
  private PatternMatchingOps() {}

  public record Triple(int fst, String snd, boolean thd) {}

  public static String patternMatchDemo(SimpleResult<Integer, String> x) {
    return switch (x) {
      case SimpleResult.Ok<Integer, String>(Integer v) when v > 100 -> "Large Ok: " + v;
      case SimpleResult.Ok<Integer, String>(Integer v) when v == 0 -> "Zero Ok";
      case SimpleResult.Ok<Integer, String>(Integer v) when v == 1 -> "One Ok";
      case SimpleResult.Ok<Integer, String>(Integer v) -> "Normal Ok: " + v;
      case SimpleResult.Err<Integer, String>(String msg) -> "Error: " + msg;
    };
  }

  public static String tuplePatternMatch(Triple p) {
    return switch (p) {
      case Triple(int n, String s, boolean b) when n == 0 -> "Zero fst";
      case Triple(int n, String s, boolean b) when b -> s + " true (" + n + ")";
      case Triple(int n, String s, boolean b) -> "False thd";
    };
  }
}
