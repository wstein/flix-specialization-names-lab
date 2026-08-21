(ns dev.wstein.flixlab.allconstructs.anonclass
  "Section 16: Specialization Coverage -- anonymous classes. Clojure's
  `reify` instantiates an anonymous instance of one or more interfaces on
  the spot -- the same construct Flix's demo exercises, reached through
  different syntax. A direct, unapproximated mapping."
  (:import (java.util Comparator)
           (java.util.function Supplier)))

(defn anon-class-demo []
  (let [supplier (reify Supplier
                   (get [_] "from-anon-class"))
        cmp (reify Comparator
              (compare [_ a b] (- (count a) (count b))))
        v (.get supplier)
        c (.compare cmp "aa" "b")]
    (str v "/" c)))
