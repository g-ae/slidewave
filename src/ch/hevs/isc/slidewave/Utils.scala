package ch.hevs.isc.slidewave

/**
 * Utils
 *
 * Lightweight container for **generic helper methods** that do not belong to
 * a particular game subsystem. Everything here is *stateless* so the functions
 * are safe to call from anywhere, any time.
 */
object Utils {

  /**
   * Formats a duration given in **milliseconds** into a concise string representation.
   *
   * The format emitted is either `MM:SS.mmm` (when the duration contains at
   * least one full minute) or `SS.mmm` if the minute component is zero. The
   * trailing letter *s* is intentionally **omitted** because surrounding UI
   * elements already indicate that the value is a time.
   *
   * @param ms Duration to convert, expressed in *milliseconds*.
   * @return A human‑readable string where:
   *         - `MM`  → minutes   (`00`‑`59`) – optional
   *         - `SS`  → seconds   (`00`‑`59`)
   *         - `mmm` → millisecs (`000`‑`999`)
   */
  def msToSecondsStr(ms: Double): String = {
    val final_ms  = (ms % 1000).toInt           // Milliseconds component (0‑999)
    val seconds   = Math.floor(ms / 1000)       // Total seconds as floating‑point
    val final_s   = (seconds % 60).toInt        // Seconds   component (0‑59)
    val minutes   = Math.floor(seconds / 60)    // Total minutes
    val final_min = (minutes % 60).toInt        // Minutes   component (0‑59)

    // Build the string, prefixing minutes only when non‑zero
    (if (final_min != 0) f"$final_min%02d:" else "") +
      f"$final_s%02d.$final_ms%03d"
  }
}
