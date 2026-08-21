package dev.wstein.flixlab.allconstructs

/**
 * Section 6: Algebraic Effects & Handlers, approximated with a hand-rolled interpreter pattern:
 * each effect becomes a plain interface, and a "handler" is just an implementation of it passed
 * in explicitly. Deliberately not built on `kotlinx.coroutines`' structured concurrency or an
 * effect library -- see docs/ports.md for why.
 */
interface StateHandler {
    fun get(): Int

    fun put(v: Int)
}

interface LogHandler {
    fun log(msg: String)
}

/** Reads the current state, logs it, stores the incremented value, and returns it. Requires both
 * handlers, mirroring Flix's `\ {StateEff, LogEff}` effect signature.
 */
fun incrementState(
    state: StateHandler,
    log: LogHandler,
): Int {
    val curr = state.get()
    log.log("Current state is $curr")
    state.put(curr + 1)
    return curr + 1
}

/** Runs [incrementState] with a state handler seeded at `10` and a log handler that discards its
 * messages -- the same nesting Flix's `run ... with handler` chain expresses.
 */
fun runEffectDemo(): Int {
    val state =
        object : StateHandler {
            private var value = 10

            override fun get(): Int = value

            override fun put(v: Int) {
                value = v
            }
        }
    val log =
        object : LogHandler {
            override fun log(msg: String) {}
        }
    return incrementState(state, log)
}
