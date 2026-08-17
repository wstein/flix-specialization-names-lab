// Enum case: a generic enum instantiated at several distinct types. See docs/adr/
// 0001-content-addressed-specialization-names.md, "The five id-minting families" -- this
// is the `ErasureKey.ofEnum(sym, targs)`-keyed family, computed in the Eraser phase from
// the enum's erased type arguments.
//
// Specifically, this demonstrates it through the zero-payload case, `Empty`: that one
// becomes the generated `.class` file name directly, `Wrapper$<id>$Empty`, one per
// instantiation. `Value(a)`, the payload-carrying case, does not -- checked directly, the
// same way the struct family's caveat in the ADR was checked, rather than assumed by
// analogy. It boxes its payload through the same shared, shape-keyed `Tag$<shape>` carrier
// (`Tag$Int32`, `Tag$Obj`, ...) the struct family uses for its own fields, not through a
// nominal class of its own. A singleton case needs its own distinct object per
// instantiation to satisfy typing; a payload-carrying one does not.
//
// A single-case enum is not enough to demonstrate even the `Empty` half of this: with
// only one possible case there is no discriminant to carry, so the compiler elides the
// wrapper as a newtype rather than emitting a class for it (found the same way as the
// identity-function trap in specialized-def.flix -- by trying the simplest version first
// and finding nothing there to census). Two cases, matching the standard library's own
// `Option`/`List` shape, is what keeps this from being erased away.
enum Wrapper[a] with ToString {
    case Value(a),
    case Empty
}

def main(): Unit \ IO =
    println("${Wrapper.Value(1)} ${Wrapper.Value("s")} ${Wrapper.Value(true)} ${(Wrapper.Empty: Wrapper[Int32])}")
