package ch.hevs.isc.slidewave

import ch.hevs.gdx2d.lib.GdxGraphics
import ch.hevs.isc.slidewave.components.Wheel
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.maps.MapLayer
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer
import com.badlogic.gdx.maps.tiled.{TiledMap, TiledMapTileLayer, TmxMapLoader}
import com.badlogic.gdx.math.{Intersector, Polygon, Vector2}

import scala.collection.mutable.ArrayBuffer

object TileManager {
  var tiledMap: TiledMap = new TmxMapLoader().load("data/tracks/track_test.tmx")
  var tiledLayerCheckPoint: MapLayer = tiledMap.getLayers.get("cp")
  var tiledLayerBG: TiledMapTileLayer = tiledMap.getLayers.get("track").asInstanceOf[TiledMapTileLayer]
  var tiledMapRenderer: OrthogonalTiledMapRenderer = new OrthogonalTiledMapRenderer(tiledMap)
  val checkpoints = setupCheckpoints()  // ordered
  val zoom = 1f

  def getStartingPoint: Vector2 = {
    /*
    Propriétés objet: -> sp1
    id, type, width, height, x, y
    */
    val prop = tiledLayerCheckPoint.getObjects.get("sp1").getProperties
    new Vector2(prop.get("x").asInstanceOf[Float], prop.get("y").asInstanceOf[Float])
  }

  // Draw checkpoints for debug
  def drawCheckpoint(g: GdxGraphics, checkpoint: Polygon): Unit = {
    g.setColor(if (isCarOverCheckpoint(checkpoint)) Color.RED else Color.BLUE)
  }
  def drawCheckpoint(g: GdxGraphics, checkpoint: Int): Unit = {
    val cPolygon = getCheckpoint(checkpoint)
    if (cPolygon != null) drawCheckpoint(g, cPolygon)
  }
  def drawCheckpoints(g: GdxGraphics): Unit = for (r <- checkpoints) drawCheckpoint(g, r)

  // get checkpoint from number
  def getCheckpoint(i: Int): Polygon = {
    val obj = tiledLayerCheckPoint.getObjects.get(s"c$i")
    if (obj == null) return null

    val cp = obj.getProperties
    val width = cp.get("width").asInstanceOf[Float]
    val height = cp.get("height").asInstanceOf[Float]
    val checkpointPolygon = new Polygon(Array(
      0, 0,
      width, 0,
      width, height,
      0, height
    ))
    checkpointPolygon.setPosition(cp.get("x").asInstanceOf[Float], cp.get("y").asInstanceOf[Float])
    checkpointPolygon
  }
  private def setupCheckpoints(): Array[Polygon] = {
    val checkpoints = new ArrayBuffer[Polygon]()
    var currentCheckpoint: Int = 0
    while (true) {
      val obj = getCheckpoint(currentCheckpoint)
      if (obj == null) return checkpoints.toArray

      checkpoints += obj
      currentCheckpoint += 1
    }
    checkpoints.toArray
  }
  def isCarOverCheckpoint(cp: Polygon): Boolean = Intersector.overlapConvexPolygons(cp, Slidewave.playerCar.carPolygon)
  def getTileAt(x: Float, y: Float): TiledMapTileLayer.Cell = {
    tiledLayerBG.getCell(
      Math.floor(x / tiledLayerBG.getTileWidth).toInt,
      Math.floor(y / tiledLayerBG.getTileHeight).toInt
    )
  }
  def isWheelInTrack(wheel: Wheel): Boolean = {
    // todo: change this
    val tile = getTileAt(wheel.wheelbox.getBodyPosition.x, wheel.wheelbox.getBodyPosition.y)
    tile != null && tile.getTile.getId != 49 // grass
  }
}