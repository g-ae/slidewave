package ch.hevs.isc.slidewave

/**
 * LapController
 *
 * Gameplay component that tracks **lap progression** and **timings** for the
 * player.  All temporal values are expressed in **milliseconds** since the Unix
 * epoch (see [[java.lang.System.currentTimeMillis]]).
 *
 * Thread‑safety: the object is mutated only from the single LibGDX render
 * thread; therefore it deliberately contains no synchronisation.
 *
 * @param lapNumber Total number of laps that define a complete race.
 */
class LapController(val lapNumber: Int = 5) {

  // -------------------------------------------------------------------------
  // Time‑related state (all in *milliseconds*)
  // -------------------------------------------------------------------------
  var currentLap: Int = 0 // Index of the lap currently being driven (1‑based, 0 before the start).
  var lastTime: Double = -1 // Duration of the *previously completed* lap, `-1` until first lap done.
  var bestTime: Double = -1 // Fastest lap recorded so far, `-1` until a first valid lap is completed.
  var currentLapTimeStart: Double = -1  // Absolute timestamp at which the *current* lap started.
  var totalTime: Double = 0 // Aggregate race time (sum of every **counted** lap).

  // -------------------------------------------------------------------------
  // Lap validity & checkpoint sequencing
  // -------------------------------------------------------------------------
  /**
   * Flag set to `false` whenever the player misses a checkpoint or drives the
   * wrong way. A lap ends up discarded if this flag is `false` when the start
   * line is crossed again.
   */
  var currentLapCounted: Boolean = false

  /**
   * Index of the **next** checkpoint expected in sequence.
   *
   *  • `-1` → car has not crossed the start line yet
   *  • `n`  → waiting for checkpoint *n*
   */
  var passedCheckpoints = -1

  // -------------------------------------------------------------------------
  // Functions called by game logic
  // -------------------------------------------------------------------------
  /** Begins a new lap and (re)initialises timing counters. */
  def startLap(): Unit = {
    currentLap += 1
    restartTimer()
  }

  /**
   * Finalises the current lap, updates statistics and checks whether the race
   * is finished. If the lap was invalid (wrong checkpoint order) it is simply
   * ignored and a fresh lap is started instead.
   */
  def endLap(): Unit = {
    // Abort if the player violated the checkpoint order
    if (!currentLapCounted) {
      println("You failed to take the right checkpoint. Lap has not been counted.")
      startLap()
      return
    }

    // Compute raw lap duration
    lastTime = System.currentTimeMillis() - currentLapTimeStart
    println("Lap time : " + Utils.msToSecondsStr(lastTime) + " seconds")
    totalTime += lastTime

    // Personal best
    if (bestTime == -1 || lastTime < bestTime) {
      bestTime = lastTime
      println(s"New best ! : ${Utils.msToSecondsStr(bestTime)} seconds")
    }

    // End‑of‑race detection
    if (currentLap == lapNumber) {
      currentLapTimeStart = -1   // stop the live timer
      Slidewave.displayEndGame = true
      return
    }
    startLap()  // Otherwise chain into the next lap
  }

  /** Resets the live timer and re‑enables lap counting. */
  def restartTimer(): Unit = {
    currentLapCounted = true
    println(s"Starting lap $currentLap / $lapNumber")
    currentLapTimeStart = System.currentTimeMillis()
  }

  /**
   * Callback triggered by [[Car.wentOverCheckpoint]] every time the player
   * crosses checkpoint *i*.
   *
   * @param i Sequential checkpoint index as defined in the Tiled map (0 is the
   *          start/finish line).
   */
  def carPassedCheckpoint(i: Int): Unit = {
    // Start / finish line (checkpoint 0)
    if (i == 0) {
      if (passedCheckpoints == -1) startLap() // First time → begin race
      else if (!currentLapCounted) restartTimer() // Lap invalidated → restart timer
      else if (passedCheckpoints == TileManager.checkpoints.length) endLap() // All checkpoints taken → lap completed
      passedCheckpoints = 1 // Expect checkpoint #1 next
      return
    }

    // Intermediate checkpoints
    if (i == passedCheckpoints) {
      passedCheckpoints += 1  // Correct order – move on to the next checkpoint
    } else if (i > passedCheckpoints) {
      currentLapCounted = false // Checkpoint skipped or wrong direction → invalidate lap
      println("lap not counted")
    }
  }
}
