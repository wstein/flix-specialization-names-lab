(ns dev.wstein.flixlab.allconstructs.adts
  "Section 3: Algebraic Data Types (Enums) & Restrictable Enums.

  `Color` is plain keywords -- any of `:red`/`:green`/`:blue` already *is* a
  valid case, matched with `case`. `Tree` and `SimpleResult` are tagged maps
  with constructor functions, Clojure's idiomatic stand-in for a closed sum
  type when there's no static exhaustiveness check to lean on.

  `Shape` approximates Flix's *restrictable* enum, which lets callers be
  polymorphic over an open subset of cases (via `choose`). Clojure is
  dynamically typed, so there is no compile-time restriction to approximate
  at all -- `make-shape` validates its `:kind` against `shape-kinds` at
  construction *at runtime* instead, which is the honest limit of the
  approximation.")

(defn leaf [value]
  {:tag :leaf :value value})

(defn node [left right]
  {:tag :node :left left :right right})

(defn ok [value]
  {:tag :ok :value value})

(defn err [error]
  {:tag :err :error error})

(def shape-kinds #{:circle :square})

(defn make-shape [kind data]
  {:pre [(contains? shape-kinds kind)]}
  (assoc data :kind kind))

(defn circle [radius]
  (make-shape :circle {:radius radius}))

(defn square [side]
  (make-shape :square {:side side}))

(defn shape-area [s]
  (case (:kind s)
    :circle (* 3.14159 (:radius s) (:radius s))
    :square (* (:side s) (:side s))))
