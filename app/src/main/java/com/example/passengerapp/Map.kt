package com.example.passengerapp

import android.Manifest
import android.animation.ValueAnimator
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.res.ResourcesCompat
import com.google.firebase.firestore.FirebaseFirestore
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.geocoding.v5.models.CarmenFeature
import com.mapbox.core.constants.Constants
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.annotations.IconFactory
import com.mapbox.mapboxsdk.annotations.Marker
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.plugins.places.autocomplete.PlaceAutocomplete
import com.mapbox.mapboxsdk.plugins.places.autocomplete.model.PlaceOptions
import com.mapbox.mapboxsdk.style.layers.LineLayer
import com.mapbox.mapboxsdk.style.layers.Property
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.layers.SymbolLayer
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.mapbox.mapboxsdk.utils.BitmapUtils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class Map : AppCompatActivity(), OnMapReadyCallback,
    Callback<DirectionsResponse?>, PermissionsListener {
    private lateinit var mapView: MapView
    private lateinit var mapboxMap: MapboxMap
    private lateinit var permissionsManager: PermissionsManager
    private lateinit var db: FirebaseFirestore
    private lateinit var fabUserLocation: View
    private lateinit var fabLocationSearch: View
    private lateinit var tvDistance: TextView
    private lateinit var tvS: TextView
    private lateinit var tvD: TextView
    private lateinit var stopId: MutableList<String>
    private lateinit var stops: MutableList<Point>
    private val geojsonSourceLayerId = "geojsonSourceLayerId"
    private val symbolIconId = "symbolIconId"
    private var origin: Point = Point.fromLngLat(90.399452, 23.777176)
    private var destination: Point = Point.fromLngLat(90.399452, 23.777176)
    private var distance = 0.0
    private var st: String? = null
    private var bus: Marker? = null
    private lateinit var busLoc: LatLng
    private lateinit var firestore: FirebaseFirestore
    private var previousLocation: LatLng? = null
    private var currentLocation: LatLng? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(
            this,
            resources.getString(R.string.accessToken)
        )
        setContentView(R.layout.activity_map)
        init()
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        this.mapboxMap = mapboxMap
        mapboxMap.setStyle(
            Style.MAPBOX_STREETS
        ) { style ->
            enableLocationComponent(style)
            initSearchFab()
            moveToUserLoc()
            getBusLoc()

            val drawable = ResourcesCompat.getDrawable(
                resources, R.drawable.ic_baseline_location_on_24, null
            )
            val mBitmap = BitmapUtils.getBitmapFromDrawable(drawable)
            // Add the symbol layer icon to map for future use
            style.addImage(symbolIconId, mBitmap!!)

            initSource(style)
            initLayers(style)

        }
    }

    private fun init(){
        mapView = findViewById<View>(R.id.mapView) as MapView
        fabUserLocation = findViewById(R.id.fabUserLocation)
        fabLocationSearch = findViewById(R.id.fabLocationSearch)
        tvDistance = findViewById(R.id.distanceView)
        tvS = findViewById(R.id.tvS)
        tvD = findViewById(R.id.tvD)
        db = FirebaseFirestore.getInstance()
        stopId = mutableListOf()
        stops = mutableListOf()
        firestore = FirebaseFirestore.getInstance()
    }


    private fun moveToUserLoc() {
        fabUserLocation.setOnClickListener {
            val lastLocation = mapboxMap.locationComponent.lastKnownLocation
            if (lastLocation != null) {
                val position: CameraPosition = CameraPosition.Builder()
                    .target(LatLng(lastLocation.latitude, lastLocation.longitude))
                    .zoom(14.0)
                    .tilt(13.0)
                    .build()
                mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(position), 1000)
            }
        }
    }

    private fun getBusLoc(){
        firestore.collection("userLocations").document("89")
            .addSnapshotListener{ snapshot, exception ->
                if (exception != null) {
                    Log.e("Firestore", "Listen failed", exception)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val lat = snapshot.getDouble("latitude")
                    val long = snapshot.getDouble("longitude")
                    val status = snapshot.getString("status")

                    if(lat != null && long != null){
                        busLoc = LatLng(lat, long)
                        updateMarkerPosition(busLoc, status)
                        Log.i("my_tag", "lat: $lat long: $long")
                    }else {
                        Log.e("Firestore", "One or both fields are missing")
                    }
                } else {
                    Log.d("Firestore", "Current data: null")
                }
            }
    }

    private fun updateMarkerPosition(location: LatLng, status: String?) {
        if(bus == null){
            val busPosDrawable = ResourcesCompat.getDrawable(resources, R.drawable.bus_position_symbol, null)
            val busPosIcon = IconFactory.getInstance(this).fromBitmap(BitmapUtils.getBitmapFromDrawable(busPosDrawable)!!)
            bus = mapboxMap.addMarker(
                MarkerOptions()
                    .position(location)
                    .icon(busPosIcon)
                    .title("BUS")
            )

        } else {
            if(status == "inactive"){
                hideBusMarker()
                return
            }
            updateMarkerPositionAnimation(location)
        }
    }

    private fun hideBusMarker() {
        mapboxMap.removeMarker(bus!!)

        bus = null
    }


    private fun updateMarkerPositionAnimation(newLocation: LatLng) {
        Log.i("my_tag", "marker update fun called")

        previousLocation = currentLocation
        currentLocation = newLocation
        Log.i("my_tag", "previous location: $previousLocation")
        Log.i("my_tag", "current location: $currentLocation")

        previousLocation?.let { prevLocation ->
            val interpolator = LinearInterpolator()
            val valueAnimator = ValueAnimator.ofFloat(0f, 1f)
            valueAnimator.duration = 1000 // Animation duration in milliseconds
            valueAnimator.addUpdateListener { animator ->
                val fraction = animator.animatedFraction
                val lat = (currentLocation!!.latitude - prevLocation.latitude) * fraction + prevLocation.latitude
                val lng = (currentLocation!!.longitude - prevLocation.longitude) * fraction + prevLocation.longitude
                val animatedPosition = LatLng(lat, lng)

                bus!!.position = animatedPosition
                mapboxMap.updateMarker(bus!!)

            }
            valueAnimator.interpolator = interpolator
            valueAnimator.start()
        }
    }

    private fun initLayers(loadedMapStyle: Style) {
        val routeLayer = LineLayer(ROUTE_LAYER_ID, ROUTE_SOURCE_ID)

        // Add the LineLayer to the map. This layer will display the directions route.
        routeLayer.setProperties(
            PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
            PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
            PropertyFactory.lineWidth(5f),
            PropertyFactory.lineColor(Color.parseColor("#009688"))
        )
        loadedMapStyle.addLayer(routeLayer)

        // Add the red marker icon image to the map
        loadedMapStyle.addImage(
            RED_PIN_ICON_ID, BitmapUtils.getBitmapFromDrawable(
                resources.getDrawable(R.drawable.marker)
            )!!
        )

        // Add the red marker icon SymbolLayer to the map
        loadedMapStyle.addLayer(
            SymbolLayer(ICON_LAYER_ID, ICON_SOURCE_ID).withProperties(
                PropertyFactory.iconImage(RED_PIN_ICON_ID),
                PropertyFactory.iconIgnorePlacement(true),
                PropertyFactory.iconAllowOverlap(true),
                PropertyFactory.iconOffset(arrayOf(0f, -9f))
            )
        )
    }

    private fun initSource(loadedMapStyle: Style) {
        loadedMapStyle.addSource(GeoJsonSource(ROUTE_SOURCE_ID))
        val iconGeoJsonSource = GeoJsonSource(
            ICON_SOURCE_ID, FeatureCollection.fromFeatures(
                arrayOf(
                    Feature.fromGeometry(Point.fromLngLat(origin.longitude(), origin.latitude())),
                    Feature.fromGeometry(
                        Point.fromLngLat(
                            destination.longitude(),
                            destination.latitude()
                        )
                    )
                )
            )
        )
        loadedMapStyle.addSource(iconGeoJsonSource)
    }

    override fun onResponse(
        call: Call<DirectionsResponse?>,
        response: Response<DirectionsResponse?>
    ) {
        // You can get the generic HTTP info about the response
        if (response.body() == null) {
            Toast.makeText(
                this@Map,
                "No routes found, make sure to set right user and access token",
                Toast.LENGTH_LONG
            ).show()
            return
        } else if (response.body()!!.routes().size < 1) {
            Toast.makeText(this@Map, "NO routes found", Toast.LENGTH_LONG).show()
            return
        }


        // Get the directions route
        val currentRoute = response.body()!!.routes()[0]
        distance = currentRoute.distance() / 1000
        st = String.format("%.2f K.M", distance)
        tvDistance.text = st

        mapboxMap.getStyle { style -> // Retrieve and update the source designated for showing the directions route
            val source = style.getSourceAs<GeoJsonSource>(ROUTE_SOURCE_ID)

            // Create a LineString with the directions route's geometry and
            // reset the GeoJSON source for the route LineLayer source
            source?.setGeoJson(
                LineString.fromPolyline(
                    currentRoute.geometry()!!,
                    Constants.PRECISION_6
                )
            )
        }
    }

    override fun onFailure(call: Call<DirectionsResponse?>, throwable: Throwable) {}

    private fun initSearchFab() {
        fabLocationSearch.setOnClickListener {
            val intent: Intent = PlaceAutocomplete.IntentBuilder()
                .placeOptions(
                    PlaceOptions.builder()
                        .limit(10)
                        .build(PlaceOptions.MODE_CARDS)
                )
                .accessToken(resources.getString(R.string.accessToken))
                .build(this@Map)

            startActivityForResult(intent, REQUEST_CODE_AUTOCOMPLETE)
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == REQUEST_CODE_AUTOCOMPLETE) {

            // Retrieve selected location's CarmenFeature
            val selectedCarmenFeature: CarmenFeature = PlaceAutocomplete.getPlace(data)

            // Create a new FeatureCollection and add a new Feature to it using selectedCarmenFeature above.
            // Then retrieve and update the source designated for showing a selected location's symbol layer icon
            val style = mapboxMap.style
            if (style != null) {
                val source = style.getSourceAs<GeoJsonSource>(geojsonSourceLayerId)

                source?.setGeoJson(
                    FeatureCollection.fromFeatures(
                        arrayOf(
                            Feature.fromJson(
                                selectedCarmenFeature.toJson()
                            )
                        )
                    )
                )

                // Move map camera to the selected location
                mapboxMap.animateCamera(
                    CameraUpdateFactory.newCameraPosition(
                        CameraPosition.Builder()
                            .target(
                                LatLng(
                                    (selectedCarmenFeature.geometry() as Point?)!!.latitude(),
                                    (selectedCarmenFeature.geometry() as Point?)!!.longitude()
                                )
                            )
                            .zoom(14.0)
                            .build()
                    ), 4000
                )
            }
        }
    }

    private fun enableLocationComponent(loadedMapStyle: Style) {
        // Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(this@Map)) {

            // Get an instance of the component
            val locationComponent = mapboxMap.locationComponent

            // Activate with options
            locationComponent.activateLocationComponent(
                LocationComponentActivationOptions.builder(this@Map, loadedMapStyle)
                    .build()
            )

            // Enable to make component visible
            if (ActivityCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this,Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return
            }
            locationComponent.isLocationComponentEnabled = true

            // Set the component's camera mode
//            locationComponent.cameraMode = CameraMode.TRACKING

            // Set the component's render mode
            locationComponent.renderMode = RenderMode.COMPASS
        } else {
            permissionsManager = PermissionsManager(this)
            permissionsManager.requestLocationPermissions(this)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onExplanationNeeded(permissionsToExplain: List<String>) {}

    override fun onPermissionResult(granted: Boolean) {
        if (granted) {
            mapboxMap.getStyle { style -> enableLocationComponent(style) }
        } else {
            finish()
        }
    }

    // Add the mapView lifecycle to the activity's lifecycle methods
    public override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    public override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }

    companion object {
        private const val REQUEST_CODE_AUTOCOMPLETE = 1
        private const val ROUTE_LAYER_ID = "route-layer-id"
        private const val ROUTE_SOURCE_ID = "route-source-id"
        private const val ICON_LAYER_ID = "icon-layer-id"
        private const val ICON_SOURCE_ID = "icon-source-id"
        private const val RED_PIN_ICON_ID = "red-pin-icon-id"
    }
    data class Bus(val id: String, val lat: Double, val lng: Double)

}

