(ns dev.wstein.flixlab.allconstructs.structs
  "Section 5: Structs & Companion Module (Internal Mutability with Regions).

  Flix allocates `Person` in a region `r`, whose lifetime the compiler
  checks statically. Clojure has no equivalent static region checking; the
  nearest idiomatic approximation is an atom scoped to a `let` block -- the
  *shape* of \"construct, mutate, use, then let go out of scope\" survives,
  but nothing here is checked the way Flix's region typing is. That gap is
  deliberate, not an oversight.")

(defn make-and-age [name age]
  (let [person (atom {:name name :age age})]
    (swap! person update :age inc)
    [(:name @person) (:age @person)]))

(defn struct-demo []
  (make-and-age "Alice" 30))
