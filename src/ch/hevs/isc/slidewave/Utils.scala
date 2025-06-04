package ch.hevs.isc.slidewave

object Utils {
  /**
   * returns time in seconds, string will not contain "s" for seconds
   * @param ms
   * @return
   */
  def msToSecondsStr(ms: Double): String = (ms / 1000).toString
}
