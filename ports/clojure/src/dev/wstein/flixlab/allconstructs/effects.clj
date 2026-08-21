(ns dev.wstein.flixlab.allconstructs.effects
  "Section 6: Algebraic Effects & Handlers, approximated with a hand-rolled
  interpreter pattern: each effect becomes a protocol, and a \"handler\" is
  a `reify` of it passed in explicitly. Deliberately not built on
  `core.async` or an effect library -- see docs/ports.md for why.")

(defprotocol StateHandler
  (get-state [this])
  (put-state [this v]))

(defprotocol LogHandler
  (log-msg [this msg]))

(defn increment-state [state log]
  (let [curr (get-state state)]
    (log-msg log (str "Current state is " curr))
    (put-state state (inc curr))
    (inc curr)))

(defn run-effect-demo []
  (let [box (atom 10)
        state (reify StateHandler
                (get-state [_] @box)
                (put-state [_ v] (reset! box v)))
        log (reify LogHandler
              (log-msg [_ _] nil))]
    (increment-state state log)))
