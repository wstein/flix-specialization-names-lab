(ns dev.wstein.flixlab.allconstructs.concurrency
  "Section 13: Concurrency, Channels, Select & Parallelism (par yield),
  approximated with `java.util.concurrent.ArrayBlockingQueue` for channels
  and `java.util.concurrent.CompletableFuture` for select/par-yield, run on
  a virtual-thread executor. Deliberately plain `java.util.concurrent`
  rather than `core.async` -- which would otherwise be the obvious
  idiomatic choice -- to keep this port's concurrency section an
  approximation like Scala and Java's, rather than adding a fifth
  dependency-heavy exception the way Kotlin's `kotlinx.coroutines` already
  is one."
  (:import (java.util.concurrent ArrayBlockingQueue CompletableFuture Executors)))

(def virtual-executor (Executors/newVirtualThreadPerTaskExecutor))

(defn concurrency-demo []
  (let [channel (ArrayBlockingQueue. 1)]
    (.execute virtual-executor (fn [] (.put channel 42)))
    (.take channel)))

(defn select-demo []
  (let [channel1 (ArrayBlockingQueue. 1)
        channel2 (ArrayBlockingQueue. 1)]
    (.execute virtual-executor (fn [] (.put channel1 "Hello Select")))
    (let [f1 (CompletableFuture/supplyAsync (fn [] (.take channel1)) virtual-executor)
          f2 (CompletableFuture/supplyAsync (fn [] (.take channel2)) virtual-executor)]
      (.join (CompletableFuture/anyOf (into-array CompletableFuture [f1 f2]))))))

(defn par-yield-demo []
  (let [fx (CompletableFuture/supplyAsync (fn [] (+ 1 2)) virtual-executor)
        fy (CompletableFuture/supplyAsync (fn [] "hello world") virtual-executor)]
    [(.join fx) (.join fy)]))
