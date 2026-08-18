// Minimal fixture exercising derived-instance def id minting.
// In Deriver.scala, derived instance defs (e.g. ToString.toString, Eq.eq) mint a
// content-addressed SymId.Hash via Symbol.memberDefnSym/StableName, the same helper
// Resolver.resolveInstance uses for a written instance member, keyed on the looked-up
// trait symbol rather than the raw derivation name.
enum Color with ToString, Eq {
    case Red,
    case Green,
    case Blue
}

def main(): Unit \ IO = {
    println(ToString.toString(Color.Red));
    println(ToString.toString(Color.Green));
    println(Eq.eq(Color.Red, Color.Red));
    println(Eq.eq(Color.Red, Color.Blue))
}
