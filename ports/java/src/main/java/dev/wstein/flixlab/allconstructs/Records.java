package dev.wstein.flixlab.allconstructs;

/**
 * Section 10: Records (Construction, Extension, Restriction, Update).
 *
 * <p>Flix's anonymous records support structural extension ({@code +z}), update ({@code x = 20 |
 * r}), and restriction ({@code -y}) on values of literally any matching shape. Java {@code record}s
 * are nominal and fixed-shape: "extension" and "restriction" have to become genuinely different
 * types, built by hand from the previous one's fields, rather than a single value growing and
 * shrinking in place. There is no {@code copy()} either; a new record is constructed directly for
 * the "update".
 *
 * <p>The row-polymorphic {@code getX} -- "any record with an {@code x} field, whatever else it
 * carries" -- is approximated with a {@link HasX} interface that each record explicitly implements,
 * rather than Flix's implicit structural row typing.
 */
public final class Records {
  private Records() {}

  public record Base(int x, String y) {}

  public record Extended(int x, String y, double z) {}

  public record Restricted(int x, double z) {}

  public record RecordDemoResult(int x, String y, double z) {}

  public interface HasX<A> {
    A x();
  }

  public record PolyRecord(Integer x, String extra) implements HasX<Integer> {}

  public static RecordDemoResult recordDemo() {
    Base base = new Base(10, "Flix");
    Extended extended = new Extended(base.x(), base.y(), 3.14);
    Extended updated = new Extended(20, extended.y(), extended.z());
    Restricted restricted = new Restricted(updated.x(), updated.z());
    return new RecordDemoResult(updated.x(), updated.y(), updated.z());
  }

  public static <A> A getX(HasX<A> r) {
    return r.x();
  }
}
