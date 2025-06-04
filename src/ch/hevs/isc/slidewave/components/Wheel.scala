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

class Wheel(
           val car: Car,
           wheelPos: Vector2,
           width: Float,
           length: Float,
           val revolving: Boolean, // est-ce que la roue tourne en gros
           val powered: Boolean // ajouter sur les roues où l'on met le POWER
           ) extends DrawableObject {
  val world: World = PhysicsWorld.getInstance()
  val x: Vector2 = PhysicsConstants.coordPixelsToMeters(wheelPos)
  var wheelAngle: Float = 0f

  // convert car position to pixels
  val carPos: Vector2 = car.carbox.getBody.getWorldPoint(x)

  // créer une physique pour la roue
  val wheelbox: PhysicsBox = new PhysicsBox("wheel", PhysicsConstants.coordMetersToPixels(carPos), width, length /2, car.carbox.getBodyAngle)

  if (revolving) { // create ar evoluting joint to connect wheel to body
    // la roue pourra changer de sens (roues avant sur voiture lambda)
    val jointdef: RevoluteJointDef = new RevoluteJointDef
    jointdef.initialize(car.carbox.getBody, wheelbox.getBody, wheelbox.getBody.getWorldCenter)
    jointdef.enableMotor = false
    world.createJoint(jointdef)
  } else {
    val jointdef: PrismaticJointDef = new PrismaticJointDef
    jointdef.initialize(car.carbox.getBody, wheelbox.getBody, wheelbox.getBody.getWorldCenter, new Vector2(1,0)) // euhhh g pas compri
    jointdef.enableLimit = true
    jointdef.lowerTranslation = 0
    jointdef.upperTranslation = 0
    world.createJoint(jointdef)
  }

  /**
   * Change wheel angle -> relative to current car angle
   * @param angle
   */
  def setAngle(angle: Float): Unit = {
    val velocity = wheelbox.getBody.getLinearVelocity
    val speed = math.sqrt(velocity.x * velocity.x + velocity.y * velocity.y)

    // Paramètres de friction
    val maxAngle = Math.toRadians(30).toFloat  // angle max à vitesse nulle
    val speedThreshold = 10f                   // vitesse où la friction commence
    val frictionFactor = 0.8f                  // intensité de la friction (0-1)

    // Calcul de la réduction d'angle basée sur la vitesse
    val speedRatio = math.min(speed / speedThreshold, 1f)
    val angleReduction = speedRatio * frictionFactor
    val effectiveMaxAngle = maxAngle * (1f - angleReduction)

    // Limiter l'angle cible
    val clampedAngle = math.max(-effectiveMaxAngle,
      math.min(effectiveMaxAngle, Math.toRadians(angle).toFloat)).toFloat

    wheelAngle = Math.toDegrees(clampedAngle).toFloat
    wheelbox.getBody.setTransform(wheelbox.getBody.getPosition.x, wheelbox.getBody.getPosition.y, car.carbox.getBodyAngle + clampedAngle)
  }

  def getAngle: Float = wheelAngle

  def getLocalVelocity: Vector2 = {
    car.carbox.getBody.getLocalVector(car.carbox.getBody.getLinearVelocityFromLocalPoint(wheelbox.getBody.getPosition))
  }

  def getDirectionVector: Vector2 = {
    val directionVector = new Vector2(0, if (getLocalVelocity.y > 0) 1 else -1)

    directionVector.rotate(Math.toDegrees(wheelbox.getBody.getAngle).toFloat)
  }

  def getKillVelocityVector: Vector2 = {
    val sidewaysAxis: Vector2 = getDirectionVector
    val dotprod: Float = wheelbox.getBody.getLinearVelocity.dot(sidewaysAxis)

    new Vector2(sidewaysAxis.x * dotprod, sidewaysAxis.y * dotprod)
  }

  def killSidewaysVelocity(strength: Float = 0f): Unit = {
    val lateralVelocity = wheelbox.getBody.getLinearVelocity.cpy().sub(getKillVelocityVector)
    val reducedLateral = lateralVelocity.scl(strength) // 0 = pas de friction, 1 = friction totale
    val newVelocity = getKillVelocityVector.add(reducedLateral)
    wheelbox.getBody.setLinearVelocity(newVelocity)
  }

  def draw(g: GdxGraphics): Unit = {
    val pos = wheelbox.getBodyPosition
    g.drawFilledRectangle(pos.x, pos.y, length / 2, width, Math.toDegrees(car.carbox.getBodyAngle).toFloat + 90f + getAngle, Color.BLACK)
  }
}