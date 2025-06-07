package ch.hevs.isc.slidewave.components

import ch.hevs.gdx2d.components.physics.primitives.PhysicsBox
import ch.hevs.gdx2d.components.physics.utils.PhysicsConstants
import ch.hevs.gdx2d.lib.GdxGraphics
import ch.hevs.gdx2d.lib.interfaces.DrawableObject
import ch.hevs.gdx2d.lib.physics.PhysicsWorld
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.World
import com.badlogic.gdx.physics.box2d.joints.{PrismaticJointDef, RevoluteJointDef}

/**
 * Wheel
 *
 * Physical **and** visual representation of a single tyre that belongs to a
 * parent [[Car]].  Each wheel can be:
 *
 *   • '''revolving''' – allowed to steer via a revolute joint (front axle)
 *   • '''powered'''  – receives engine force directly
 *
 * Every wheel is a slim Box2D rectangle rigidly attached to the chassis
 * through either
 *   – a [[RevoluteJointDef]] → front wheels (free to rotate for steering) or
 *   – a [[PrismaticJointDef]] → rear wheels (no steering, slide constrained)
 *
 * All coordinates are **physics metres** unless suffixed with *px*.
 *
 * @param car       Parent [[Car]].
 * @param wheelPos  Offset from the car centre (*pixels*).
 * @param width     Visual tyre width  (*pixels*).
 * @param length    Visual tyre length (*pixels*).
 * @param revolving `true` → this wheel may steer (front axle).
 * @param powered   `true` → engine force is applied to this wheel.
 */
class Wheel(
             val car: Car,
             wheelPos: Vector2,
             width: Float,
             length: Float,
             val revolving: Boolean,
             val powered: Boolean
           ) extends DrawableObject {

  // ---------------------------------------------------------------------------
  // Box2D initialisation
  // ---------------------------------------------------------------------------
  private val world: World = PhysicsWorld.getInstance() // Shared Box2D world (singleton handled by gdx2d)
  private val localPosM: Vector2 = PhysicsConstants.coordPixelsToMeters(wheelPos) // Wheel centre in **metres** relative to the chassis origin.
  private var wheelAngle: Float = 0f  // Current steering angle (°). Positive → steer left

  // Build the Box2D body
  private val carPosPx: Vector2 =
    car.carbox.getBody.getWorldPoint(localPosM) // world→px

  // Half-length rectangle to prevent self-collision when turning
  val wheelbox: PhysicsBox = new PhysicsBox(
    "wheel",
    PhysicsConstants.coordMetersToPixels(carPosPx),
    width,
    length / 2,
    car.carbox.getBodyAngle
  )

  // Joint selection: revolute (front) vs prismatic (rear)
  if (revolving) {
    val jd = new RevoluteJointDef
    jd.initialize(
      car.carbox.getBody,
      wheelbox.getBody,
      wheelbox.getBody.getWorldCenter
    )
    jd.enableMotor = false            // no steering motor, purely passive
    world.createJoint(jd)
  } else {
    val jd = new PrismaticJointDef
    jd.initialize(
      car.carbox.getBody,
      wheelbox.getBody,
      wheelbox.getBody.getWorldCenter,
      new Vector2(1, 0)               // slide axis irrelevant (locked below)
    )
    jd.enableLimit       = true
    jd.lowerTranslation  = 0
    jd.upperTranslation  = 0          // lock translation → behaves like bolt
    world.createJoint(jd)
  }

  // ---------------------------------------------------------------------------
  // Steering control
  // ---------------------------------------------------------------------------

  // Updates the wheel heading relative to the chassis.
  def setAngle(angleDeg: Float): Unit = {

    // --- Speed-based under-steer model ---------------------------------------
    val v          = wheelbox.getBody.getLinearVelocity
    val speedMS    = math.hypot(v.x, v.y).toFloat          // m/s
    val fullLock   = Math.toRadians(30).toFloat            // 30° at 0 km/h
    val fadeStart  = 10f                                   // m/s ≈ 36 km/h
    val fadeFactor = 0.8f                                  // 0=no fade, 1=full

    val fade      = math.min(speedMS / fadeStart, 1f) * fadeFactor
    val allowed   = fullLock * (1f - fade)                 // shrink with speed
    val demanded  = Math.toRadians(angleDeg).toFloat
    val clamped   = demanded.max(-allowed).min(allowed)

    wheelAngle = Math.toDegrees(clamped).toFloat
    wheelbox.getBody.setTransform(
      wheelbox.getBody.getPosition.x,
      wheelbox.getBody.getPosition.y,
      car.carbox.getBodyAngle + clamped
    )
  }

  // Current steering angle in degrees (read-only)
  def getAngle: Float = wheelAngle

  // ---------------------------------------------------------------------------
  // Helper vectors
  // ---------------------------------------------------------------------------

  // Velocity in the car *local* frame (m/s)
  private def localVelocity: Vector2 =
    car.carbox.getBody.getLocalVector(
      car.carbox.getBody.getLinearVelocityFromLocalPoint(
        wheelbox.getBody.getPosition
      )
    )

  // Unit vector pointing in the tyre’s facing direction
  private def directionVector: Vector2 = {
    val dir = new Vector2(0, if (localVelocity.y > 0) 1 else -1)
    dir.rotate(Math.toDegrees(wheelbox.getBody.getAngle).toFloat)
  }

  // Projection of velocity onto the sideways axis (needs killing for grip)
  private def killVelocity: Vector2 = {
    val side = directionVector
    val dot  = wheelbox.getBody.getLinearVelocity.dot(side)
    new Vector2(side.x * dot, side.y * dot)
  }

  /** Removes a share of lateral speed to simulate tyre grip.
   *
   * @param strength 0 → ice (no grip), 1 → full grip.
   */
  def killSidewaysVelocity(strength: Float = 0f): Unit = {
    val lateral    = wheelbox.getBody.getLinearVelocity.cpy().sub(killVelocity)
    val damped     = lateral.scl(strength)                // interpolate
    val corrected  = killVelocity.add(damped)
    wheelbox.getBody.setLinearVelocity(corrected)
  }

  // ---------------------------------------------------------------------------
  // Rendering
  // ---------------------------------------------------------------------------

  // Draws a simple filled rectangle representing the tyre
  override def draw(g: GdxGraphics): Unit = {
    val pos = wheelbox.getBodyPosition
    g.drawFilledRectangle(
      pos.x,
      pos.y,
      length / 2,
      width,
      Math.toDegrees(car.carbox.getBodyAngle).toFloat + 90f + wheelAngle,
      Color.BLACK
    )
  }
}
