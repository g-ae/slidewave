package ch.hevs.isc.slidewave

import ch.hevs.gdx2d.components.physics.utils.PhysicsScreenBoundaries
import ch.hevs.gdx2d.desktop.PortableApplication
import ch.hevs.gdx2d.desktop.physics.DebugRenderer
import ch.hevs.gdx2d.lib.GdxGraphics
import ch.hevs.gdx2d.lib.physics.PhysicsWorld
import ch.hevs.isc.slidewave.components.Car
import com.badlogic.gdx.{Gdx, Input}
import com.badlogic.gdx.math.Vector2

object Slidewave extends App {
    new SlidewaveWindow
}

class SlidewaveWindow extends PortableApplication(1920, 1080){
    var dbgRenderer: DebugRenderer = null
    val world = PhysicsWorld.getInstance()
    var c1: Car = null
    var tileManager: TileManager = null

    override def onInit(): Unit = {
        setTitle("Slidewave alpha")
        // No gravity in this world
        world.setGravity(new Vector2(0, 0))
        dbgRenderer = new DebugRenderer

        // TileManager
        tileManager = new TileManager("data/tracks/track_test.tmx")

        new PhysicsScreenBoundaries(tileManager.tiledLayer.getWidth * tileManager.tiledLayer.getTileWidth,
            tileManager.tiledLayer.getHeight * tileManager.tiledLayer.getTileHeight)

        // Our car
        c1 = new Car(30, 70, new Vector2(100, 100), Math.PI.toFloat, 4, 20, 15)
    }

    override def onGraphicRender(g: GdxGraphics): Unit = {
        g.clear()
        g.zoom(tileManager.zoom)
        g.moveCamera(c1.carbox.getBodyPosition.x, c1.carbox.getBodyPosition.y, tileManager.tiledLayer.getWidth * tileManager.tiledLayer.getTileWidth, tileManager.tiledLayer.getHeight * tileManager.tiledLayer.getTileHeight)

        tileManager.tiledMapRenderer.setView(g.getCamera)
        tileManager.tiledMapRenderer.render()

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

        // TODO: gérer affichage correct des FPS
        g.drawFPS()
        g.drawSchoolLogo()
    }
}
