package dev.wstein.flixlab.allconstructs

import java.util.concurrent.{ArrayBlockingQueue, Executors, ThreadFactory}
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

/** Section 13: Concurrency, Channels, Select & Parallelism (par yield), approximated with
  * `java.util.concurrent.BlockingQueue` for channels, `Future.firstCompletedOf` for `select`, and a
  * `Future` zip for `par`/ `yield`. Deliberately plain `java.util.concurrent` + `scala.concurrent`
  * rather than a reactive/effect library -- see docs/ports.md.
  */
private val daemonThreads: ThreadFactory = (r: Runnable) =>
  val t = new Thread(r)
  t.setDaemon(true)
  t

private given ExecutionContext =
  ExecutionContext.fromExecutor(Executors.newCachedThreadPool(daemonThreads))

def concurrencyDemo(): Int =
  val channel = new ArrayBlockingQueue[Int](1)
  val sender = Future(channel.put(42))
  val received = channel.take()
  Await.result(sender, Duration.Inf)
  received

def selectDemo(): String =
  val channel1 = new ArrayBlockingQueue[String](1)
  val channel2 = new ArrayBlockingQueue[String](1)
  val sender = Future(channel1.put("Hello Select"))
  val winner = Future.firstCompletedOf(
    Seq(Future(channel1.take()), Future(channel2.take()))
  )
  val result = Await.result(winner, Duration.Inf)
  Await.result(sender, Duration.Inf)
  result

def parYieldDemo(): (Int, String) =
  val fx = Future(1 + 2)
  val fy = Future("hello world")
  Await.result(fx.zip(fy), Duration.Inf)
