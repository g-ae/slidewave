package ch.hevs.isc.slidewave

import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer
import com.badlogic.gdx.maps.tiled.{TiledMap, TiledMapTileLayer, TmxMapLoader}
import com.badlogic.gdx.math.Vector2

class TileManager(tiledFileName: String, val zoom: Float = 1f) {
  var tiledMap: TiledMap = new TmxMapLoader().load(tiledFileName)
  var tiledLayer: TiledMapTileLayer = tiledMap.getLayers.get(0).asInstanceOf[TiledMapTileLayer]
  var tiledMapRenderer: OrthogonalTiledMapRenderer = new OrthogonalTiledMapRenderer(tiledMap)

  def getStartingPoint: Vector2 = {
    //tiledLayer.getObjects.get()
    Vector2.Zero
  }
}
