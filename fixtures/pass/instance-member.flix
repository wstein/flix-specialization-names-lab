// Minimal fixture exercising instance-member specialization id minting -- and specifically
// the qualified-vs-simple trait reference path fixed in Resolver.scala's resolveInstance
// (fork commit 6a298af02): a member's id used to be keyed on the trait name and head type
// *as written*, so `instance M.Greet[Int32]` (self-qualified) and `instance Greet[String]`
// (the simple name, equally in scope inside M's own declaration) minted inconsistently --
// the same trait, spelled two ways, is not two traits. Both spellings appear below, both
// declared alongside the trait itself to satisfy the orphan-instance rule, so this fixture
// actually exercises that site, which nothing in this lab previously did: both instances
// mint via Symbol.memberDefnSym/StableName, keyed on the *resolved* trait symbol and head
// type, regardless of which name reached the instance.
mod M {
    pub trait Greet[t] {
        pub def greet(x: t): String
    }

    instance M.Greet[Int32] {
        pub def greet(x: Int32): String = "Int32: ${x}"
    }

    instance Greet[String] {
        pub def greet(x: String): String = "String: ${x}"
    }
}

def main(): Unit \ IO = {
    println(M.Greet.greet(42));
    println(M.Greet.greet("hello"))
}
