// Two struct declarations, same name, byte-identical field lists -- the sharper version
// of "duplicate declaration": a naive content-addressed frontend could see these as
// indistinguishable and merge, drop, or misattribute one. The compiler must still report
// both at their own distinct source locations.
//
// Checked directly: it reports three separate duplicates here (the struct name `Point`
// plus each of its two fields), each correctly citing its own pair of distinct locations.
// The field-name diagnostics render with a leading "€" (e.g. "Duplicate definition:
// '€x'") -- looks like an internal struct-field-accessor namespacing prefix leaking into
// a user-facing message. Cosmetic, not a correctness issue: the location tracking this
// fixture exists to check is unaffected either way.
struct Point[r] {
    x: Int32,
    y: Int32
}

struct Point[r] {
    x: Int32,
    y: Int32
}

def main(): Unit \ IO = println("unreachable")
