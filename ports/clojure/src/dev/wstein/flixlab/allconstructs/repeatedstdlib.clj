(ns dev.wstein.flixlab.allconstructs.repeatedstdlib
  "Section 16: Specialization Coverage -- repeated stdlib calls. Applies
  `map`/`filter` at many distinct element types. Clojure's `map` and
  `filter` are single, ungeneric functions operating on seqs of anything --
  there is no per-element-type specialization to observe, unlike Flix's
  specializer minting one copy of the underlying library def per element
  type.")

(defn repeated-stdlib-demo []
  (let [a (count (map inc [1 2 3]))
        b (count (map inc [1 2]))
        c (count (map #(str % "!") ["p" "q"]))
        d (count (map not [true false]))
        e (count (map #(* % 2.0) [1.0 2.0]))
        f (count (filter #(> % 1) [1 2 3]))
        g (count (filter #(> (count %) 1) ["y" "zz"]))
        h (count (map inc [1 2 3]))
        i (count (map str [\a \b]))]
    (+ a b c d e f g h i)))
