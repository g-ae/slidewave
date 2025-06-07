package ch.hevs.isc.slidewave.components

import ch.hevs.gdx2d.components.bitmaps.BitmapImage
import ch.hevs.gdx2d.components.physics.primitives.PhysicsBox
import ch.hevs.gdx2d.lib.GdxGraphics
import ch.hevs.gdx2d.lib.interfaces.DrawableObject
import ch.hevs.isc.slidewave.{LapController, TileManager}
import com.badlogic.gdx.math.{Polygon, Vector2}
import scala.collection.mutable.ArrayBuffer

/**
 * Car
 *
 * Runtime representation of the **player‑controlled vehicle**. A car is modeled as a
 * rectangular rigid body (the *chassis*) and four independent [[Wheel]] instances
 * that handle steering, traction and lateral friction. All physics are executed in
 * the Box2D world managed by gdx2d.
 *
 * Coordinate units:
 *   • Positions, dimensions and forces are expressed in **physics metres** unless
 *     explicitly suffixed with *px*.
 *
 * Thread‑safety: Mutated exclusively from the single LibGDX render thread; no
 * synchronization is required.
 *
 * @param width         Chassis width (*pixels*).
 * @param length        Chassis length (*pixels*).
 * @param position      Spawn location (*pixels*).
 * @param angle         Initial heading (radians, 0 = facing −Y in screen space).
 * @param power         Base engine force applied to powered wheels.
 * @param maxSteerAngle Maximum steering lock (*degrees*).
 * @param maxSpeed      Forward speed cap (*km/h*).
 * @param carImage      Sprite drawn for the car top‑view.
 */
class Car(width: Float,
          length: Float,
          position: Vector2,
          angle: Float,
          val power: Float,
          val maxSteerAngle: Float,
          val maxSpeed: Float,
          val carImage: BitmapImage
         ) extends DrawableObject {

  // -------------------------------------------------------------------------
  // Mutable control flags – toggled every frame by SlidewaveWindow.onGraphicRender
  // -------------------------------------------------------------------------
  var steer_left:  Boolean = false  // player presses 'A'
  var steer_right: Boolean = false  // player presses 'D'
  var accelerate:  Boolean = false  // player presses 'W'
  var brake:       Boolean = false  // player presses 'S'
  var handbrake:   Boolean = false  // player presses 'SHIFT'
  var wheelAngle: Float = 0f        // Current wheel angle (°). Positive → steer left, negative → steer right

  // -------------------------------------------------------------------------
  // Physical sub‑components
  // -------------------------------------------------------------------------
  val wheelWidth:  Int = 16 // Width of a single tyre rectangle (px). Purely visual.
  val wheelHeight: Int = 60 // Height of a single tyre rectangle (px). Half the car length by design.
  val carbox: PhysicsBox = new PhysicsBox("carCenter", position.cpy(), width, length, angle)  // Box2D body representing the *centre* of mass of the car.
  carbox.setCollisionGroup(-1) // ignore collisions with wheels
  val wheels: ArrayBuffer[Wheel] = new ArrayBuffer[Wheel]() // Container for the four [[Wheel]] instances (FL, FR, BL, BR)

  // Polygon updated every frame – used for checkpoint overlap tests.
  val carPolygon: Polygon = new Polygon(Array(
    -width / 2, -length / 2,   // bottom‑left
    width / 2, -length / 2,   // bottom‑right
    width / 2,  length / 2,   // top‑right
    -width / 2,  length / 2    // top‑left
  ))

  val lapController: LapController = new LapController()  // Dedicated controller tracking laps / checkpoints for this car.

  // -------------------------------------------------------------------------
  // Misc. tuning constants
  // -------------------------------------------------------------------------
  val slowingFactor: Float = 0.7f // Engine drag applied when throttle is released (0 → coasts, 1 → stops instantly)
  val wheelOffset: Vector2 = new Vector2(25, 35)  // Offset (px) locating wheels relative to the chassis centre

  // -------------------------------------------------------------------------
  // Wheel construction – order matters (FL, FR, BL, BR)
  // -------------------------------------------------------------------------
  // Front‑left (revolving + powered)
  wheels += new Wheel(this, wheelOffset.cpy().scl(-1, -1), wheelWidth, wheelHeight, revolving = true,  powered = true)
  // Front‑right
  wheels += new Wheel(this, wheelOffset.cpy().scl( 1, -1), wheelWidth, wheelHeight, revolving = true,  powered = true)
  // Back‑left (fixed + powered)
  wheels += new Wheel(this, wheelOffset.cpy().scl(-1,  1), wheelWidth, wheelHeight, revolving = false, powered = true)
  // Back‑right
  wheels += new Wheel(this, wheelOffset.cpy().scl( 1,  1), wheelWidth, wheelHeight, revolving = false, powered = true)

  // -------------------------------------------------------------------------
  // Convenience getters
  // -------------------------------------------------------------------------

  // Returns velocity in the car *local* frame (m/s).
  def getLocalVelocity: Vector2 = carbox.getBody.getLocalVector(carbox.getBody.getLinearVelocityFromLocalPoint(Vector2.Zero))

  // Wheels that can steer
  def getRevolvingWheels: Array[Wheel] = wheels.filter(_.revolving).toArray

  // Wheels that receive engine force
  def getPoweredWheels:  Array[Wheel] = wheels.filter(_.powered).toArray

  // Instantaneous speed (*km/h*)
  def getSpeedKMH: Float = carbox.getBodyLinearVelocity.len() * 3.6f

  /**
   * Adjusts the car’s velocity vector so that its magnitude equals the given speed.
   *
   * @param speed Target speed (*km/h*).
   */
  def setSpeed(speed: Float): Unit = {
    val velocity: Vector2 = carbox.getBodyLinearVelocity.nor.scl(speed / 3.6f)
    carbox.setBodyLinearVelocity(velocity)
  }

  // -------------------------------------------------------------------------
  // Per‑frame update
  // -------------------------------------------------------------------------

  /**
   * Advances the car physics and control logic by one fixed‑time step.
   *
   * @param deltaTime Frame delta provided by LibGDX (*seconds*).
   */
  def update(deltaTime: Float): Unit = {
    // -----------------------------------------------------------------------
    // 1) Tyre lateral friction & off‑track detection
    // -----------------------------------------------------------------------
    var wheelsOffTrack = 0
    for (w <- wheels) {
      // Non‑powered wheels: always kill lateral drift completely
      if (!w.powered) w.killSidewaysVelocity()
      else {
        // Powered wheels: grip depends on speed & handbrake state
        if (getLocalVelocity.len() < 1.5f) {
          // Low speed → strong grip for easier start
          w.killSidewaysVelocity(0.3f)
        } else if (!handbrake) {
          w.killSidewaysVelocity(TileManager.getTileUnderWheelGrip(w))
        } else {
          // Handbrake engaged → treat as off‑road
          w.killSidewaysVelocity(TileManager.getTileTypeGrip(null))
        }
      }

      // Track limits
      if (!TileManager.isWheelInTrack(w)) wheelsOffTrack += 1
    }
    if (wheelsOffTrack == wheels.length) lapController.currentLapCounted = false

    // Update polygon position for checkpoint collisions ----------------------
    carPolygon.setPosition(carbox.getBodyPosition.x, carbox.getBodyPosition.y)
    carPolygon.setRotation(carbox.getBodyAngleDeg)

    // -----------------------------------------------------------------------
    // 2) Steering – smooth wheel angle interpolation
    // -----------------------------------------------------------------------
    val change: Float = maxSteerAngle * deltaTime * 4 // ×4 → snappy steering response

    if (steer_left)
      wheelAngle = Math.min(Math.max(wheelAngle, 0f) + change, maxSteerAngle)
    else if (steer_right)
      wheelAngle = Math.max(Math.min(wheelAngle, 0f) - change, -maxSteerAngle)
    else
      wheelAngle = 0f

    // Apply steering angle to front axle
    for (w <- getRevolvingWheels) w.setAngle(wheelAngle)

    // -----------------------------------------------------------------------
    // 3) Engine & braking forces
    // -----------------------------------------------------------------------
    var baseVector = Vector2.Zero // local +Y points *backward* (LibGDX convention)

    if (accelerate && getSpeedKMH < maxSpeed) {
      baseVector = new Vector2(0, -1) // forward thrust
    } else if (brake) {
      // Braking: direction depends on current velocity sign
      if (getLocalVelocity.y < 0)      baseVector = new Vector2(0,  1.3f) // braking while going forward
      else if (getSpeedKMH < maxSpeed) baseVector = new Vector2(0,  0.3f) // gentle reverse
    } else {
      // Neither accelerating nor braking → apply engine drag
      if (getSpeedKMH < 1.5f) setSpeed(0)
      else if (getLocalVelocity.y < 0) baseVector = new Vector2(0,  slowingFactor)
      else if (getLocalVelocity.y > 0) baseVector = new Vector2(0, -slowingFactor)
    }

    val forceVector = baseVector.scl(power) // scale by engine power

    // Distribute force to each powered wheel
    for (w <- getPoweredWheels) {
      val onTrack = TileManager.isWheelInTrack(w)
      w.wheelbox.getBody.applyForce(
        w.wheelbox.getBody.getWorldVector(forceVector.cpy().scl(if (onTrack) 1f else 0.2f)), // reduced grip off‑track
        w.wheelbox.getBody.getWorldCenter,
        /* wake = */ true)
    }
  }

  // -------------------------------------------------------------------------
  // Rendering
  // -------------------------------------------------------------------------

  /** Draws the car sprite and its wheels in world space. */
  override def draw(g: GdxGraphics): Unit = {
    // Tyres ------------------------------------------------------------------
    wheels.foreach(_.draw(g))

    // Chassis sprite ---------------------------------------------------------
    val pos = carbox.getBodyPosition
    g.drawTransformedPicture(pos.x, pos.y, carbox.getBodyAngleDeg + 180, (width * 1.4f), length, carImage)
  }

  // -------------------------------------------------------------------------
  // Checkpoint forwarding
  // -------------------------------------------------------------------------

  // Delegates checkpoint crossing events to the underlying [[LapController]]
  def wentOverCheckpoint(i: Int): Unit = lapController.carPassedCheckpoint(i)
}
