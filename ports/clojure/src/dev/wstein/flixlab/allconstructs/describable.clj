(ns dev.wstein.flixlab.allconstructs.describable
  "Section 4: Traits, Instances & Trait Constraints.

  A Clojure protocol *is* a native typeclass mechanism -- `describe`
  dispatches on its argument's runtime type, same as Flix's trait dispatch,
  with no dictionary-passing approximation needed. `describe-twice` doesn't
  need to be part of the protocol at all: it's a plain function built on
  top of the polymorphic `describe`, simpler than Flix's default-method
  ceremony.

  Clojure has no `Option` type to extend a protocol on; presence/absence is
  just nil-vs-not-nil. So Flix's conditional `Describable[Option[t]]`
  instance becomes a plain function ([[describe-optional]]) rather than a
  dispatched instance -- there is no wrapper value to dispatch on.")

(defprotocol Describable
  (describe [this]))

(extend-protocol Describable
  Long
  (describe [this] (str "Int(" this ")"))
  String
  (describe [this] (str "String(" this ")"))
  clojure.lang.Keyword
  (describe [this]
    (case this
      :red "Color.Red"
      :green "Color.Green"
      :blue "Color.Blue")))

(defn describe-twice [x]
  (str (describe x) " & " (describe x)))

(defn describe-optional [x]
  (if (nil? x)
    "None"
    (str "Some(" (describe x) ")")))

(defn describe-optional-twice [x]
  (let [once (describe-optional x)]
    (str once " & " once)))
