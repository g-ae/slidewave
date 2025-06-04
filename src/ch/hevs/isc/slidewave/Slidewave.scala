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
    var car: Car = null

    override def onInit(): Unit = {
        setTitle("Slidewave")

        // No gravity in this world
        world.setGravity(new Vector2(0, 0))

        dbgRenderer = new DebugRenderer

        new PhysicsScreenBoundaries(TileManager.tiledLayerBG.getWidth * TileManager.tiledLayerBG.getTileWidth,
            TileManager.tiledLayerBG.getHeight * TileManager.tiledLayerBG.getTileHeight)

        // Our car
        car = new Car(30, 70, TileManager.getStartingPoint, (Math.PI/2).toFloat, 3, 30, 30)
    }
    override def onGraphicRender(g: GdxGraphics): Unit = {
        g.clear()
        g.zoom(TileManager.zoom)
        g.moveCamera(
            Math.round(car.carbox.getBodyPosition.x),
            Math.round(car.carbox.getBodyPosition.y),
            TileManager.tiledLayerBG.getWidth * TileManager.tiledLayerBG.getTileWidth,
            TileManager.tiledLayerBG.getHeight * TileManager.tiledLayerBG.getTileHeight
        )

        TileManager.tiledMapRenderer.setView(g.getCamera)
        TileManager.tiledMapRenderer.render()
        TileManager.drawCheckpoints(g)

        // Physics update
        PhysicsWorld.updatePhysics(Gdx.graphics.getDeltaTime)

        // Move the car according to key presses
        car.accelerate = Gdx.input.isKeyPressed(Input.Keys.DPAD_UP)
        car.brake = Gdx.input.isKeyPressed(Input.Keys.DPAD_DOWN)

        // Turn the car according to key presses
        if (Gdx.input.isKeyPressed(Input.Keys.DPAD_LEFT)) {
            car.steer_left = true
            car.steer_right = false
        } else if (Gdx.input.isKeyPressed(Input.Keys.DPAD_RIGHT)) {
            car.steer_right = true
            car.steer_left = false
        } else {
            car.steer_left = false
            car.steer_right = false
        }
        car.update(Gdx.graphics.getDeltaTime)
        car.draw(g)
        // dbgRenderer.render(world, g.getCamera.combined) // cause cam lag when on

        TileManager.drawCurrentTile(g, car.wheels(0))

        // display FPS
        g.setColor(Color.BLACK)
        g.drawString((g.getCamera.position.x - Slidewave.screenWidth /2) + 5, (g.getCamera.position.y + Slidewave.screenHeight / 2) - 5, "FPS: " + Gdx.graphics.getFramesPerSecond)
    }
}
