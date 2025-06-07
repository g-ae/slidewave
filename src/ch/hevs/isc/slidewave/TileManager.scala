package ch.hevs.isc.slidewave

import ch.hevs.isc.slidewave.components.Wheel
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.maps.MapLayer
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer
import com.badlogic.gdx.maps.tiled.{TiledMap, TiledMapTileLayer, TmxMapLoader}
import com.badlogic.gdx.math.{Intersector, Polygon, Vector2}
import scala.collection.mutable.ArrayBuffer

/**
 * TileManager
 *
 * Central utility object that loads the Tiled map for the current track and exposes helper
 * functions to query checkpoints, tile properties and grip values. It also sets up renderers
 * for both the main view and the in‑game mini‑map.
 *
 * All coordinates that originate from the `.tmx` file are **pixel‑based** unless explicitly
 * converted to physics metres.
 */
object TileManager {
  // ---------------------------------------------------------------------------
  // Tiled map & layers
  // ---------------------------------------------------------------------------
  var tiledMap: TiledMap = new TmxMapLoader().load("data/tracks/track1.tmx") // Full Tiled map of the level, loaded once at application startup.
  var tiledLayerCheckPoint: MapLayer = tiledMap.getLayers.get("cp") // Object layer that contains checkpoints and the spawn point (layer name: "cp")
  var tiledLayerBG: TiledMapTileLayer = tiledMap.getLayers.get("track").asInstanceOf[TiledMapTileLayer] // Background tile layer that represents the drivable surface (layer name: "track").
  var tiledMapRenderer: OrthogonalTiledMapRenderer = new OrthogonalTiledMapRenderer(tiledMap) // Renderer tied to the main camera for the normal game view

  // ---------------------------------------------------------------------------
  // Mini‑map support
  // ---------------------------------------------------------------------------
  var miniMapRenderer: OrthogonalTiledMapRenderer = new OrthogonalTiledMapRenderer(tiledMap) // Separate renderer used exclusively for the mini‑map.
  val miniMapHeight = 200 // Height of the mini‑map viewport (pixels)
  val minMapWidth = 300 // Width of the mini‑map viewport (pixels)
  val miniCam = new OrthographicCamera(minMapWidth, miniMapHeight) // Independent camera controlling the mini‑map view

  // Icon rendered on the mini‑map to show the player position.
  val carMiniTexture = new com.badlogic.gdx.graphics.Texture("data/images/bmw-logo.png")
  val carMiniRegion  = new com.badlogic.gdx.graphics.g2d.TextureRegion(carMiniTexture)

  // ---------------------------------------------------------------------------
  // Checkpoints & Parameters
  // ---------------------------------------------------------------------------
  val checkpoints = setupCheckpoints()  // Ordered array of checkpoint polygons extracted from the Tiled map.
  val zoom = 1f // Default zoom factor used by the main camera.

  // ---------------------------------------------------------------------------
  // Functions
  // ---------------------------------------------------------------------------
  /**
   * Retrieves the raw spawn point defined in the Tiled map object named **"sp1"**.
   * The coordinates are **pixel** based (same unit as the map texture).
   *
   * @return Vector2 containing the spawn location in pixels.
   */
  def getStartingPoint: Vector2 = {
    /*
     * Object properties for "sp1": id, type, width, height, x, y
     */
    val prop = tiledLayerCheckPoint.getObjects.get("sp1").getProperties
    new Vector2(prop.get("x").asInstanceOf[Float], prop.get("y").asInstanceOf[Float])
  }

  /**
   * Returns the starting point converted to physics metres (Box2D world units).
   * The magic scale factor (≈ 1 / 75) was determined empirically so that in‑game
   * distances feel realistic.
   */
  def getStartingPointMeters: Vector2 = getStartingPoint.cpy().scl(0.013333f) // Trial‑and‑error factor

  /**
   * Fetches checkpoint **i** from the object layer. The objects are named "c0", "c1", ...
   *
   * @param i Sequential index of the checkpoint (0‑based).
   * @return Polygon representing the checkpoint bounds in pixel space, or `null` when no such
   *         checkpoint exists.
   */
  def getCheckpoint(i: Int): Polygon = {
    val obj = tiledLayerCheckPoint.getObjects.get(s"c$i")
    if (obj == null) return null

    val cp = obj.getProperties
    val width  = cp.get("width").asInstanceOf[Float]
    val height = cp.get("height").asInstanceOf[Float]
    val checkpointPolygon = new Polygon(Array(
      0f,     0f,
      width,  0f,
      width,  height,
      0f,     height
    ))
    checkpointPolygon.setPosition(cp.get("x").asInstanceOf[Float], cp.get("y").asInstanceOf[Float])
    checkpointPolygon
  }

  /**
   * Builds the ordered list of checkpoints by iterating until a `null` object is returned.
   * Executed once during static initialisation.
   *
   * @return Array containing checkpoint polygons in traversal order.
   */
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

  /**
   * Checks whether the player's car is overlapping the provided checkpoint polygon.
   *
   * @param cp Checkpoint polygon to test against the car shape.
   * @return `true` when the car intersects the checkpoint, otherwise `false`.
   */
  def isCarOverCheckpoint(cp: Polygon): Boolean = Intersector.overlapConvexPolygons(cp, Slidewave.playerCar.carPolygon)

  /**
   * Returns the tile cell located at the specified world coordinates (pixels).
   *
   * @param x Horizontal map coordinate in pixels.
   * @param y Vertical map coordinate in pixels.
   * @return The `Cell` under the coordinates, or `null` if outside the map.
   */
  def getTileAt(x: Float, y: Float): TiledMapTileLayer.Cell = {
    tiledLayerBG.getCell(
      Math.floor(x / tiledLayerBG.getTileWidth).toInt,
      Math.floor(y / tiledLayerBG.getTileHeight).toInt
    )
  }

  /**
   * Convenience overload: returns the tile under the given wheel.
   */
  def getTileAt(wheel: Wheel): TiledMapTileLayer.Cell = getTileAt(wheel.wheelbox.getBodyPosition.x, wheel.wheelbox.getBodyPosition.y)

  /**
   * Determines whether the specified wheel is still within the track bounds.
   *
   * @param wheel Wheel to test.
   */
  def isWheelInTrack(wheel: Wheel): Boolean = getTileAt(wheel) != null

  /**
   * Retrieves the logical ground type of the tile directly under the provided wheel.
   * Possible return values:
   *   - `null`   → wheel is outside the map
   *   - "sand"  → low‑grip sand tile
   *   - "track" → regular asphalt (default)
   */
  def getTileUnderWheelType(w: Wheel): String = {
    val t = getTileAt(w)
    if (t == null) return null  // out of map bounds

    val prop = t.getTile.getProperties.get("ground")

    if (prop != null) prop.asInstanceOf[String] // explicit ground property (e.g. sand)
    else "track"                                // fallback: track asphalt
  }

  /**
   * Calculates the grip factor (used by `killSidewaysVelocity`) for the tile under the wheel.
   *
   * @param w Wheel of interest.
   * @return Grip coefficient in the range 0.0 – 1.0 (higher → more grip).
   */
  def getTileUnderWheelGrip(w: Wheel): Float = getTileTypeGrip(getTileUnderWheelType(w))

  /**
   * Maps a textual ground type to its numeric grip coefficient.
   *
   * @param s Ground type string (`null`, "sand", "track", ...).
   * @return Empirically chosen grip coefficient.
   */
  def getTileTypeGrip(s: String): Float = {
    s match {
      case null     => 0.95f // grass / off‑road: very high resistance
      case "sand"   => 0.8f  // low‑grip sand
      case "track"  => 0f    // asphalt: let the physics handle it naturally
      case _        => 0f    // default / unknown
    }
  }
}
