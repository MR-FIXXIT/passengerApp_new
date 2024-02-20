package com.example.passengerapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.res.ResourcesCompat
import com.google.firebase.firestore.FirebaseFirestore
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.MapboxDirections
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.geocoding.v5.models.CarmenFeature
import com.mapbox.core.constants.Constants
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.LocationComponent
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.plugins.places.autocomplete.PlaceAutocomplete
import com.mapbox.mapboxsdk.style.layers.LineLayer
import com.mapbox.mapboxsdk.style.layers.Property
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.layers.SymbolLayer
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.mapbox.mapboxsdk.utils.BitmapUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.TreeMap

class RouteMap : AppCompatActivity(), OnMapReadyCallback,
    Callback<DirectionsResponse?>, PermissionsListener {
    private lateinit var mapView: MapView
    var mapboxMap: MapboxMap? = null
    private var permissionsManager: PermissionsManager? = null
    private val locationComponent: LocationComponent? = null
    private var home: CarmenFeature? = null
    private var work: CarmenFeature? = null
    private val geojsonSourceLayerId = "geojsonSourceLayerId"
    private val symbolIconId = "symbolIconId"
    var address: String? = null
    var origin = Point.fromLngLat(75.783165, 11.279197)
    var destination = Point.fromLngLat(75.837528, 11.270646)
    private var client: MapboxDirections? = null
    var c = 0
    var distance = 0.0
    var st: String? = null
    var startLocation: String? = ""
    var endLocation: String? = ""
    private lateinit var firestore: FirebaseFirestore
    private lateinit var stops: MutableList<Point>
    private lateinit var btnDisplayRoute: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        Mapbox.getInstance(
            this,
            resources.getString(R.string.accessToken)
        )
        setContentView(R.layout.activity_route_map)
        init()
        mapView = findViewById<View>(R.id.mapView) as MapView
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)
    }

    private fun init(){
        firestore = FirebaseFirestore.getInstance()
        btnDisplayRoute = findViewById(R.id.btnDisplayRoute)
        stops = mutableListOf()
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        this.mapboxMap = mapboxMap
        mapboxMap.setStyle(
            Style.MAPBOX_STREETS
        ) { style ->
            enableLocationComponent(style)
            val drawable = ResourcesCompat.getDrawable(
                resources, R.drawable.ic_baseline_location_on_24, null
            )
            val mBitmap = BitmapUtils.getBitmapFromDrawable(drawable)
            // Add the symbol layer icon to map for future use
            style.addImage(symbolIconId, mBitmap!!)

            // Create an empty GeoJSON source using the empty feature collection
            setUpSource(style)

            // Set up a new symbol layer for displaying the searched location's feature coordinates
            setupLayer(style)
            initSource(style)
            initLayers(style)


            getRou()


//            getRoute(stops)
        }
    }

    private fun getRou() {
        btnDisplayRoute.setOnClickListener {

            getStopIDFromFirestore()

        }
    }

    private fun getStopIDFromFirestore() {
        firestore.collection("Route")
            .document("wc97kYQg4zhxXIdyUobk")
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val dataMap = document.data
                    if (dataMap != null) {
                        val stopIdsMap = mutableMapOf<Int, String>()
                        // Iterate over keys and add stop IDs to the map with their corresponding number
                        for ((key, value) in dataMap) {
                            if (key.startsWith("stop")) {
                                val stopId = value as? String
                                val stopNumber = key.substring(4).toInt()
                                if (stopId != null) {
                                    stopIdsMap[stopNumber] = stopId
                                }
                            }
                        }

                        // Sort the map by keys
                        val sortedStopIds = stopIdsMap.toSortedMap().values.toList()

                        fetchStopCoordinates(sortedStopIds)
                        Log.d("my_tag", "$sortedStopIds")



                    }
                } else {
                    Log.d("my_tag", "Document does not exist")
                }
            }
    }



    private fun fetchStopCoordinates(stopIds: List<String>) {
        val stopCoordinatesFetched = LinkedHashMap<String, Point>()
        // Iterate over stopIds and fetch corresponding coordinates from Firestore
        for (stopId in stopIds) {
            firestore.collection("Stop")
                .document(stopId)
                .get()
                .addOnSuccessListener { stopDocument ->
                    if (stopDocument != null && stopDocument.exists()) {
                        val lat = stopDocument.getString("lat")
                        val lng = stopDocument.getString("long")
                        val point = Point.fromLngLat(lng!!.toDouble(), lat!!.toDouble())
                        stopCoordinatesFetched[stopId] = point
                        // Check if all stop coordinates have been fetched
                        if (stopCoordinatesFetched.size == stopIds.size) {
                            // All stop coordinates fetched, now call getRoute using coroutine
                            CoroutineScope(Dispatchers.Main).launch {
                                delay(3000) // Delay for 3 seconds
                                Log.i("my_tag", "$stopCoordinatesFetched")
                                val orderedPoints = mutableListOf<Point>()

                                for (stopId in stopIds) {
                                    val point = stopCoordinatesFetched[stopId]
                                    if (point != null) {
                                        orderedPoints.add(point)
                                    }
                                }
                                Log.d("my_tag", "$orderedPoints")

                                getRoute(orderedPoints)
                            }
                        }
                    } else {
                        Log.d("my_tag", "Stop document not found for ID: $stopId")
                    }
                }
                .addOnFailureListener { exception ->
                    Log.d("my_tag", "Error getting stop document for ID: $stopId", exception)
                }
        }
    }






    private fun initLayers(loadedMapStyle: Style) {
        val routeLayer = LineLayer(ROUTE_LAYER_ID, ROUTE_SOURCE_ID)

        routeLayer.setProperties(
            PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
            PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
            PropertyFactory.lineWidth(5f),
            PropertyFactory.lineColor(Color.parseColor("#009688"))
        )
        loadedMapStyle.addLayer(routeLayer)

        loadedMapStyle.addImage(
            RED_PIN_ICON_ID, BitmapUtils.getBitmapFromDrawable(
                resources.getDrawable(R.drawable.marker)
            )!!
        )

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
                    Feature.fromGeometry(
                        Point.fromLngLat(
                            origin.longitude(),
                            origin.latitude()
                        )),
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

    private fun getRoute(stops: List<Point>) {
        if (stops.size >= 3) {
            val origin = stops.first()
            val destination = stops.last()
            val waypoints = mutableListOf<Point>()
            for (i in 1 until stops.size - 1) {
                waypoints.add(stops[i])
            }

            client = MapboxDirections.builder()
                .origin(origin)
                .destination(destination)
                .waypoints(waypoints)
                .overview(DirectionsCriteria.OVERVIEW_FULL)
                .profile(DirectionsCriteria.PROFILE_DRIVING)
                .accessToken(resources.getString(R.string.accessToken))
                .build()

            client!!.enqueueCall(this)
        } else {
            // Handle case when there are not enough stops to construct a route
            Toast.makeText(this, "At least 3 stops are required to construct a route", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResponse(
        call: Call<DirectionsResponse?>,
        response: Response<DirectionsResponse?>
    ) {
// You can get the generic HTTP info about the response
        if (response.body() == null) {
            Toast.makeText(
                this,
                "NO routes found make sure to set right user and access token",
                Toast.LENGTH_LONG
            ).show()
            return
        } else if (response.body()!!.routes().size < 1) {
            Toast.makeText(this, "NO routes found", Toast.LENGTH_LONG).show()
        }


// Get the directions route
        val currentRoute = response.body()!!.routes()[0]
        // Toast.makeText(MainActivity.this,currentRoute.distance()+" metres ",Toast.LENGTH_SHORT).show();
        distance = currentRoute.distance() / 1000
        st = String.format("%.2f K.M", distance)
        val dv = findViewById<TextView>(R.id.distanceView)
        dv.text = st

        if (mapboxMap != null) {
            mapboxMap!!.getStyle { style -> // Retrieve and update the source designated for showing the directions route
                val source = style.getSourceAs<GeoJsonSource>(ROUTE_SOURCE_ID)

                source?.setGeoJson(
                    LineString.fromPolyline(
                        currentRoute.geometry()!!,
                        Constants.PRECISION_6
                    )
                )
            }
        }
    }

    override fun onFailure(call: Call<DirectionsResponse?>, throwable: Throwable) {}

    fun confirmed(view: View?) {
        val i = Intent(this, Home::class.java)
        i.putExtra("total distance", distance.toString() + "")
        i.putExtra("start", "From: $startLocation")
        i.putExtra("destination", "To: $endLocation")
        startActivity(i)
    }

    private fun setUpSource(loadedMapStyle: Style) {
        loadedMapStyle.addSource(GeoJsonSource(geojsonSourceLayerId))
    }

    private fun setupLayer(loadedMapStyle: Style) {
        loadedMapStyle.addLayer(
            SymbolLayer("SYMBOL_LAYER_ID", geojsonSourceLayerId).withProperties(
                PropertyFactory.iconImage(symbolIconId),
                PropertyFactory.iconOffset(arrayOf(0f, -8f))
            )
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == REQUEST_CODE_AUTOCOMPLETE) {

            // Retrieve selected location's CarmenFeature
            val selectedCarmenFeature = PlaceAutocomplete.getPlace(data)

            // Create a new FeatureCollection and add a new Feature to it using selectedCarmenFeature above.
            // Then retrieve and update the source designated for showing a selected location's symbol layer icon
            if (mapboxMap != null) {
                val style = mapboxMap!!.style
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
                    mapboxMap!!.animateCamera(
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

    }

    private fun enableLocationComponent(loadedMapStyle: Style) {
        // Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(this)) {

            // Get an instance of the component
            val locationComponent = mapboxMap!!.locationComponent

            // Activate with options
            locationComponent.activateLocationComponent(
                LocationComponentActivationOptions.builder(this, loadedMapStyle)
                    .build()
            )

            // Enable to make component visible
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
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
            locationComponent.cameraMode = CameraMode.TRACKING

            // Set the component's render mode
            locationComponent.renderMode = RenderMode.COMPASS
        } else {
            permissionsManager = PermissionsManager(this)
            permissionsManager!!.requestLocationPermissions(this)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionsManager!!.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onExplanationNeeded(permissionsToExplain: List<String>) {}

    override fun onPermissionResult(granted: Boolean) {
        if (granted) {
            mapboxMap!!.getStyle { style -> enableLocationComponent(style) }
        } else {
            //Toast.makeText(this, R.string.user_location_permission_not_granted, Toast.LENGTH_LONG).show();
            finish()
        }
    }

    // Add the mapView lifecycle to the activity's lifecycle methods
    public override fun onResume() {
        super.onResume()
        mapView!!.onResume()
    }

    override fun onStart() {
        super.onStart()
        mapView!!.onStart()
    }

    override fun onStop() {
        super.onStop()
        mapView!!.onStop()
    }

    public override fun onPause() {
        super.onPause()
        mapView!!.onPause()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView!!.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView!!.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView!!.onSaveInstanceState(outState)
    }

    companion object {
        private const val REQUEST_CODE_AUTOCOMPLETE = 1
        private const val REQUEST_CODE = 5678
        private const val ROUTE_LAYER_ID = "route-layer-id"
        private const val ROUTE_SOURCE_ID = "route-source-id"
        private const val ICON_LAYER_ID = "icon-layer-id"
        private const val ICON_SOURCE_ID = "icon-source-id"
        private const val RED_PIN_ICON_ID = "red-pin-icon-id"
    }
}