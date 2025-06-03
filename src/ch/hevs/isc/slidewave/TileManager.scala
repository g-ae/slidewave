package ch.hevs.isc.slidewave

import ch.hevs.gdx2d.lib.GdxGraphics
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.maps.MapLayer
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer
import com.badlogic.gdx.maps.tiled.{TiledMap, TiledMapTileLayer, TmxMapLoader}
import com.badlogic.gdx.math.{Rectangle, Vector2}

import scala.collection.mutable.ArrayBuffer

class TileManager(tiledFileName: String, val zoom: Float = 1f) {
  var tiledMap: TiledMap = new TmxMapLoader().load(tiledFileName)
  var tiledLayerCheckPoint: MapLayer = tiledMap.getLayers.get("checkpoints")
  var tiledLayerBG: TiledMapTileLayer = tiledMap.getLayers.get("bg").asInstanceOf[TiledMapTileLayer]
  var tiledMapRenderer: OrthogonalTiledMapRenderer = new OrthogonalTiledMapRenderer(tiledMap)

  def getStartingPoint: Vector2 = {
    /*
    Propriétés objet: -> sp1
    id, type, width, height, x, y
    */
    val prop = tiledLayerCheckPoint.getObjects.get("sp1").getProperties
    new Vector2(prop.get("x").asInstanceOf[Float], prop.get("y").asInstanceOf[Float])
  }

  def getFinishLine: Rectangle = getCheckpoint(0)
  def drawFinishLine(g: GdxGraphics): Unit = {
    g.setColor(Color.RED)
    val rect = getFinishLine
    g.drawFilledRectangle(rect.x + rect.width / 2, rect.y + rect.height / 2, rect.width, rect.height, 0)
  }
  def getCheckpoint(i: Int): Rectangle = {
    val obj = tiledLayerCheckPoint.getObjects.get(s"c$i")
    if (obj == null) return null

    val cp = obj.getProperties
    new Rectangle(cp.get("x").asInstanceOf[Float], cp.get("y").asInstanceOf[Float], cp.get("width").asInstanceOf[Float], cp.get("height").asInstanceOf[Float])
  }
  def getCheckpoints(withFinishLine: Boolean = false): Array[Rectangle] = {
    val checkpoints = new ArrayBuffer[Rectangle]()
    if (withFinishLine) checkpoints += getFinishLine
    var currentCheckpoint: Int = 0
    while (true) {
      val obj = getCheckpoint(currentCheckpoint)
      if (obj == null) return checkpoints.toArray

      checkpoints += obj
      currentCheckpoint += 1
    }
    checkpoints.toArray
  }
}
