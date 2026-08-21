(ns dev.wstein.flixlab.allconstructs.demobox
  "Section 16: Specialization Coverage -- a polymorphic \"struct\".

  In Flix, `DemoBox[a, r]` specialized at ten distinct element types mints
  ten distinct generated classes. Clojure never had static generics to
  specialize in the first place: `demo-box-round-trip` is a single
  function, and every call below is the same dynamic dispatch regardless
  of argument type. There is no erasure story to document here either --
  there was nothing to erase, because there was nothing generic to compile
  away. This is the sharpest contrast point in the whole comparison.")

(defn demo-box-round-trip [v]
  (let [b (atom v)]
    (reset! b v)
    @b))

(defn box-demo []
  (let [i (demo-box-round-trip 42)
        s (demo-box-round-trip "boxed")
        b (demo-box-round-trip true)
        c (demo-box-round-trip \x)
        f (demo-box-round-trip 2.5)
        l (demo-box-round-trip 7)
        col (demo-box-round-trip :blue)
        opt (demo-box-round-trip 1)
        lst (demo-box-round-trip ["a" "b"])
        tup (demo-box-round-trip [1 "t"])]
    (str i "/" s "/" b "/" c "/" f "/" l "/" (name col) "/" opt "/" (count lst) "/" (first tup))))
