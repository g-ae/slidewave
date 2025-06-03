package ch.hevs.isc.slidewave

import com.badlogic.gdx.maps.MapLayer
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer
import com.badlogic.gdx.maps.tiled.{TiledMap, TiledMapTileLayer, TmxMapLoader}
import com.badlogic.gdx.math.Vector2

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
}
