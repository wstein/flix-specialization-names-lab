(ns dev.wstein.flixlab.allconstructs.literals
  "Section 12: Literals, Collections & Monadic For (forM). Clojure literals
  and collection constructors are native. Core Clojure's `some->`/`some->>`
  only thread a *single* value through a pipeline of transformations, short-
  circuiting to nil at the first nil step -- they don't combine two
  independent optional sources the way `forM`'s `x <- Some(10); y <-
  Some(20)` does, so `and`/`when` combines the two sources here instead, a
  closer semantic match than forcing an unrelated single-value thread.")

(defn literals-demo []
  (let [unit-val nil ; Clojure's nearest analogue of Unit -- the value with no information
        bool-val true
        char-val \F
        i8-val (byte 8)
        i16-val (short 16)
        i32-val (int 32)
        i64-val (long 64)
        bi-val (biginteger 128)
        f32-val (float 3.14)
        f64-val 6.28
        bd-val (bigdec 12.34)
        str-val (str "Hello " 42)
        regex-val #"^[a-zA-Z]+$"]
    i32-val))

(defn collections-demo []
  (let [list1 (list 1 2 3)
        vec1 [4 5 6]
        set1 #{1 2 3}
        map1 {1 "one" 2 "two"}
        x 10
        y 20
        opt-result (when (and x y) (+ x y))]
    (if (nil? opt-result)
      list1
      (cons opt-result list1))))
