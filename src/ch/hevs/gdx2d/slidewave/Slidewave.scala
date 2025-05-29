package ch.hevs.gdx2d.slidewave

import ch.hevs.gdx2d.components.physics.utils.PhysicsScreenBoundaries
import ch.hevs.gdx2d.desktop.PortableApplication
import ch.hevs.gdx2d.desktop.physics.DebugRenderer
import ch.hevs.gdx2d.lib.GdxGraphics
import ch.hevs.gdx2d.lib.physics.PhysicsWorld
import ch.hevs.gdx2d.lib.utils.Logger
import ch.hevs.gdx2d.slidewave.components.Car
import com.badlogic.gdx.{Gdx, Input}
import com.badlogic.gdx.math.Vector2

object Slidewave extends App {
    new SlidewaveWindow
}

class SlidewaveWindow extends PortableApplication{
    var dbgRenderer: DebugRenderer = null
    val world = PhysicsWorld.getInstance()
    var c1: Car = null

    override def onInit(): Unit = {
        setTitle("Slidewave alpha")
        Logger.log("Use the arrows to move the car")
        // No gravity in this world
        world.setGravity(new Vector2(0, 0))
        dbgRenderer = new DebugRenderer
        // Create the obstacles in the scene
        new PhysicsScreenBoundaries(getWindowWidth, getWindowHeight)
        // Our car
        c1 = new Car(30, 70, new Vector2(200, 200), Math.PI.toFloat, 10, 30, 15)
    }

    override def onGraphicRender(g: GdxGraphics): Unit = {
        g.clear()
        // Physics update
        PhysicsWorld.updatePhysics(Gdx.graphics.getDeltaTime)

        /**
         * Move the car according to key presses
         */
        c1.accelerate = Gdx.input.isKeyPressed(Input.Keys.DPAD_UP)
        c1.brake = Gdx.input.isKeyPressed(Input.Keys.DPAD_DOWN)

        /**
         * Turn the car according to key presses
         */
        if (Gdx.input.isKeyPressed(Input.Keys.DPAD_LEFT)) {
            c1.steer_left = true
            c1.steer_right = false
        }
        else if (Gdx.input.isKeyPressed(Input.Keys.DPAD_RIGHT)) {
            c1.steer_right = true
            c1.steer_left = false
        }
        else {
            c1.steer_left = false
            c1.steer_right = false
        }
        c1.update(Gdx.graphics.getDeltaTime)
        c1.draw(g)
        dbgRenderer.render(world, g.getCamera.combined)
        g.drawFPS()
        g.drawSchoolLogo()
    }
}
