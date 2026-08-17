// Two instances of the same trait at the same type, byte-identical bodies -- the sharper
// version of "duplicate declaration" for instances specifically: they aren't named the
// way a def/enum/struct/trait is, so a frontend has to identify them by (trait, type)
// instead, and must still report both at their own distinct source locations rather than
// silently picking one.
pub trait Describable[t] {
    pub def describe(x: t): String
}

instance Describable[Int32] {
    pub def describe(x: Int32): String = "Int32(${x})"
}

instance Describable[Int32] {
    pub def describe(x: Int32): String = "Int32(${x})"
}

def main(): Unit \ IO = println("unreachable")
