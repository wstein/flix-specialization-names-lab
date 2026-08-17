// Two trait declarations, same name, byte-identical member signatures -- the sharper
// version of "duplicate declaration": a naive content-addressed frontend could see these
// as indistinguishable and merge, drop, or misattribute one. The compiler must still
// report both at their own distinct source locations.
pub trait Describable[t] {
    pub def describe(x: t): String
}

pub trait Describable[t] {
    pub def describe(x: t): String
}

def main(): Unit \ IO = println("unreachable")
