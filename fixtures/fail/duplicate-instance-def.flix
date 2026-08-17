// Duplicate definitions of the same def within an instance body -- tests that duplicate
// member definitions inside an instance are reported cleanly at their distinct source
// locations rather than being silently merged or dropped.
pub trait C[a] {
    pub def f(x: a): a
}

instance C[String] {
    pub def f(x: String): String = x
    pub def f(x: String): String = x
}

instance C[Int32] {
    pub def f(x: Int32): Int32 = x
    pub def f(x: Int32): Int32 = x
}

def main(): Unit \ IO = println("unreachable")
