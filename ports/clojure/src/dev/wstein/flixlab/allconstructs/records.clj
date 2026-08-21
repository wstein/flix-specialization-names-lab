(ns dev.wstein.flixlab.allconstructs.records
  "Section 10: Records (Construction, Extension, Restriction, Update).

  Clojure maps are *already* row-polymorphic: `assoc` extends, `assoc`
  again updates, `dissoc` restricts, and `:x` looks a key up on any map
  that has one, whatever else it carries. This is the one port where
  section 10's `getX` needs no approximation at all -- no `HasX`-style
  interface, because there is no nominal record type to satisfy in the
  first place.")

(defn record-demo []
  (let [base {:x 10 :y "Flix"}
        extended (assoc base :z 3.14)
        updated (assoc extended :x 20)
        restricted (dissoc updated :y)]
    [(:x updated) (:y updated) (:z updated)]))

(defn get-x [r]
  (:x r))
