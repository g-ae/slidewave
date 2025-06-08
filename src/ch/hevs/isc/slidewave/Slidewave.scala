package ch.hevs.isc.slidewave

import ch.hevs.gdx2d.components.bitmaps.BitmapImage
import ch.hevs.gdx2d.components.physics.utils.PhysicsScreenBoundaries
import ch.hevs.gdx2d.desktop.PortableApplication
import ch.hevs.gdx2d.lib.GdxGraphics
import ch.hevs.gdx2d.lib.physics.PhysicsWorld
import ch.hevs.isc.slidewave.components.Car
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.{Gdx, Input}
import com.badlogic.gdx.math.Vector2

/**
 * Slidewave
 *
 * Global singleton holding **application‑wide constants** (screen size, flags), a reference to the
 * current player [[Car]] instance and the **program entry‑point**.
 *
 * All coordinates manipulated in this file are expressed in **physics metres** unless otherwise
 * noted. Screen dimensions are in logical pixels.
 */
object Slidewave {
  val screenWidth  = 1920               // Width of the main window in logical pixels
  val screenHeight = 1080               // Height of the main window in logical pixels
  var displayEndGame: Boolean = false   // When set to `true` the end‑game overlay is shown and user input is frozen.
  var stopPhysics   = false             // Master switch allowing the physics world to be paused (useful for menus/debug).
  var playerCar: Car = null             // Handle to the player's car initialised in SlidewaveWindow.onInit

  /**
   * Application entry‑point
   * Creates the libGDX window and starts the game.
   *
   * @param args Command‑line arguments (ignored).
   */
  def main(args: Array[String]): Unit = {
    new SlidewaveWindow // instantiates the desktop window → the game loop starts automatically
  }

  /**
   * Builds a brand‑new player [[Car]] positioned at the spawn point.
   * The numerical parameters have been **tuned empirically** for responsive handling.
   *
   * @return Freshly created car ready to be added to the physics world.
   */
  def getNewCar: Car = new Car(
    /* width  */ 30,
    /* height */ 70,
    /* spawn  */ TileManager.getStartingPoint,
    /* angle  */ (Math.PI / 2).toFloat, // facing upwards
    /* mass   */ 2,
    /* fwdMax */ 30,
    /* revMax */ 20,
    /* sprite */ new BitmapImage("data/images/bmw-car.png")
  )
}

/**
 * SlidewaveWindow
 *
 * Concrete libGDX [[PortableApplication]] responsible for **rendering**, **input handling** and
 * **physics stepping**. It effectively *is* the game loop.
 *
 * @constructor The parent [[PortableApplication]] constructor receives the window dimensions.
 */
class SlidewaveWindow extends PortableApplication(Slidewave.screenWidth, Slidewave.screenHeight) {

  val world = PhysicsWorld.getInstance()  // Shared Box2D world (singleton managed by gdx2d)
  var showControls: Boolean = true  // Whether the on‑screen help (controls diagram) should be displayed.

  /**
   * Runtime‑generated bitmap font used to display lap times and misc. UI captions.
   * Lazy so that the relatively heavy FT generation only occurs when first accessed.
   */
  lazy val lapTimeFont: BitmapFont = {
    val paramLapTime = new FreeTypeFontGenerator.FreeTypeFontParameter
    paramLapTime.color   = Color.BLACK
    paramLapTime.size    = MenuController.generator.scaleForPixelHeight(38)
    paramLapTime.hinting = FreeTypeFontGenerator.Hinting.Full

    MenuController.generator.generateFont(paramLapTime)
  }

  /**
   * Initialisation
   *
   * Called once right after the OpenGL context is ready.
   * Sets up the physics world, player car and screen boundaries.
   */
  override def onInit(): Unit = {
    setTitle("Slidewave")
    Slidewave.playerCar = Slidewave.getNewCar // Spawn the player car and place it in the physics world
    world.setGravity(new Vector2(0, 0)) // This track is top‑down → no gravity

    // Create static Box2D walls matching the map bounds so the car cannot leave
    new PhysicsScreenBoundaries(
      TileManager.tiledLayerBG.getWidth  * TileManager.tiledLayerBG.getTileWidth,
      TileManager.tiledLayerBG.getHeight * TileManager.tiledLayerBG.getTileHeight
    )
  }

  /**
   * Main loop – called once per rendered frame
   * Renders the current frame and advances game/physics state.
   *
   * @param g High‑level graphics context provided by gdx2d.
   */
  override def onGraphicRender(g: GdxGraphics): Unit = {
    // -----------------------------------------------------------------------
    // 1) Camera setup & world rendering
    // -----------------------------------------------------------------------
    g.clear()
    g.zoom(TileManager.zoom)

    // Center camera on the car while keeping it inside the track rectangle
    g.moveCamera(
      Math.round(Slidewave.playerCar.carbox.getBodyPosition.x),
      Math.round(Slidewave.playerCar.carbox.getBodyPosition.y),
      TileManager.tiledLayerBG.getWidth  * TileManager.tiledLayerBG.getTileWidth,
      TileManager.tiledLayerBG.getHeight * TileManager.tiledLayerBG.getTileHeight
    )

    // Draw the tiled map
    TileManager.tiledMapRenderer.setView(g.getCamera)
    TileManager.tiledMapRenderer.render()

    // -----------------------------------------------------------------------
    // 2) Mini‑map (off‑screen viewport)
    // -----------------------------------------------------------------------
    TileManager.tiledLayerBG.setOpacity(0.5f)

    // Switch the GL viewport so we can render the miniature in the top‑right corner
    com.badlogic.gdx.Gdx.gl.glViewport(
      Slidewave.screenWidth  - TileManager.minMapWidth,
      Slidewave.screenHeight - TileManager.miniMapHeight,
      TileManager.minMapWidth,
      TileManager.miniMapHeight
    )

    // Configure the mini‑map camera to encompass the *whole* track
    TileManager.miniCam.viewportWidth  = TileManager.tiledLayerBG.getWidth  * TileManager.tiledLayerBG.getTileWidth
    TileManager.miniCam.viewportHeight = TileManager.tiledLayerBG.getHeight * TileManager.tiledLayerBG.getTileHeight
    TileManager.miniCam.position.set(
      TileManager.miniCam.viewportWidth  / 2f,
      TileManager.miniCam.viewportHeight / 2f,
      0f
    )
    TileManager.miniCam.update()
    TileManager.miniMapRenderer.setView(TileManager.miniCam)

    // Batch draw: background followed by the small BMW icon representing the player
    val miniBatch = TileManager.miniMapRenderer.getBatch
    miniBatch.begin()

    // Draw semi‑transparent track
    miniBatch.setColor(Color.DARK_GRAY)
    TileManager.miniMapRenderer.renderTileLayer(TileManager.tiledLayerBG)

    // Draw the player icon
    miniBatch.setColor(Color.WHITE)
    val carPos      = Slidewave.playerCar.carbox.getBodyPosition
    val desiredPx   = 30f                     // icon size in screen pixels
    val worldPerPx  = TileManager.miniCam.viewportWidth / TileManager.minMapWidth // scale world→pixel
    val iconW       = desiredPx * worldPerPx
    val iconH       = desiredPx * worldPerPx
    miniBatch.draw(TileManager.carMiniRegion,
      carPos.x - iconW / 2f,
      carPos.y - iconH / 2f,
      iconW,
      iconH)

    miniBatch.end()

    // Restore full‑screen viewport
    com.badlogic.gdx.Gdx.gl.glViewport(0, 0, Slidewave.screenWidth, Slidewave.screenHeight)
    TileManager.tiledLayerBG.setOpacity(1f)

    // -----------------------------------------------------------------------
    // 3) Draw the car & UI overlays
    // -----------------------------------------------------------------------
    Slidewave.playerCar.draw(g)

    // Menus
    if (showControls) MenuController.drawStartMenu(g)
    if (Slidewave.displayEndGame) {
      MenuController.drawEndMenu(g)
      // Restart game when the player presses 'R'
      if (Gdx.input.isKeyPressed(Input.Keys.R)) {
        Slidewave.displayEndGame = false
        Slidewave.playerCar = Slidewave.getNewCar
      }
    }

    // Early‑exit if physics are paused
    if (Slidewave.stopPhysics) return

    // -----------------------------------------------------------------------
    // 4) Checkpoint detection
    // -----------------------------------------------------------------------
    // Starting line
    if (TileManager.isCarOverCheckpoint(TileManager.checkpoints(0))){
      Slidewave.playerCar.wentOverCheckpoint(0)
    }

    // Sequential checkpoints (only after race start)
    if (Slidewave.playerCar.lapController.passedCheckpoints != -1 && Slidewave.playerCar.lapController.passedCheckpoints != TileManager.checkpoints.length) {
      if (TileManager.isCarOverCheckpoint(TileManager.checkpoints(Slidewave.playerCar.lapController.passedCheckpoints))) {
        Slidewave.playerCar.wentOverCheckpoint(Slidewave.playerCar.lapController.passedCheckpoints)
      }
    }

    // -----------------------------------------------------------------------
    // 5) Physics & car controls
    // -----------------------------------------------------------------------
    PhysicsWorld.updatePhysics(Gdx.graphics.getDeltaTime)

    // Read keyboard input
    val pressingW = Gdx.input.isKeyPressed(Input.Keys.W)
    Slidewave.playerCar.accelerate  = pressingW
    if (pressingW) showControls = false // hide help overlay once player moves
    Slidewave.playerCar.brake      = Gdx.input.isKeyPressed(Input.Keys.S)
    Slidewave.playerCar.handbrake  = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)

    // Steering left/right
    if (Gdx.input.isKeyPressed(Input.Keys.A)) {
      Slidewave.playerCar.steer_left  = true
      Slidewave.playerCar.steer_right = false
    } else if (Gdx.input.isKeyPressed(Input.Keys.D)) {
      Slidewave.playerCar.steer_right = true
      Slidewave.playerCar.steer_left  = false
    } else {
      Slidewave.playerCar.steer_left  = false
      Slidewave.playerCar.steer_right = false
    }

    Slidewave.playerCar.update(Gdx.graphics.getDeltaTime)

    // -----------------------------------------------------------------------
    // 6) Debug overlays (FPS, lap time, etc.)
    // -----------------------------------------------------------------------
    // FPS
    g.setColor(Color.BLACK)
    g.drawString(
      (g.getCamera.position.x - Slidewave.screenWidth  / 2) + 5,
      (g.getCamera.position.y + Slidewave.screenHeight / 2) - 5,
      s"FPS: ${Gdx.graphics.getFramesPerSecond}")

    if (Slidewave.playerCar.lapController.currentLapTimeStart != -1) {
      // Live lap time ---------------------------------------------------------
      g.drawString(
        g.getCamera.position.x,
        (g.getCamera.position.y + Slidewave.screenHeight / 2) - 5,
        Utils.msToSecondsStr(System.currentTimeMillis() - Slidewave.playerCar.lapController.currentLapTimeStart),
        lapTimeFont)

      // Lap counter & out‑of‑bounds warning ----------------------------------
      g.drawString(
        (g.getCamera.position.x - Slidewave.screenWidth / 2) + 5,
        (g.getCamera.position.y - Slidewave.screenHeight / 2) + lapTimeFont.getLineHeight,
        s"Lap ${Slidewave.playerCar.lapController.currentLap} / ${Slidewave.playerCar.lapController.lapNumber}" +
          (if (!Slidewave.playerCar.lapController.currentLapCounted) " (you went out of bounds, current lap not counted)" else ""),
        lapTimeFont)
    } else if (Slidewave.playerCar.lapController.bestTime != -1) {
      // Personal best ---------------------------------------------------------
      g.drawString(
        (g.getCamera.position.x - Slidewave.screenWidth / 2) + 5,
        (g.getCamera.position.y - Slidewave.screenHeight / 2) + lapTimeFont.getLineHeight,
        s"Best time : ${Utils.msToSecondsStr(Slidewave.playerCar.lapController.bestTime)} seconds",
        lapTimeFont)
    }
  }
}
