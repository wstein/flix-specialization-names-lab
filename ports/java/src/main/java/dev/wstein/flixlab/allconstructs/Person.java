package dev.wstein.flixlab.allconstructs;

/**
 * Section 5: Structs &amp; Companion Module (Internal Mutability with Regions).
 *
 * <p>Flix allocates {@code Person} in a region {@code r}, whose lifetime the compiler checks
 * statically. Java has no equivalent static region checking; the nearest idiomatic approximation is
 * {@link AutoCloseable} + try-with-resources -- the *shape* of "construct, mutate, use, then let go
 * out of scope" survives, but nothing here is checked the way Flix's region typing is. That gap is
 * deliberate, not an oversight.
 */
public final class Person implements AutoCloseable {
  public final String name;
  public int age;

  public Person(String name, int age) {
    this.name = name;
    this.age = age;
  }

  @Override
  public void close() {
    // No real resource to release -- this exists only so Person can be
    // used in try-with-resources, approximating Flix's region-scoped
    // lifetime.
  }

  public record NameAge(String name, int age) {}

  public static NameAge makeAndAge(String name, int age) {
    try (Person p = new Person(name, age)) {
      p.age = p.age + 1;
      return new NameAge(p.name, p.age);
    }
  }

  public static NameAge structDemo() {
    return makeAndAge("Alice", 30);
  }
}
