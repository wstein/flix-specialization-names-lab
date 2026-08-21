package dev.wstein.flixlab.allconstructs;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Section 13: Concurrency, Channels, Select &amp; Parallelism (par yield), approximated with {@link
 * ArrayBlockingQueue} for channels and {@link CompletableFuture} for select/par-yield, run on a
 * virtual-thread executor so a permanently blocked "loser" thread ({@link #selectDemo}'s unused
 * channel) cannot prevent the JVM from exiting.
 *
 * <p>JDK 21 also ships {@code StructuredTaskScope} (JEP 453) as a closer structural match for
 * par/yield, but only as a preview API requiring {@code --enable-preview} at both build and run
 * time; {@code CompletableFuture} is used instead to keep this port buildable and runnable without
 * that flag.
 */
public final class Concurrency {
  private Concurrency() {}

  private static final ExecutorService VIRTUAL_EXECUTOR =
      Executors.newVirtualThreadPerTaskExecutor();

  public record ParYieldResult(int x, String y) {}

  public static int concurrencyDemo() {
    var channel = new ArrayBlockingQueue<Integer>(1);
    VIRTUAL_EXECUTOR.submit(
        () -> {
          channel.put(42);
          return null;
        });
    return take(channel);
  }

  public static String selectDemo() {
    var channel1 = new ArrayBlockingQueue<String>(1);
    var channel2 = new ArrayBlockingQueue<String>(1);
    VIRTUAL_EXECUTOR.submit(
        () -> {
          channel1.put("Hello Select");
          return null;
        });
    CompletableFuture<String> f1 =
        CompletableFuture.supplyAsync(() -> take(channel1), VIRTUAL_EXECUTOR);
    CompletableFuture<String> f2 =
        CompletableFuture.supplyAsync(() -> take(channel2), VIRTUAL_EXECUTOR);
    return (String) CompletableFuture.anyOf(f1, f2).join();
  }

  public static ParYieldResult parYieldDemo() {
    CompletableFuture<Integer> fx = CompletableFuture.supplyAsync(() -> 1 + 2, VIRTUAL_EXECUTOR);
    CompletableFuture<String> fy =
        CompletableFuture.supplyAsync(() -> "hello world", VIRTUAL_EXECUTOR);
    return new ParYieldResult(fx.join(), fy.join());
  }

  private static <T> T take(ArrayBlockingQueue<T> queue) {
    try {
      return queue.take();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(e);
    }
  }
}
