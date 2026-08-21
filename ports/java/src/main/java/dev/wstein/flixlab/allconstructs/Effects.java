package dev.wstein.flixlab.allconstructs;

/**
 * Section 6: Algebraic Effects &amp; Handlers, approximated with a hand-rolled interpreter pattern:
 * each effect becomes a plain functional interface, and a "handler" is just an implementation of it
 * passed in explicitly. Deliberately not built on an effect library -- see docs/ports.md for why.
 */
public final class Effects {
  private Effects() {}

  public interface StateHandler {
    int get();

    void put(int v);
  }

  public interface LogHandler {
    void log(String msg);
  }

  /**
   * Reads the current state, logs it, stores the incremented value, and returns it. Requires both
   * handlers, mirroring Flix's {@code \ {StateEff, LogEff}} effect signature.
   */
  public static int incrementState(StateHandler state, LogHandler log) {
    int curr = state.get();
    log.log("Current state is " + curr);
    state.put(curr + 1);
    return curr + 1;
  }

  /**
   * Runs {@link #incrementState} with a state handler seeded at {@code 10} and a log handler that
   * discards its messages -- the same nesting Flix's {@code run ... with handler} chain expresses.
   */
  public static int runEffectDemo() {
    StateHandler state =
        new StateHandler() {
          private int value = 10;

          @Override
          public int get() {
            return value;
          }

          @Override
          public void put(int v) {
            value = v;
          }
        };
    LogHandler log = msg -> {};
    return incrementState(state, log);
  }
}
