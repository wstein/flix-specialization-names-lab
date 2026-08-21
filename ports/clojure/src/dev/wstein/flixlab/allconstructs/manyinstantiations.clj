(ns dev.wstein.flixlab.allconstructs.manyinstantiations
  "Section 16: Specialization Coverage -- many instantiations.

  `describeShape` called at twelve distinct type arguments would mint
  twelve distinct generated defs in Flix. Clojure's `describe-shape` is a
  single function with no type parameter to specialize at all -- every
  call below is the same dynamic dispatch, regardless of argument type.")

(defn describe-shape [x label]
  (str label "=" x))

(defn many-instantiations []
  (let [parts [(describe-shape 1 "i32")
               (describe-shape (byte 2) "i8")
               (describe-shape (short 3) "i16")
               (describe-shape (long 4) "i64")
               (describe-shape (float 5.0) "f32")
               (describe-shape 6.0 "f64")
               (describe-shape \c "char")
               (describe-shape true "bool")
               (describe-shape "str" "string")
               (describe-shape :red "enum")
               (describe-shape 9 "option")
               (describe-shape [1 2] "list")]]
    (count parts)))
