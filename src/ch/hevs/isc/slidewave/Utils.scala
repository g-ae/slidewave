package ch.hevs.isc.slidewave

object Utils {
  /**
   * returns time in seconds, string will not contain "s" for seconds
   */
  def msToSecondsStr(ms: Double): String = {
    val final_ms = (ms % 1000).toInt
    val seconds = Math.floor(ms / 1000)
    val final_s = (seconds % 60).toInt
    val minutes = Math.floor(seconds / 60)
    val final_min = (minutes % 60).toInt


    (if (final_min != 0) f"$final_min%02d:" else "") + f"$final_s%02d.$final_ms%03d"
  }
}
