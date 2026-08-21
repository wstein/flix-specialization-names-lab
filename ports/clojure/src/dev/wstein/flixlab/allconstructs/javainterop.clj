(ns dev.wstein.flixlab.allconstructs.javainterop
  "Section 7: Java Interoperability & Exception Handling. Clojure has
  first-class, essentially zero-ceremony Java interop -- `.method` calls,
  `Class/staticMethod`, and `(Class.)` constructors -- so this section is
  trivial: no approximation needed, and less ceremony than even Flix's own
  `import java.lang.Math`.")

(defn java-interop-demo []
  (let [abs-val (Math/abs -42)
        sb (StringBuilder.)]
    (.append sb "Hello from ")
    (.append sb "Java Interop!")
    (.toString sb)))

(defn java-exception-demo []
  (try
    (let [sb (StringBuilder.)]
      (.append sb "Try/Catch demo")
      (.toString sb))
    (catch Exception _
      "Caught Java Exception")))
