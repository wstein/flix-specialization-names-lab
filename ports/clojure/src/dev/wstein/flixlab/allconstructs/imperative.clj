(ns dev.wstein.flixlab.allconstructs.imperative
  "Section 14: Imperative Constructs, Foreach Loops & References in
  Regions. Flix's `Ref` cell in a region becomes a plain atom -- the same
  atom-scoped approximation as section 5's `Person`. `doseq`'s `:when`
  modifier is a native, unapproximated match for Flix's filtered
  `foreach (item <- xs; if cond)`.")

(defn imperative-demo []
  (let [total (atom 0)]
    (doseq [item [1 2 3 4 5]]
      (swap! total + item))
    (doseq [item [10 20 30]
            :when (> item 15)]
      (swap! total + item))
    @total))
