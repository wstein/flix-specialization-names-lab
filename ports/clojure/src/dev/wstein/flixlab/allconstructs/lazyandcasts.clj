(ns dev.wstein.flixlab.allconstructs.lazyandcasts
  "Section 15: Lazy Evaluation, Unsafe & Checked/Unchecked Casts. `delay`/
  `force` (here, deref via `@`) are Clojure's native, first-class lazy
  value and forcing operation -- a direct, unapproximated mapping of
  Flix's `lazy`/`force`.

  Flix's `unsafe IO { ... }` block discharges a *static* effect-typing
  obligation Clojure function signatures never carry -- there is nothing
  to unsafely bypass, a genuine \"no equivalent\" gap. Dynamic typing also
  means there is no static type to unsafely cast away: a Clojure call is
  checked at the point of use (an arity or method-not-found error), not
  via a separate cast expression.")

(defn lazy-and-casts-demo []
  (let [lazy-val (delay (+ 10 20))
        forced-val @lazy-val]
    (println "Evaluating (Clojure has no static effect tracking to bypass)")
    forced-val))
