package ch.hevs.isc.slidewave

class LapController(val lapNumber: Int = 5) {
  // All times are in milliseconds
  var currentLap: Int = 0
  var lastTime: Double = -1
  var bestTime: Double = -1
  var currentLapTimeStart: Double = -1
  var totalTime: Double = 0
  var currentLapCounted: Boolean = false
  /**
   * Last checkpoint that the car crossed, -1 = didn't go through start yet
   */
  var passedCheckpoints = -1
  def startLap(): Unit = {
    currentLap += 1
    restartTimer()
  }
  def endLap(): Unit = {
    if (!currentLapCounted) {
      println("You failed to take the right checkpoint. Lap has not been counted.")
      startLap()
      return
    }
    lastTime = System.currentTimeMillis() - currentLapTimeStart
    println("Lap time : " + Utils.msToSecondsStr(lastTime) + " seconds")
    totalTime += lastTime
    if (bestTime == -1 || lastTime < bestTime) {
      bestTime = lastTime
      println(s"New best ! : ${Utils.msToSecondsStr(bestTime)} seconds")
    }
    if (currentLap == lapNumber) {
      currentLapTimeStart = -1
      Slidewave.displayEndGame = true
      return
    }
    startLap()
  }
  def restartTimer(): Unit = {
    currentLapCounted = true
    println(s"Starting lap $currentLap / $lapNumber")
    currentLapTimeStart = System.currentTimeMillis()
  }
  def carPassedCheckpoint(i: Int): Unit = {
    if (i == 0) {
      if (passedCheckpoints == -1) startLap()
      else if (!currentLapCounted) restartTimer()
      else if (passedCheckpoints == TileManager.checkpoints.length) endLap()
      passedCheckpoints = 1
      return
    }
    if (i == passedCheckpoints) {
      // bon chemin !
      passedCheckpoints += 1
    } else if (i > passedCheckpoints) {
      // AHHHHH checkpoint loupé / sens inverse
      currentLapCounted = false
      println("lap not counted")
    }
  }
}
