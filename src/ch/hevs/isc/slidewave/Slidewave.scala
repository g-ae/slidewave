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
    var playerCar: Car = null
    new SlidewaveWindow
}

class SlidewaveWindow extends PortableApplication(Slidewave.screenWidth, Slidewave.screenHeight) {
    var dbgRenderer: DebugRenderer = null
    val world = PhysicsWorld.getInstance()

    override def onInit(): Unit = {
        setTitle("Slidewave")

        // No gravity in this world
        world.setGravity(new Vector2(0, 0))

        dbgRenderer = new DebugRenderer

        new PhysicsScreenBoundaries(TileManager.tiledLayerBG.getWidth * TileManager.tiledLayerBG.getTileWidth,
            TileManager.tiledLayerBG.getHeight * TileManager.tiledLayerBG.getTileHeight)

        Slidewave.playerCar = new Car(30, 70, TileManager.getStartingPoint, (Math.PI/2).toFloat, 3, 30, 30)
    }
    override def onGraphicRender(g: GdxGraphics): Unit = {
        g.clear()
        g.zoom(TileManager.zoom)
        g.moveCamera(
            Math.round(Slidewave.playerCar.carbox.getBodyPosition.x),
            Math.round(Slidewave.playerCar.carbox.getBodyPosition.y),
            TileManager.tiledLayerBG.getWidth * TileManager.tiledLayerBG.getTileWidth,
            TileManager.tiledLayerBG.getHeight * TileManager.tiledLayerBG.getTileHeight
        )

        TileManager.tiledMapRenderer.setView(g.getCamera)
        TileManager.tiledMapRenderer.render()

        // Test for checkpoints
        for (i <- TileManager.checkpoints.indices) {
            if (i != Slidewave.playerCar.passedCheckpoints - 1)
                if (TileManager.isCarOverCheckpoint(TileManager.checkpoints(i))) {
                    Slidewave.playerCar.wentOverCheckpoint(i)
                    println(s"went over checkpoint $i", s"new passed checkpoints : ${Slidewave.playerCar.passedCheckpoints}")
                }
        }

        // Physics update
        PhysicsWorld.updatePhysics(Gdx.graphics.getDeltaTime)

        // Move the car according to key presses
        Slidewave.playerCar.accelerate = Gdx.input.isKeyPressed(Input.Keys.DPAD_UP)
        Slidewave.playerCar.brake = Gdx.input.isKeyPressed(Input.Keys.DPAD_DOWN)

        // Turn the car according to key presses
        if (Gdx.input.isKeyPressed(Input.Keys.DPAD_LEFT)) {
            Slidewave.playerCar.steer_left = true
            Slidewave.playerCar.steer_right = false
        } else if (Gdx.input.isKeyPressed(Input.Keys.DPAD_RIGHT)) {
            Slidewave.playerCar.steer_right = true
            Slidewave.playerCar.steer_left = false
        } else {
            Slidewave.playerCar.steer_left = false
            Slidewave.playerCar.steer_right = false
        }
        Slidewave.playerCar.update(Gdx.graphics.getDeltaTime)
        Slidewave.playerCar.draw(g)

        dbgRenderer.render(world, g.getCamera.combined) // causes cam lag when on

        // display FPS
        g.setColor(Color.BLACK)
        g.drawString((g.getCamera.position.x - Slidewave.screenWidth /2) + 5, (g.getCamera.position.y + Slidewave.screenHeight / 2) - 5, "FPS: " + Gdx.graphics.getFramesPerSecond)
    }
}
