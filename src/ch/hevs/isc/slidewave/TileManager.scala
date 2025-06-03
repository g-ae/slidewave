package ch.hevs.isc.slidewave

import ch.hevs.gdx2d.lib.GdxGraphics
import ch.hevs.isc.slidewave.components.{Car, Wheel}
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.maps.MapLayer
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer
import com.badlogic.gdx.maps.tiled.{TiledMap, TiledMapTileLayer, TmxMapLoader}
import com.badlogic.gdx.math.{Rectangle, Vector2}

import scala.collection.mutable.ArrayBuffer

object TileManager {
  var tiledMap: TiledMap = new TmxMapLoader().load("data/tracks/track_test2.tmx")
  var tiledLayerCheckPoint: MapLayer = tiledMap.getLayers.get("checkpoints")
  var tiledLayerBG: TiledMapTileLayer = tiledMap.getLayers.get("bg").asInstanceOf[TiledMapTileLayer]
  var tiledMapRenderer: OrthogonalTiledMapRenderer = new OrthogonalTiledMapRenderer(tiledMap)

  val zoom = 1f

  def getStartingPoint: Vector2 = {
    /*
    Propriétés objet: -> sp1
    id, type, width, height, x, y
    */
    val prop = tiledLayerCheckPoint.getObjects.get("sp1").getProperties
    new Vector2(prop.get("x").asInstanceOf[Float], prop.get("y").asInstanceOf[Float])
  }
  def getFinishLine: Rectangle = getCheckpoint(0)
  def drawCheckpoint(g: GdxGraphics, checkpoint: Rectangle): Unit = {
    // todo: isCarOverCheckpoint trouver moyen de donner la voiture
    g.setColor(/*if (isCarOverCheckpoint(, checkpoint)) Color.RED else */Color.BLUE)
    g.drawFilledRectangle(checkpoint.x + checkpoint.width / 2, checkpoint.y + checkpoint.height / 2, checkpoint.width, checkpoint.height, 0)
  }
  def drawCheckpoint(g: GdxGraphics, checkpoint: Int): Unit = {
    val rect = getCheckpoint(checkpoint)
    if (rect != null) drawCheckpoint(g, rect)
  }
  def drawCheckpoints(g: GdxGraphics): Unit = for (r <- getCheckpoints(true)) drawCheckpoint(g, r)
  def getCheckpoint(i: Int): Rectangle = {
    val obj = tiledLayerCheckPoint.getObjects.get(s"c$i")
    if (obj == null) return null

    val cp = obj.getProperties
    new Rectangle(cp.get("x").asInstanceOf[Float], cp.get("y").asInstanceOf[Float], cp.get("width").asInstanceOf[Float], cp.get("height").asInstanceOf[Float])
  }
  def getCheckpoints(withFinishLine: Boolean = false): Array[Rectangle] = {
    val checkpoints = new ArrayBuffer[Rectangle]()
    var currentCheckpoint: Int = if (withFinishLine) 0 else 1
    while (true) {
      val obj = getCheckpoint(currentCheckpoint)
      if (obj == null) return checkpoints.toArray

      checkpoints += obj
      currentCheckpoint += 1
    }
    checkpoints.toArray
  }
  def isCarOverCheckpoint(car: Car, cp: Rectangle): Boolean = car.carRectangle.overlaps(cp)
  def getTileAt(x: Float, y: Float): TiledMapTileLayer.Cell = {
    tiledLayerBG.getCell(
      Math.floor(x / tiledLayerBG.getTileWidth).toInt,
      Math.floor(y / tiledLayerBG.getTileHeight).toInt
    )
  }
  def isWheelInTrack(wheel: Wheel): Boolean = {
    val tile = getTileAt(wheel.wheel.getBodyPosition.x, wheel.wheel.getBodyPosition.y)
    tile != null && tile.getTile.getId != 49
  }
  def drawCurrentTile(g: GdxGraphics, wheel: Wheel): Unit = {
    val tile = getTileAt(wheel.wheel.getBodyPosition.x, wheel.wheel.getBodyPosition.y)
    if (tile == null) return
    g.drawFilledCircle(tile.getTile.getOffsetX, tile.getTile.getOffsetY, 10f, Color.PINK)
  }
}
