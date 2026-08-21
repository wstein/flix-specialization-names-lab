(ns dev.wstein.flixlab.allconstructs.liftedclosures
  "Section 16: Specialization Coverage -- lifted closures.

  Both closures capture `n`; Clojure compiles each `fn` literal to its own
  class regardless, so there is no separate \"specialization\" step to
  observe here the way there is in Flix's lambda-lifting pass.")

(defn lifted-closures [n]
  (let [f (fn [x] (+ x n))
        g (fn [y] (* y n))]
    [f g]))

(defn lifted-closures-demo []
  (let [[f g] (lifted-closures 3)]
    (+ (f 10) (g 10))))
