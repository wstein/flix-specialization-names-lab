(ns dev.wstein.flixlab.allconstructs.patternmatching
  "Section 11: Pattern Matching & Guards. `case`/`cond` over a tagged map's
  keys covers Flix's pattern matching and guards without a dedicated
  pattern-matching syntax -- there is no `core.match` dependency here, by
  design (see docs/ports.md).")

(defn pattern-match-demo [x]
  (case (:tag x)
    :ok (let [v (:value x)]
          (cond
            (> v 100) (str "Large Ok: " v)
            (= v 0) "Zero Ok"
            (= v 1) "One Ok"
            :else (str "Normal Ok: " v)))
    :err (str "Error: " (:error x))))

(defn tuple-pattern-match [p]
  (let [[n s b] p]
    (cond
      (= n 0) "Zero fst"
      b (str s " true (" n ")")
      :else "False thd")))
