// Minimal fixture exercising derived-instance def id minting.
// In Deriver.scala, derived instance defs (e.g. ToString.toString, Eq.eq)
// mint a content-addressed SymId.Hash via StableName.suffix.
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
