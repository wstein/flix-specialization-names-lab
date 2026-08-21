(ns dev.wstein.flixlab.allconstructs.functions
  "Section 9: Functions, Currying, Pipelines, Lambdas & Tail Recursion. The
  `->` threading macro is the native counterpart of Flix's `|>`. `recur`
  gives a *stronger* tail-call guarantee than Flix's own: the compiler
  rejects a `recur` that isn't actually in tail position, rather than
  silently leaving it unoptimized.")

(defn add [x y]
  (+ x y))

(defn curried-multiply [x]
  (fn [y] (* x y)))

(defn generic-identity [x]
  x)

(defn higher-order-demo [f x]
  (f x))

(defn pipeline-demo [x]
  (-> x (+ 5) (* 2)))

(defn binary-pipeline-demo [a b]
  (add a b))

(defn factorial [n acc]
  (if (<= n 1)
    acc
    (recur (dec n) (* n acc))))
