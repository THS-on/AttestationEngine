package com.example.mobileattester.data.util

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.toBitmap
import com.example.mobileattester.R
import com.example.mobileattester.data.model.Element
import com.example.mobileattester.data.model.emptyElement
import com.example.mobileattester.ui.components.ElementInfoWindow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.osmdroid.bonuspack.clustering.RadiusMarkerClusterer
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.lang.ref.WeakReference

enum class MapMode {
    SINGLE_ELEMENT, EDIT_LOCATION, ALL_ELEMENTS
}

/**
 * Provides everything for the map that the application requires
 */
class MapManager(
    private val locationEditor: LocationEditor,
) {
    private var mapView: WeakReference<MapView>? = null
    private var mapListener: WeakReference<View.OnTouchListener>? = null
    private val _map: () -> MapView?
        get() = {
            mapView?.get()
        }

    val mapMode: MutableStateFlow<MapMode?> = MutableStateFlow(null)

    init {
        locationEditor.requestLocationUpdates()
    }

    /**
     * Set the map to show a location for an element.
     * @return true if the element had a location in it. False if the element did not contain
     * a location.
     */
    fun displayElement(map: MapView, element: Element): Boolean {
        initializeMap(map, MapMode.SINGLE_ELEMENT)

        val m = addMarker(element, map.context, "Element position")
        map.controller.setCenter(m.position)
        return true
    }

    fun displayElements(map: MapView, elements: List<Element>) {
        initializeMap(map, MapMode.ALL_ELEMENTS, 2.0)

        val markerIcon =
            AppCompatResources.getDrawable(map.context, R.drawable.ic_baseline_location_on_32)
                .apply {
                    this?.setTint(map.context.getColor(R.color.primary))
                }
        val clusterIcon =
            AppCompatResources.getDrawable(map.context, R.drawable.ic_baseline_circle_32).apply {
                this?.setTint(map.context.getColor(R.color.primary))
            }

        val cluster = RadiusMarkerClusterer(map.context)
        cluster.setIcon(clusterIcon!!.toBitmap())

        elements.forEach { element ->
            element.geoPoint()?.let {
                val m = Marker(map)
                m.title = element.name
                m.position = it
                m.icon = markerIcon
                m.setInfoWindow(ElementInfoWindow(map))
                cluster.add(m)
            }
        }
        map.overlayManager.add(cluster)
    }

    /**
     * Edit a location for an element.
     * @return StateFlow with the location that is being edited. Null if location was not provided,
     * and device location could not be found.
     */
    @SuppressLint("ClickableViewAccessibility")
    fun useEditLocation(map: MapView, element: Element): StateFlow<Location?> {
        initializeMap(map, MapMode.EDIT_LOCATION)
        clearMarkers()

        val locToEdit = if (element.geoPoint() != null) {
            println("@@ Using element location")
            geoToLoc(element.geoPoint()!!)
        } else {
            println("@@ Using device location ${locationEditor.deviceLocation.value}")
            locationEditor.deviceLocation.value
        } ?: geoToLoc(GeoPoint(map.mapCenter.latitude, map.mapCenter.longitude))

        locationEditor.setLocation(locToEdit)

        val gp = GeoPoint(locToEdit.latitude, locToEdit.longitude)

        addMarker(element, map.context, "New element position").apply {
            setMarkerFollowScreen(this)
        }

        _map()?.controller?.setCenter(gp)
        return locationEditor.currentLocation
    }

    fun resetMapState() {
        mapView = null
        mapMode.value = null
    }

    @SuppressLint("ClickableViewAccessibility")
    fun lockInteractions() {
        mapView?.get()?.setOnTouchListener { _, _ -> false }
    }

    fun getEditedLocation() = locationEditor.currentLocation
    fun getCurrentLocation() = locationEditor.deviceLocation

    fun centerToDevice(): GeoPoint? {
        val lat = locationEditor.deviceLocation.value?.latitude ?: return null
        val long = locationEditor.deviceLocation.value?.longitude ?: return null
        val geoPoint = GeoPoint(lat, long)
        _map()?.controller?.setCenter(geoPoint)

        // In the case we are centering when in edit mode
        if (this.mapMode.value == MapMode.EDIT_LOCATION) {
            locationEditor.setLocation(geoToLoc(geoPoint))
            clearMarkers()
            val m = _map()?.context?.let {
                addMarker(emptyElement().cloneWithNewLocation(geoToLoc(geoPoint)),
                    it,
                    "New element position")
            }
            m?.let { setMarkerFollowScreen(it) }
        }

        return geoPoint
    }

    // ------------------------- Private --------------------------------------
    // ------------------------- Private --------------------------------------

    @SuppressLint("ClickableViewAccessibility")
    private fun setMarkerFollowScreen(marker: Marker) {
        val l = object : View.OnTouchListener {
            override fun onTouch(v: View?, e: MotionEvent?): Boolean {
                if (e?.action == MotionEvent.ACTION_MOVE) {
                    val c = _map()?.mapCenter ?: return false
                    locationEditor.setLocation(geoToLoc(c as GeoPoint))
                    marker.position = GeoPoint(c)
                }
                return false
            }
        }

        mapListener = WeakReference(l)
        _map()?.setOnTouchListener(l)
    }

    private fun initializeMap(map: MapView, type: MapMode, zoomLevel: Double = 17.0) {
        mapView = WeakReference(map)
        _map()?.apply {
            setTileSource(TileSourceFactory.WIKIMEDIA)
            isTilesScaledToDpi = true
            setMultiTouchControls(true)
            controller.setZoom(zoomLevel)
        }
        clearMarkers()
        mapMode.value = type
    }

    private fun clearMarkers() {
        _map()?.overlayManager?.clear()
    }

    private fun geoToLoc(geoPoint: GeoPoint): Location {
        val location = Location("")
        val latitude: Double = geoPoint.latitude
        val longitude: Double = geoPoint.longitude

        location.latitude = latitude
        location.longitude = longitude
        return location
    }

    private fun addMarker(element: Element, ctx: Context, txt: String): Marker {
        // Position
        val marker = Marker(_map())
        marker.icon =
            AppCompatResources.getDrawable(ctx, R.drawable.ic_baseline_location_on_32).apply {
                this?.setTint(ctx.getColor(R.color.primary))
            }
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
        marker.title = txt
        marker.setInfoWindow(ElementInfoWindow(_map()))
        element.geoPoint()?.let { marker.position = it }
        _map()?.overlays?.add(marker)
        return marker
    }
}