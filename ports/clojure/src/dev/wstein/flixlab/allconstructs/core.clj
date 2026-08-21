(ns dev.wstein.flixlab.allconstructs.core
  "Entry point mirroring `AllConstructs.flix`'s `main`, minus the Datalog
  section (excluded from every port -- see docs/ports.md at the repository
  root)."
  (:gen-class)
  (:require [dev.wstein.flixlab.allconstructs.adts :as adts]
            [dev.wstein.flixlab.allconstructs.describable :as describable]
            [dev.wstein.flixlab.allconstructs.effects :as effects]
            [dev.wstein.flixlab.allconstructs.functions :as functions]
            [dev.wstein.flixlab.allconstructs.javainterop :as javainterop]
            [dev.wstein.flixlab.allconstructs.modules :as modules]
            [dev.wstein.flixlab.allconstructs.patternmatching :as pm]
            [dev.wstein.flixlab.allconstructs.records :as records]
            [dev.wstein.flixlab.allconstructs.structs :as structs]))

(defn -main
  [& _args]
  (println "=== AllConstructs (Clojure port) ===")

  ;; Modules & Imports
  (let [h (modules/helper)
        mv (modules/module-value)]
    (println (str "Module Helper: " h ", Module Value: " mv)))

  ;; Structs & Regions
  (let [[name age] (structs/struct-demo)]
    (println (str "Struct Person: " name ", Age: " age)))

  ;; Effects & Handlers
  (println (str "Effect result: " (effects/run-effect-demo)))

  ;; Java Interop & Exceptions
  (println (str "Java Interop: " (javainterop/java-interop-demo)))
  (println (str "Java Try/Catch: " (javainterop/java-exception-demo)))

  ;; Traits & Polymorphism
  (println (str "Trait describe: " (describable/describe-twice :green)))
  (println (str "Trait describe: " (describable/describe-twice 123)))
  (println (str "Trait describe: " (describable/describe-optional-twice "nested")))

  ;; Enums & Restrictable Enums
  (let [shape (adts/circle 5.0)]
    (println (str "Restrictable Shape area: " (adts/shape-area shape))))

  ;; Functions, Currying & Pipelines
  (println (str "Pipeline: " (functions/pipeline-demo 10)))
  (println (str "Binary Pipeline: " (functions/binary-pipeline-demo 20 22)))
  (let [mult-fn (functions/curried-multiply 6)]
    (println (str "Curried Multiply: " (mult-fn 7))))
  (println (str "Factorial(5): " (functions/factorial 5 1)))

  ;; Records & Pattern Matching
  (let [[rx ry rz] (records/record-demo)]
    (println (str "Record: x=" rx ", y=" ry ", z=" rz)))
  (println (str "Poly record getX: " (records/get-x {:x 99 :extra "info"})))
  (println (str "Pattern match: " (pm/pattern-match-demo (adts/ok 150))))
  (println (str "Tuple pattern match: " (pm/tuple-pattern-match [1 "Status" true])))

  (println "=== Sections 1-11 done; 12-16 land in the next phase ==="))
