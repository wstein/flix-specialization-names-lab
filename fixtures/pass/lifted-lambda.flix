// Lifted lambda / closure: a lambda that captures a variable and escapes its enclosing
// def (by being returned), rather than being inlined into its only use. See docs/adr/
// 0001-content-addressed-specialization-names.md, "The five id-minting families" -- this
// is the `<enclosing symbol>#lift<index>`-keyed family: LambdaLift numbers a lambda by
// its position within the def it was lifted from, not by a program-wide counter.
//
// Both properties matter for the fixture to actually exercise this: `f` and `g` both
// capture `n` (so they become closures, not plain top-level functions), and both escape
// by being returned (so neither is inlined away into a single call site).
def liftedClosures(n: Int32): (Int32 -> Int32, Int32 -> Int32) =
    let f = x -> x + n;
    let g = y -> y * n;
    (f, g)

def main(): Unit \ IO =
    let (f, g) = liftedClosures(3);
    println("${f(10)} ${g(10)}")
