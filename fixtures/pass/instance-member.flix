// Minimal fixture exercising instance-member specialization id minting.
// In Namer.scala, instance defs mint a content-addressed SymId.Hash via StableName.suffix.
pub trait Greet[t] {
    pub def greet(x: t): String
}

instance Greet[Int32] {
    pub def greet(x: Int32): String = "Int32: ${x}"
}

instance Greet[String] {
    pub def greet(x: String): String = "String: ${x}"
}

def main(): Unit \ IO = {
    println(Greet.greet(42));
    println(Greet.greet("hello"))
}
