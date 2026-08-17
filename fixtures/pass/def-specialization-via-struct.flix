// This is NOT an example of the struct-specialization family, despite how it might look.
// It was written as one and turned out to be something else: `DemoBox` (the struct
// declaration) never mints its own per-instantiation class here -- its runtime value
// representation is a shared, field-shape-keyed class (`Struct$Int32`, `Struct$Obj`, ...)
// with no id and no connection to the struct's declared name, the same kind of generic
// scaffolding as a closure's `Clo1$Bool$Obj`. What genuinely gets specialized per call
// site is `roundTrip` -- a def, not the struct -- so this is the same
// `SpecializationKey`-keyed family as specialized-def.flix, just demonstrated through a
// def that mutates a struct field in a region rather than one that formats a value.
// Kept as a separate fixture anyway because that interaction (specialization through a
// region-scoped mutation) is worth having isolated on its own.
//
// docs/adr/0001-content-addressed-specialization-names.md's "five id-minting families"
// table is being corrected to match: what actually triggers `ErasureKey.ofStruct` for a
// struct's own class, if anything does under this compiler's current behavior, is still
// open -- see that ADR section for the current state of that question.
struct DemoBox[a, r] {
    mut value: a
}

mod DemoBox {
    pub def roundTrip(v: a, rc: Region[r]): a \ r = {
        let b = new DemoBox @ rc { value = v };
        b->value = v;
        b->value
    }
}

def main(): Unit \ IO = region rc {
    let i = DemoBox.roundTrip(42, rc);
    let s = DemoBox.roundTrip("boxed", rc);
    let b = DemoBox.roundTrip(true, rc);
    println("${i} ${s} ${b}")
}
