package ch.hevs.isc.slidewave

import ch.hevs.isc.slidewave.components.Wheel
import com.badlogic.gdx.graphics.{OrthographicCamera}
import com.badlogic.gdx.maps.MapLayer
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer
import com.badlogic.gdx.maps.tiled.{TiledMap, TiledMapTileLayer, TmxMapLoader}
import com.badlogic.gdx.math.{Intersector, Polygon, Vector2}

import scala.collection.mutable.ArrayBuffer

object TileManager {
  var tiledMap: TiledMap = new TmxMapLoader().load("data/tracks/track1.tmx")
  var tiledLayerCheckPoint: MapLayer = tiledMap.getLayers.get("cp")
  var tiledLayerBG: TiledMapTileLayer = tiledMap.getLayers.get("track").asInstanceOf[TiledMapTileLayer]
  var tiledMapRenderer: OrthogonalTiledMapRenderer = new OrthogonalTiledMapRenderer(tiledMap)

  // mini map
  var miniMapRenderer: OrthogonalTiledMapRenderer = new OrthogonalTiledMapRenderer(tiledMap)
  val miniMapHeight = 200
  val minMapWidth = 300
  val miniCam = new OrthographicCamera(minMapWidth, miniMapHeight)
  val carMiniTexture = new com.badlogic.gdx.graphics.Texture("data/images/bmw-logo.png")
  val carMiniRegion  = new com.badlogic.gdx.graphics.g2d.TextureRegion(carMiniTexture)

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
  def getTileAt(wheel: Wheel): TiledMapTileLayer.Cell = getTileAt(wheel.wheelbox.getBodyPosition.x, wheel.wheelbox.getBodyPosition.y)

  def isWheelInTrack(wheel: Wheel): Boolean = getTileAt(wheel) != null
  def getTileUnderWheelType(w: Wheel): String = {
    val t = getTileAt(w)
    if (t == null) return null  // out of bounds

    val prop = t.getTile.getProperties.get("ground")

    if (prop != null) prop.asInstanceOf[String] // in track, probably on sand
    else "track"  // track
  }

  /**
   * Get `killSidewaysVelocity` strength
   */
  def getTileUnderWheelGrip(w: Wheel): Float = getTileTypeGrip(getTileUnderWheelType(w))
  def getTileTypeGrip(s: String): Float = {
    s match {
      case null => 0.95f
      case "sand" => 0.8f
      case "track" => 0f
      case _ => 0f
    }
  }
}