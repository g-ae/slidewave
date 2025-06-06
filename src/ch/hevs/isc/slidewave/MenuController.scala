package ch.hevs.isc.slidewave

import ch.hevs.gdx2d.components.bitmaps.BitmapImage
import ch.hevs.gdx2d.lib.GdxGraphics
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator

object MenuController {
  val gameLogo: BitmapImage = new BitmapImage("data/images/logo_nobg.png")
  val wasdImage: BitmapImage = new BitmapImage("data/images/wasd.png")
  val shiftImage: BitmapImage = new BitmapImage("data/images/shift.png")

  val consolas = Gdx.files.internal("data/fonts/Consolas.ttf")
  val generator = new FreeTypeFontGenerator(consolas)

  val font: BitmapFont = {
    val param = new FreeTypeFontGenerator.FreeTypeFontParameter
    param.color = Color.BLACK
    param.size = generator.scaleForPixelHeight(28)
    param.hinting = FreeTypeFontGenerator.Hinting.Full

    generator.generateFont(param)
  }
  def drawStartMenu(g: GdxGraphics): Unit = {
    g.setColor(Color.BLACK)
    g.drawFilledRectangle(g.getCamera.position.x, g.getCamera.position.y, Slidewave.screenWidth * 2/3 + 54, Slidewave.screenHeight * 2/3 + 54, 0)
    g.setColor(Color.GRAY)
    g.drawFilledRectangle(g.getCamera.position.x, g.getCamera.position.y, Slidewave.screenWidth * 2/3 + 40, Slidewave.screenHeight * 2/3 + 40, 0)
    g.drawTransformedPicture(g.getCamera.position.x, g.getCamera.position.y + 250, 0, 200, 200, gameLogo)
    g.drawTransformedPicture(g.getCamera.position.x - 200, g.getCamera.position.y - 130, 0, 1, wasdImage)
    g.drawString(g.getCamera.position.x - 100, g.getCamera.position.y - 30, "Accelerate", font)
    g.drawString(g.getCamera.position.x - 310, g.getCamera.position.y - 300, "Brake/Reverse", font)
    g.drawString(g.getCamera.position.x - 600, g.getCamera.position.y - 195, "Steer left", font)
    g.drawString(g.getCamera.position.x + 50, g.getCamera.position.y - 195, "Steer right", font)
    g.drawTransformedPicture(g.getCamera.position.x + 450, g.getCamera.position.y - 110, 0, 0.3f, shiftImage)
    g.drawString(g.getCamera.position.x + 380, g.getCamera.position.y - 220, "Handbrake", font)
    g.drawString(g.getCamera.position.x + 380, g.getCamera.position.y - 220, "Handbrake", font)
  }
  def drawEndMenu(g: GdxGraphics): Unit = {
    val camX = g.getCamera.position.x
    val camY = g.getCamera.position.y

    // Background rectangles
    g.setColor(Color.BLACK)
    g.drawFilledRectangle(camX, camY, Slidewave.screenWidth * 2 / 3 + 54, Slidewave.screenHeight * 2 / 3 + 54, 0)
    g.setColor(Color.GRAY)
    g.drawFilledRectangle(camX, camY, Slidewave.screenWidth * 2 / 3 + 40, Slidewave.screenHeight * 2 / 3 + 40, 0)

    // Game logo or "Game Over"
    g.drawTransformedPicture(camX, camY + 250, 0, 200, 200, gameLogo)
    // Or if no logo: g.drawString(camX - 100, camY + 250, "GAME OVER", font)

    // Time info
    g.setColor(Color.WHITE)
    g.drawString(camX - 100, camY + 50, s"Best lap: ${Utils.msToSecondsStr(Slidewave.playerCar.lapController.bestTime)}", font)
    g.drawString(camX - 100, camY - 20, s"Total time: ${Utils.msToSecondsStr(Slidewave.playerCar.lapController.totalTime)}", font)
  }
}
