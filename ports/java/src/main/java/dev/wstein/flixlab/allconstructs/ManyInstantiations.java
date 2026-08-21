package dev.wstein.flixlab.allconstructs;

import java.util.List;
import java.util.Optional;

/**
 * Section 16: Specialization Coverage -- many instantiations.
 *
 * <p>{@code describeShape} called at twelve distinct type arguments would mint twelve distinct
 * generated defs in Flix. Under JVM erasure this compiles to one generic method, called twelve
 * times -- there is nothing further to specialize.
 */
public final class ManyInstantiations {
  private ManyInstantiations() {}

  public static <A> String describeShape(A x, String label) {
    return label + "=" + x;
  }

  public static int manyInstantiations() {
    List<String> parts =
        List.of(
            describeShape(1, "i32"),
            describeShape((byte) 2, "i8"),
            describeShape((short) 3, "i16"),
            describeShape(4L, "i64"),
            describeShape(5.0f, "f32"),
            describeShape(6.0, "f64"),
            describeShape('c', "char"),
            describeShape(true, "bool"),
            describeShape("str", "string"),
            describeShape(Color.RED, "enum"),
            describeShape(Optional.of(9), "option"),
            describeShape(List.of(1, 2), "list"));
    return parts.size();
  }
}
