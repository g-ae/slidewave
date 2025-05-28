package ch.hevs.gdx2d.slidewave

import ch.hevs.gdx2d.desktop.PortableApplication
import ch.hevs.gdx2d.lib.GdxGraphics

object Slidewave extends App {
    new SlidewaveWindow
}

class SlidewaveWindow extends PortableApplication{

    override def onInit(): Unit = {
        setTitle("Slidewave alpha")
    }

    override def onGraphicRender(g: GdxGraphics): Unit = {
        g.clear()

        // draw everything
        g.drawFPS()
    }
}
