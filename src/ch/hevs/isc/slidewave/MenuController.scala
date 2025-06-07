package ch.hevs.isc.slidewave

import ch.hevs.gdx2d.components.bitmaps.BitmapImage
import ch.hevs.gdx2d.lib.GdxGraphics
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator

/**
 * MenuController
 *
 * Stateless utility *singleton* responsible for drawing the **start** and **end‑of‑race** overlays.
 * All assets (bitmap images / fonts) are loaded once at class initialisation and then reused on
 * every frame to avoid costly allocations during the render loop.
 *
 * Coordinate system:
 *   • All positions are expressed in *world* coordinates (metres) because [[GdxGraphics]] already
 *     applies the camera transform. Differences in logical pixel density are therefore irrelevant
 *     and no additional scaling is required.
 *
 * Thread‑safety:
 *   • LibGDX guarantees that all rendering happens on the *render thread*, so **no synchronisation**
 *     is necessary.
 */
object MenuController {
  // ---------------------------------------------------------------------------
  // Assets – loaded once and reused
  // ---------------------------------------------------------------------------
  val gameLogo: BitmapImage = new BitmapImage("data/images/logo_nobg.png") // Game logo displayed on the start and end screens
  val wasdImage: BitmapImage = new BitmapImage("data/images/wasd.png") // WASD key layout picture shown on the controls overlay.
  val shiftImage: BitmapImage = new BitmapImage("data/images/shift.png") // SHIFT key picture used to illustrate the handbrake control.

  // ---------------------------------------------------------------------------
  // Font generation
  // ---------------------------------------------------------------------------
  private val consolas = Gdx.files.internal("data/fonts/Consolas.ttf") // Underlying TTF file used for all textual UI captions.
  val generator = new FreeTypeFontGenerator(consolas) // Shared FT generator kept alive for dynamic sizing (don’t dispose until shutdown)

  // Default UI font (≈28 px high) pre‑generated once for efficiency.
  val font: BitmapFont = {
    val param = new FreeTypeFontGenerator.FreeTypeFontParameter
    param.color   = Color.BLACK
    param.size    = generator.scaleForPixelHeight(28)
    param.hinting = FreeTypeFontGenerator.Hinting.Full

    generator.generateFont(param)
  }

  // ---------------------------------------------------------------------------
  // Public drawing helpers
  // ---------------------------------------------------------------------------

  /**
   * Renders the **start‑menu** overlay that introduces the controls (WASD, handbrake).
   * The window is composed of two nested rectangles (black shadow + grey panel), a centred game
   * logo and several labelled key icons.
   *
   * @param g High‑level graphics context provided by gdx2d (already in world space).
   */
  def drawStartMenu(g: GdxGraphics): Unit = {
    // --- Background panel ----------------------------------------------------
    g.setColor(Color.BLACK)
    g.drawFilledRectangle(
      g.getCamera.position.x, g.getCamera.position.y,
      Slidewave.screenWidth * 2 / 3 + 54, Slidewave.screenHeight * 2 / 3 + 54, 0)

    g.setColor(Color.GRAY)
    g.drawFilledRectangle(
      g.getCamera.position.x, g.getCamera.position.y,
      Slidewave.screenWidth * 2 / 3 + 40, Slidewave.screenHeight * 2 / 3 + 40, 0)

    // --- Static logo ---------------------------------------------------------
    g.drawTransformedPicture(
      g.getCamera.position.x, g.getCamera.position.y + 250,
      0, 200, 200, gameLogo)

    // --- Controls legend -----------------------------------------------------
    // WASD picture (scale 1:1) ----------------------------------------------
    g.drawTransformedPicture(
      g.getCamera.position.x - 200, g.getCamera.position.y - 130,
      0, 1.0f, wasdImage)

    // Captions ---------------------------------------------------------------
    g.drawString(g.getCamera.position.x - 100, g.getCamera.position.y -  30, "Accelerate",    font)
    g.drawString(g.getCamera.position.x - 310, g.getCamera.position.y - 300, "Brake/Reverse", font)
    g.drawString(g.getCamera.position.x - 600, g.getCamera.position.y - 195, "Steer left",    font)
    g.drawString(g.getCamera.position.x +  50, g.getCamera.position.y - 195, "Steer right",   font)

    // SHIFT picture (scaled down) -------------------------------------------
    g.drawTransformedPicture(
      g.getCamera.position.x + 450, g.getCamera.position.y - 110,
      0, 0.3f, shiftImage)
    g.drawString(g.getCamera.position.x + 380, g.getCamera.position.y - 220, "Handbrake", font)
  }

  /**
   * Draws the **end‑of‑race** overlay (sometimes referred to as *Game‑Over screen*).
   * Displays the player’s best lap, total time and a restart instruction.
   *
   * @param g Graphics context (world space).
   */
  def drawEndMenu(g: GdxGraphics): Unit = {
    // Pre‑compute camera centre to avoid repetitive calls ---------------------
    val camX = g.getCamera.position.x
    val camY = g.getCamera.position.y

    // --- Background rectangles ---------------------------------------------
    g.setColor(Color.BLACK)
    g.drawFilledRectangle(
      camX, camY,
      Slidewave.screenWidth * 2 / 3 + 54,
      Slidewave.screenHeight * 2 / 3 + 54, 0)

    g.setColor(Color.GRAY)
    g.drawFilledRectangle(
      camX, camY,
      Slidewave.screenWidth * 2 / 3 + 40,
      Slidewave.screenHeight * 2 / 3 + 40, 0)

    // --- Title (logo or fallback text) --------------------------------------
    g.drawTransformedPicture(camX, camY + 250, 0, 200, 200, gameLogo)
    // Alternative text‑only fallback:
    // g.drawString(camX - 100, camY + 250, "GAME OVER", font)

    // --- Time statistics ----------------------------------------------------
    g.setColor(Color.WHITE)
    g.drawString(camX - 100, camY +  50,
      s"Best lap:   ${Utils.msToSecondsStr(Slidewave.playerCar.lapController.bestTime)}", font)
    g.drawString(camX - 100, camY -  20,
      s"Total time: ${Utils.msToSecondsStr(Slidewave.playerCar.lapController.totalTime)}", font)

    // --- Restart hint -------------------------------------------------------
    g.drawString(camX - 150, camY - 120, "Press [R] to restart", font)
  }
}
