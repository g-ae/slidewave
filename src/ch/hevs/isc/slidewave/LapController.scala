package ch.hevs.isc.slidewave

class LapController(val lapNumber: Int = 5) {
  // All times are in milliseconds
  var currentLap: Int = 0
  var lastTime: Double = -1
  var bestTime: Double = -1
  var currentLapTimeStart: Double = -1
  var currentLapCounted: Boolean = false
  /**
   * Last checkpoint that the car crossed, -1 = didn't go through start yet
   */
  var passedCheckpoints = -1
  def startLap(): Unit = {
    currentLap += 1
    currentLapCounted = true
    println(s"Starting lap $currentLap / $lapNumber")
    currentLapTimeStart = System.currentTimeMillis()
  }
  def endLap(): Unit = {
    if (!currentLapCounted) {
      println("You failed to take the right checkpoint. Lap has not been counted.")
      startLap()
      return
    }
    lastTime = System.currentTimeMillis() - currentLapTimeStart
    println("Lap time : " + Utils.msToSecondsStr(lastTime) + " seconds")
    if (lastTime == -1 || lastTime < bestTime) {
      bestTime = lastTime
      println(s"New best ! : ${Utils.msToSecondsStr(bestTime)} seconds")
    }
    if (currentLap == lapNumber) {
      println("Game ended")
      return
    }
    startLap()
  }
  def carPassedCheckpoint(i: Int): Unit = {
    if (i == 0 && passedCheckpoints == -1) {
      passedCheckpoints = 1
      startLap()
      return
    }
    if (i == 0 && passedCheckpoints == TileManager.checkpoints.length) {
      // lap completed
      passedCheckpoints = 1
      println("lap completed")
      endLap()
      return
    }
    if (i == passedCheckpoints) {
      // bon chemin !
      passedCheckpoints += 1
    } else {
      // AHHHHH checkpoint loupé / sens inverse
      currentLapCounted = false
      println("lap not counted")
    }
  }
}
