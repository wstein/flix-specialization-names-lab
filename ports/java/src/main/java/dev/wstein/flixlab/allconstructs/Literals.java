package dev.wstein.flixlab.allconstructs;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Section 12: Literals, Collections &amp; Monadic For (forM). Java's own primitive types and
 * collection factories are native; {@code forM}'s monadic {@code Option} chaining becomes a nested
 * {@link Optional#flatMap}.
 */
public final class Literals {
  private Literals() {}

  public static int literalsDemo() {
    // Java has no first-class Unit type -- void is a return-type-only
    // marker, not a value, so there is nothing to bind here.
    boolean boolVal = true;
    char charVal = 'F';
    byte i8Val = (byte) 8;
    short i16Val = (short) 16;
    int i32Val = 32;
    long i64Val = 64L;
    BigInteger biVal = BigInteger.valueOf(128);
    float f32Val = 3.14f;
    double f64Val = 6.28;
    BigDecimal bdVal = new BigDecimal("12.34");
    String strVal = "Hello " + 42;
    Pattern regexVal = Pattern.compile("^[a-zA-Z]+$");
    return i32Val;
  }

  public static List<Integer> collectionsDemo() {
    List<Integer> list1 = List.of(1, 2, 3);
    List<Integer> vec1 = List.of(4, 5, 6);
    Set<Integer> set1 = Set.of(1, 2, 3);
    Map<Integer, String> map1 = Map.of(1, "one", 2, "two");

    Optional<Integer> optResult = Optional.of(10).flatMap(x -> Optional.of(20).map(y -> x + y));

    if (optResult.isEmpty()) {
      return list1;
    }
    List<Integer> result = new ArrayList<>();
    result.add(optResult.get());
    result.addAll(list1);
    return result;
  }
}
