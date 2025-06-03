package ch.hevs.isc.slidewave.components

import ch.hevs.gdx2d.components.bitmaps.BitmapImage
import ch.hevs.gdx2d.components.physics.primitives.PhysicsBox
import ch.hevs.gdx2d.lib.GdxGraphics
import ch.hevs.gdx2d.lib.interfaces.DrawableObject
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.graphics.Color
import scala.collection.mutable.ArrayBuffer

class Car(width: Float,
          length: Float,
          position: Vector2,
          angle: Float,
          val power: Float,
          val maxSteerAngle: Float,
          val maxSpeed: Float
         ) extends DrawableObject {

  var steer_left: Boolean = false
  var steer_right: Boolean = false
  var accelerate: Boolean = false
  var brake: Boolean = false
  var wheelAngle: Float = 0
  val wheelWidth: Int = 16
  val wheelHeight: Int = 60
  val carbox: PhysicsBox = new PhysicsBox("carCenter", position, width, length, angle)
  carbox.setCollisionGroup(-1)
  val wheels = new ArrayBuffer[Wheel]()
  /**
   * How the car energy is disspated when no longer accelerating.
   * 0 doesn't brake
   * 1 stop very quickly
   */
  val slowingFactor: Float = 0.5f

  val wheelOffset = new Vector2(25,35)

  // front left
  this.wheels.append(new Wheel(this, wheelOffset.cpy().scl(-1, -1), wheelWidth, wheelHeight, true, true))
  // front right
  this.wheels.append(new Wheel(this, wheelOffset.cpy().scl(1,-1), wheelWidth, wheelHeight, true, true))
  // back left
  this.wheels.append(new Wheel(this, wheelOffset.cpy().scl(-1,1), wheelWidth, wheelHeight, false, false))
  // back right
  this.wheels.append(new Wheel(this, wheelOffset.cpy().scl(1,1), wheelWidth, wheelHeight, false, false))

  def getLocalVelocity: Vector2 = {
    carbox.getBody.getLocalVector(carbox.getBody.getLinearVelocityFromLocalPoint(new Vector2(0, 0)))
  }

  def getRevolvingWheels: Array[Wheel] = wheels.filter(_.revolving).toArray
  def getPoweredWheels: Array[Wheel] = wheels.filter(_.powered).toArray
  def getSpeedKMH: Float = carbox.getBodyLinearVelocity.len() * 3.6f
  def setSpeed(speed: Float): Unit = {
    val velocity: Vector2 = carbox.getBodyLinearVelocity.nor.scl(speed/3.6f)
    carbox.setBodyLinearVelocity(velocity)
  }

  /**
   * Updates physical parameters specific to the car
   * @param deltaTime
   */
  def update(deltaTime: Float): Unit = {
    // 1. Kill sideways velocity TODO: may change for future drifting
    for (w <- wheels) w.killSidewaysVelocity()

    // 2. Set wheel angle
    // Calculate change in wheel angle for this update -> get smooth transition
    val change: Float = this.maxSteerAngle * deltaTime * 4

    if (steer_left) wheelAngle = Math.min(Math.max(this.wheelAngle,0) + change, this.maxSteerAngle)   // max genre +30
    else if (steer_right) wheelAngle = Math.max(Math.min(this.wheelAngle, 0) - change, -this.maxSteerAngle) // max genre -30
    else this.wheelAngle = 0

    // update les roues qui tournent
    for (w <- getRevolvingWheels) w.setAngle(wheelAngle)

    // 3. Apply force to wheels
    // Vecteur qui pointe dans la direction de la force, sera appliqué à la roue et est relatif à la roue
    var baseVector = Vector2.Zero

    // si accelerateur appuyé et pas encore à la vitesse max, avancer
    if (accelerate && this.getSpeedKMH < maxSpeed) baseVector = new Vector2(0, -1)
    else if (brake) {
      // en train de freiner mais on avance tjrs, appliquer force de freinage
      if (this.getLocalVelocity.y < 0) baseVector = new Vector2(0, 1.3f)
      // reculer -> moins de force appliquée
      else if (getSpeedKMH < maxSpeed) baseVector = new Vector2(0, 0.2f)
    } else {
      // on accelere ni freine pas, appliquer frein moteur
      baseVector = new Vector2(0,0)

      // Arrêter la voiture si va trop lentement
      if (this.getSpeedKMH < 4) this.setSpeed(0)
      else {
        if (this.getLocalVelocity.y < 0) baseVector = new Vector2(0, slowingFactor)
        else if (this.getLocalVelocity.y > 0) baseVector = new Vector2(0, -slowingFactor)
      }
    }

    // appliquer puissance moteur qui nous la force actuelle de la voiture relatif à la roue
    val forceVector = baseVector.scl(power)

    // Appliquer cette force à chaque roue
    for (w <- getPoweredWheels) {
      w.body.applyForce(
        w.body.getWorldVector(
          new Vector2(
            forceVector.x,
            forceVector.y
          )
        ),
        w.body.getWorldCenter, true)
    }
  }
  override def draw(g: GdxGraphics): Unit = {
    // dessiner les roues
    for (w <- wheels) w.draw(g)

    val pos = carbox.getBodyPosition
    g.drawTransformedPicture(pos.x, pos.y, carbox.getBodyAngleDeg + 180, width, length, new BitmapImage("data/images/car_black.png"))

    val v = getLocalVelocity.scl(100)
    g.setColor(Color.BLACK)
    g.drawLine(100, 100, 100 + v.x, 100+ v.y)
  }
}
