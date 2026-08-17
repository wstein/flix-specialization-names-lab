// Two enum declarations, same name, byte-identical bodies -- the sharper version of
// "duplicate declaration": a naive content-addressed frontend could see these as
// indistinguishable and merge, drop, or misattribute one. The compiler must still report
// both at their own distinct source locations. See scripts/duplicate-decl-stress for the
// same property stress-tested at scale, for defs specifically; this is the enum case that
// script does not reach.
enum Status {
    case Active,
    case Inactive
}

enum Status {
    case Active,
    case Inactive
}

def main(): Unit \ IO = println("unreachable")
