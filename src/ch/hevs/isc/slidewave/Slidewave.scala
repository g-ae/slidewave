package ch.hevs.isc.slidewave

import ch.hevs.gdx2d.components.physics.utils.PhysicsScreenBoundaries
import ch.hevs.gdx2d.desktop.PortableApplication
import ch.hevs.gdx2d.desktop.physics.DebugRenderer
import ch.hevs.gdx2d.lib.GdxGraphics
import ch.hevs.gdx2d.lib.physics.PhysicsWorld
import ch.hevs.isc.slidewave.components.Car
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.{Gdx, Input}
import com.badlogic.gdx.math.Vector2

object Slidewave {
    val screenWidth = 1920
    val screenHeight = 1080
    lazy val playerCar: Car = new Car(30, 70, TileManager.getStartingPoint, (Math.PI/2).toFloat, 2, 30, 14)

    def main(args: Array[String]): Unit = {
        new SlidewaveWindow
    }
}

class SlidewaveWindow extends PortableApplication(Slidewave.screenWidth, Slidewave.screenHeight) {
    lazy val dbgRenderer: DebugRenderer = new DebugRenderer
    val world = PhysicsWorld.getInstance()

    // fonts
    val consolas = Gdx.files.internal("data/fonts/Consolas.ttf")
    val generator = new FreeTypeFontGenerator(consolas)

    lazy val lapTimeFont: BitmapFont = {
        val paramLapTime = new FreeTypeFontGenerator.FreeTypeFontParameter
        paramLapTime.color = Color.WHITE
        paramLapTime.size = generator.scaleForPixelHeight(72)
        paramLapTime.hinting = FreeTypeFontGenerator.Hinting.Full

        generator.generateFont(paramLapTime)
    }

    override def onInit(): Unit = {
        setTitle("Slidewave")

        // No gravity in this world
        world.setGravity(new Vector2(0, 0))

        new PhysicsScreenBoundaries(TileManager.tiledLayerBG.getWidth * TileManager.tiledLayerBG.getTileWidth,
            TileManager.tiledLayerBG.getHeight * TileManager.tiledLayerBG.getTileHeight)
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
            if (i != Slidewave.playerCar.lapController.passedCheckpoints - 1)
                if (TileManager.isCarOverCheckpoint(TileManager.checkpoints(i))) {
                    Slidewave.playerCar.wentOverCheckpoint(i)
                    println(s"went over checkpoint $i", s"new passed checkpoints : ${Slidewave.playerCar.lapController.passedCheckpoints}")
                }
        }

        // Physics update
        PhysicsWorld.updatePhysics(Gdx.graphics.getDeltaTime)

        // Move the car according to key presses
        Slidewave.playerCar.accelerate = Gdx.input.isKeyPressed(Input.Keys.DPAD_UP) || Gdx.input.isKeyPressed(Input.Keys.W)
        Slidewave.playerCar.brake = Gdx.input.isKeyPressed(Input.Keys.DPAD_DOWN) || Gdx.input.isKeyPressed(Input.Keys.S)

        // Turn the car according to key presses
        if (Gdx.input.isKeyPressed(Input.Keys.DPAD_LEFT) || Gdx.input.isKeyPressed(Input.Keys.A)) {
            Slidewave.playerCar.steer_left = true
            Slidewave.playerCar.steer_right = false
        } else if (Gdx.input.isKeyPressed(Input.Keys.DPAD_RIGHT) || Gdx.input.isKeyPressed(Input.Keys.D)) {
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

        if (Slidewave.playerCar.lapController.currentLapTimeStart != -1) {
            g.drawString(g.getCamera.position.x, (g.getCamera.position.y + Slidewave.screenHeight / 2) - 5,
                Utils.msToSecondsStr(System.currentTimeMillis() - Slidewave.playerCar.lapController.currentLapTimeStart),
                lapTimeFont)
            g.drawString((g.getCamera.position.x - Slidewave.screenWidth / 2) + 5, (g.getCamera.position.y - Slidewave.screenHeight / 2) + lapTimeFont.getLineHeight,
                s"Lap ${Slidewave.playerCar.lapController.currentLap} / ${Slidewave.playerCar.lapController.lapNumber}",
                lapTimeFont)
        } else if (Slidewave.playerCar.lapController.bestTime != -1) {
            g.drawString((g.getCamera.position.x - Slidewave.screenWidth / 2) + 5, (g.getCamera.position.y - Slidewave.screenHeight / 2) + lapTimeFont.getLineHeight,
                "Best time : " + Utils.msToSecondsStr(Slidewave.playerCar.lapController.bestTime) + " seconds",
                lapTimeFont)
        }
    }
}
