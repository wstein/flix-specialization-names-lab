(ns dev.wstein.flixlab.allconstructs.core-test
  "One test per demo function/section, verifying its observable return
  value -- the same bar test/TestMain.flix sets on the Flix side, not a
  bytecode census (there is nothing to census: Clojure never had static
  generics to begin with) and not output-string parity with Flix's own
  .out snapshot. See docs/ports.md."
  (:require [clojure.test :refer [deftest is]]
            [dev.wstein.flixlab.allconstructs.adts :as adts]
            [dev.wstein.flixlab.allconstructs.anonclass :as anonclass]
            [dev.wstein.flixlab.allconstructs.concurrency :as concurrency]
            [dev.wstein.flixlab.allconstructs.demobox :as demobox]
            [dev.wstein.flixlab.allconstructs.describable :as describable]
            [dev.wstein.flixlab.allconstructs.effects :as effects]
            [dev.wstein.flixlab.allconstructs.functions :as functions]
            [dev.wstein.flixlab.allconstructs.imperative :as imperative]
            [dev.wstein.flixlab.allconstructs.javainterop :as javainterop]
            [dev.wstein.flixlab.allconstructs.lazyandcasts :as lazyandcasts]
            [dev.wstein.flixlab.allconstructs.liftedclosures :as liftedclosures]
            [dev.wstein.flixlab.allconstructs.literals :as literals]
            [dev.wstein.flixlab.allconstructs.manyinstantiations :as manyinstantiations]
            [dev.wstein.flixlab.allconstructs.modules :as modules]
            [dev.wstein.flixlab.allconstructs.patternmatching :as pm]
            [dev.wstein.flixlab.allconstructs.records :as records]
            [dev.wstein.flixlab.allconstructs.repeatedstdlib :as repeatedstdlib]
            [dev.wstein.flixlab.allconstructs.structs :as structs]))

(deftest helper-module
  (is (= "HelperModule.helper" (modules/helper)))
  (is (= 42 (modules/module-value))))

(deftest struct-demo
  (is (= ["Alice" 31] (structs/struct-demo))))

(deftest run-effect-demo
  (is (= 11 (effects/run-effect-demo))))

(deftest java-interop-demo
  (is (= "Hello from Java Interop!" (javainterop/java-interop-demo))))

(deftest java-exception-demo
  (is (= "Try/Catch demo" (javainterop/java-exception-demo))))

(deftest describe-item
  (is (= "Color.Green & Color.Green" (describable/describe-twice :green)))
  (is (= "Int(123) & Int(123)" (describable/describe-twice 123)))
  (is (= "Some(String(nested)) & Some(String(nested))"
         (describable/describe-optional-twice "nested")))
  (is (= "None & None" (describable/describe-optional-twice nil))))

(deftest shape-area
  (is (= 78.53975 (adts/shape-area (adts/circle 5.0))))
  (is (= 16.0 (adts/shape-area (adts/square 4.0)))))

(deftest add
  (is (= 5 (functions/add 2 3))))

(deftest curried-multiply
  (let [mult3 (functions/curried-multiply 3)]
    (is (= 15 (mult3 5)))))

(deftest generic-identity
  (is (= 42 (functions/generic-identity 42)))
  (is (= "x" (functions/generic-identity "x"))))

(deftest higher-order-demo
  (is (= 6 (functions/higher-order-demo inc 5))))

(deftest pipeline-demo
  (is (= 30 (functions/pipeline-demo 10))))

(deftest binary-pipeline-demo
  (is (= 42 (functions/binary-pipeline-demo 20 22))))

(deftest factorial
  (is (= 120 (functions/factorial 5 1))))

(deftest record-demo
  (is (= [20 "Flix" 3.14] (records/record-demo))))

(deftest get-x
  (is (= 99 (records/get-x {:x 99 :extra "info"}))))

(deftest pattern-match-demo
  (is (= "Large Ok: 150" (pm/pattern-match-demo (adts/ok 150))))
  (is (= "Zero Ok" (pm/pattern-match-demo (adts/ok 0))))
  (is (= "One Ok" (pm/pattern-match-demo (adts/ok 1))))
  (is (= "Normal Ok: 5" (pm/pattern-match-demo (adts/ok 5))))
  (is (= "Error: boom" (pm/pattern-match-demo (adts/err "boom")))))

(deftest tuple-pattern-match
  (is (= "Zero fst" (pm/tuple-pattern-match [0 "x" true])))
  (is (= "Status true (1)" (pm/tuple-pattern-match [1 "Status" true])))
  (is (= "False thd" (pm/tuple-pattern-match [2 "x" false]))))

(deftest literals-demo
  (is (= 32 (literals/literals-demo))))

(deftest collections-demo
  (is (= '(30 1 2 3) (literals/collections-demo))))

(deftest imperative-demo
  (is (= 65 (imperative/imperative-demo))))

(deftest lazy-and-casts-demo
  (is (= 30 (lazyandcasts/lazy-and-casts-demo))))

(deftest par-yield-demo
  (is (= [3 "hello world"] (concurrency/par-yield-demo))))

(deftest concurrency-demo
  (is (= 42 (concurrency/concurrency-demo))))

(deftest select-demo
  (is (= "Hello Select" (concurrency/select-demo))))

(deftest box-demo
  (is (= "42/boxed/true/x/2.5/7/blue/1/2/1" (demobox/box-demo))))

(deftest anon-class-demo
  (is (= "from-anon-class/1" (anonclass/anon-class-demo))))

(deftest many-instantiations
  (is (= 12 (manyinstantiations/many-instantiations))))

(deftest lifted-closures-demo
  (is (= 43 (liftedclosures/lifted-closures-demo))))

(deftest repeated-stdlib-demo
  (is (= 19 (repeatedstdlib/repeated-stdlib-demo))))
