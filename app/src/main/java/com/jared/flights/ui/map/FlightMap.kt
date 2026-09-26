package com.jared.flights.ui.map

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.jared.flights.R
import com.jared.flights.core.model.Geo
import com.jared.flights.core.model.LatLon
import com.jared.flights.core.model.LivePosition
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapLibreMapOptions
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point

// Free map styles from OpenFreeMap: no API key, commercial use allowed (DECISIONS #12, #41).
private const val STYLE_LIGHT = "https://tiles.openfreemap.org/styles/positron"
private const val STYLE_DARK = "https://tiles.openfreemap.org/styles/dark"

/** What to draw: the route between two airports, and the plane if we know where it is. */
data class FlightMapData(val from: LatLon, val to: LatLon, val position: LivePosition?)

/**
 * The live map: great-circle route (flown part solid, rest dashed), both airports,
 * and the plane pointing the way it's flying. Light or dark to match the app.
 * [interactive] = false for the small preview (no panning or zooming).
 * Always shows the OpenStreetMap credit, which the map data licence requires.
 */
@Composable
fun FlightMap(
    data: FlightMapData,
    interactive: Boolean,
    modifier: Modifier = Modifier,
    /** Space (dp) kept around the route when zooming to fit it. */
    fitPaddingDp: Int = 32,
) {
    val context = LocalContext.current
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val colors = MapColors(
        flown = MaterialTheme.colorScheme.onSurface.toArgb(),
        remaining = MaterialTheme.colorScheme.onSurfaceVariant.toArgb(),
        plane = MaterialTheme.colorScheme.onSurface.toArgb(),
        halo = MaterialTheme.colorScheme.surface.toArgb(),
        airport = MaterialTheme.colorScheme.primary.toArgb(),
    )
    val currentData by rememberUpdatedState(data)
    val currentColors by rememberUpdatedState(colors)

    val mapView = remember {
        MapLibre.getInstance(context)
        // The small preview sits in a scrolling page: "texture mode" keeps it drawn
        // in the right place while scrolling. The full-screen map uses the faster default.
        val options = MapLibreMapOptions.createFromAttributes(context).textureMode(!interactive)
        MapView(context, options).also { it.onCreate(null) }
    }
    val holder = remember { MapHolder() }

    // Pass the screen's lifecycle (start/stop/…) on to the map, as MapLibre needs.
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            mapView.onPause()
            mapView.onStop()
            mapView.onDestroy()
        }
    }

    // (Re)load the map style when light/dark changes, then draw everything.
    val styleUrl = if (dark) STYLE_DARK else STYLE_LIGHT
    LaunchedEffect(styleUrl) {
        mapView.getMapAsync { map ->
            holder.map = map
            map.uiSettings.apply {
                isLogoEnabled = false
                isAttributionEnabled = false // we show the credit ourselves, always visible
                isCompassEnabled = interactive
                setAllGesturesEnabled(interactive)
            }
            map.setStyle(Style.Builder().fromUri(styleUrl)) { style ->
                holder.style = style
                draw(context, style, currentData, currentColors)
                mapView.post { fitRoute(map, currentData, fitPaddingDp, context) }
            }
        }
    }

    // New position (every minute) or colours: redraw without reloading the map.
    LaunchedEffect(data, colors) {
        holder.style?.let { draw(context, it, data, colors) }
    }

    Box(modifier) {
        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())
        MapAttribution(Modifier.align(Alignment.BottomStart).padding(6.dp))
    }
}

/** The data licence requires this credit to be visible on the map. */
@Composable
private fun MapAttribution(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.map_attribution),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), MaterialTheme.shapes.small)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}



private class MapHolder {
    var map: MapLibreMap? = null
    var style: Style? = null
}

private data class MapColors(val flown: Int, val remaining: Int, val plane: Int, val halo: Int, val airport: Int)

private const val SRC_DONE = "route-done"
private const val SRC_TODO = "route-todo"
private const val SRC_AIRPORTS = "airports"
private const val SRC_PLANE = "plane"
private const val IMG_PLANE = "plane-icon"

/** Add or update the route, airports and plane on the map. */
private fun draw(context: Context, style: Style, data: FlightMapData, colors: MapColors) {
    val pos = data.position?.location
    val flownPath = if (pos != null) Geo.path(data.from, pos, 48) else emptyList()
    val todoPath = Geo.path(pos ?: data.from, data.to, 48)

    setSource(style, SRC_DONE, lineFeature(flownPath))
    setSource(style, SRC_TODO, lineFeature(todoPath))
    setSource(
        style, SRC_AIRPORTS,
        FeatureCollection.fromFeatures(listOf(data.from, data.to).map { Feature.fromGeometry(it.toPoint()) }),
    )
    setSource(
        style, SRC_PLANE,
        FeatureCollection.fromFeatures(
            listOfNotNull(
                data.position?.let { p ->
                    Feature.fromGeometry(p.location.toPoint()).apply { addNumberProperty("heading", p.headingDegrees) }
                },
            ),
        ),
    )

    if (style.getImage(IMG_PLANE) == null) {
        ContextCompat.getDrawable(context, R.drawable.ic_map_plane)?.let {
            style.addImage(IMG_PLANE, it.toBitmap(96, 96), true)
        }
    }

    fun addOrUpdate(layer: org.maplibre.android.style.layers.Layer) {
        style.getLayer(layer.id)?.let { style.removeLayer(it) }
        style.addLayer(layer)
    }
    addOrUpdate(
        LineLayer("route-todo-line", SRC_TODO).withProperties(
            PropertyFactory.lineColor(colors.remaining),
            PropertyFactory.lineWidth(2f),
            PropertyFactory.lineDasharray(arrayOf(2f, 2f)),
            PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
        ),
    )
    addOrUpdate(
        LineLayer("route-done-line", SRC_DONE).withProperties(
            PropertyFactory.lineColor(colors.flown),
            PropertyFactory.lineWidth(2.5f),
            PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
        ),
    )
    addOrUpdate(
        CircleLayer("airports-dots", SRC_AIRPORTS).withProperties(
            PropertyFactory.circleRadius(5f),
            PropertyFactory.circleColor(colors.halo),
            PropertyFactory.circleStrokeColor(colors.airport),
            PropertyFactory.circleStrokeWidth(2f),
        ),
    )
    addOrUpdate(
        SymbolLayer("plane-symbol", SRC_PLANE).withProperties(
            PropertyFactory.iconImage(IMG_PLANE),
            PropertyFactory.iconSize(0.9f),
            PropertyFactory.iconRotate(Expression.get("heading")),
            PropertyFactory.iconRotationAlignment(Property.ICON_ROTATION_ALIGNMENT_MAP),
            PropertyFactory.iconAllowOverlap(true),
            PropertyFactory.iconIgnorePlacement(true),
            PropertyFactory.iconColor(colors.plane),
            PropertyFactory.iconHaloColor(colors.halo),
            PropertyFactory.iconHaloWidth(2f),
        ),
    )
}

private fun setSource(style: Style, id: String, features: FeatureCollection) {
    val existing = style.getSourceAs<GeoJsonSource>(id)
    if (existing != null) existing.setGeoJson(features) else style.addSource(GeoJsonSource(id, features))
}

private fun lineFeature(points: List<LatLon>): FeatureCollection =
    if (points.size < 2) {
        FeatureCollection.fromFeatures(emptyList())
    } else {
        FeatureCollection.fromFeature(Feature.fromGeometry(LineString.fromLngLats(points.map { it.toPoint() })))
    }

private fun LatLon.toPoint(): Point = Point.fromLngLat(longitude, latitude)

/** Zoom and centre so the whole route fits, with some space around it. */
private fun fitRoute(map: MapLibreMap, data: FlightMapData, paddingDp: Int, context: Context) {
    val points = Geo.path(data.from, data.to, 32)
    val bounds = LatLngBounds.Builder().apply {
        points.forEach { include(LatLng(it.latitude, it.longitude)) }
    }.build()
    val padding = (paddingDp * context.resources.displayMetrics.density).toInt()
    runCatching { map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding)) }
}
