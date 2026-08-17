// Duplicate trait derivation on an enum -- tests that duplicate derivations in a `with`
// clause are reported cleanly at their respective source locations rather than being
// silently deduplicated before validation.
enum Color with Eq, Eq {
    case Red
}

def main(): Unit \ IO = println("unreachable")
