package dev.wstein.flixlab.allconstructs

/** Section 6: Algebraic Effects & Handlers, approximated with a hand-rolled interpreter pattern:
  * each effect becomes a plain trait, and a "handler" is just an implementation of it passed in
  * explicitly. Deliberately not built on an effect library (cats-effect, ZIO) -- see docs/ports.md
  * for why.
  */
trait StateHandler:
  def get(): Int
  def put(v: Int): Unit

trait LogHandler:
  def log(msg: String): Unit

/** Reads the current state, logs it, stores the incremented value, and returns it. Requires both
  * handlers, mirroring Flix's `\ {StateEff, LogEff}` effect signature.
  */
def incrementState(state: StateHandler, log: LogHandler): Int =
  val curr = state.get()
  log.log(s"Current state is $curr")
  state.put(curr + 1)
  curr + 1

/** Runs `incrementState` with a state handler seeded at `10` and a log handler that discards its
  * messages -- the same nesting Flix's `run ... with handler` chain expresses.
  */
def runEffectDemo(): Int =
  val state = new StateHandler:
    private var value = 10
    def get(): Int = value
    def put(v: Int): Unit = value = v
  val log = new LogHandler:
    def log(msg: String): Unit = ()
  incrementState(state, log)
