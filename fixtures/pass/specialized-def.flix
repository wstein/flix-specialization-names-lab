// Specialized def: one polymorphic def, called at several distinct type arguments.
// Each call site becomes its own generated class, its own DefnSym, keyed on the def
// plus the resolved type. See docs/adr/0001-content-addressed-specialization-names.md,
// "The five id-minting families" -- this is the `SpecializationKey`-keyed family.
//
// A trivial identity function is not enough to demonstrate this: the compiler inlines
// `x -> x` away entirely rather than emitting a class per call site (found the hard way
// while building scripts/collision-stress.scala). `describeShape` cannot be inlined
// away like that, since it depends on the caller's `ToString` instance at each type.
def describeShape(x: a, label: String): String with ToString[a] = "${label}=${x}"

def main(): Unit \ IO =
    println(describeShape(1, "i32") + " " + describeShape("s", "str") + " " + describeShape(true, "bool"))
