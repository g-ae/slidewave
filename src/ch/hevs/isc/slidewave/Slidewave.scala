package ch.hevs.isc.slidewave

import ch.hevs.gdx2d.components.physics.utils.PhysicsScreenBoundaries
import ch.hevs.gdx2d.desktop.PortableApplication
import ch.hevs.gdx2d.desktop.physics.DebugRenderer
import ch.hevs.gdx2d.lib.GdxGraphics
import ch.hevs.gdx2d.lib.physics.PhysicsWorld
import ch.hevs.isc.slidewave.components.Car
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.{Gdx, Input}
import com.badlogic.gdx.math.Vector2

object Slidewave extends App {
    val screenWidth = 1920
    val screenHeight = 1080
    new SlidewaveWindow
}

class SlidewaveWindow extends PortableApplication(Slidewave.screenWidth, Slidewave.screenHeight) {
    var dbgRenderer: DebugRenderer = null
    val world = PhysicsWorld.getInstance()
    var c1: Car = null
    var tileManager: TileManager = null

    override def onInit(): Unit = {
        setTitle("slidewave")

        // No gravity in this world
        world.setGravity(new Vector2(0, 0))

        dbgRenderer = new DebugRenderer

        // TileManager
        tileManager = new TileManager("data/tracks/track_test2.tmx")

        new PhysicsScreenBoundaries(tileManager.tiledLayer.getWidth * tileManager.tiledLayer.getTileWidth,
            tileManager.tiledLayer.getHeight * tileManager.tiledLayer.getTileHeight)

        // Our car
        c1 = new Car(30, 70, new Vector2(675, 2290), (Math.PI/2).toFloat, 3, 30, 30)
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

        // display FPS
        g.setColor(Color.BLACK)
        g.drawString((g.getCamera.position.x - Slidewave.screenWidth /2) + 5, (g.getCamera.position.y + Slidewave.screenHeight / 2) - 5, "FPS: " + Gdx.graphics.getFramesPerSecond)
    }
}
