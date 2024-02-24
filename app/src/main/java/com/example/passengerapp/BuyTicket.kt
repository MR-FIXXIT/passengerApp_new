package com.example.passengerapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.MapboxDirections
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.geojson.Point
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

import java.util.*
import kotlin.math.round

class BuyTicket : AppCompatActivity(), Callback<DirectionsResponse?> {

    private val firestore = FirebaseFirestore.getInstance()
    private lateinit var sourceDestAutoComplete: AutoCompleteTextView
    private lateinit var endDestAutoComplete: AutoCompleteTextView
    private lateinit var bookTicket: Button
    private var client: MapboxDirections? = null
    private lateinit var sourceID: String
    private lateinit var destinationID: String
    private var distance: Float = 0.0F



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ticket_booking)

        val stopCollectionRef = firestore.collection("Stop")
        //val inProcessCollectionRef = db.collection("inProcess")
        val inProcessTicketsCollectionRef = firestore.collection("inProcessTickets")

        sourceDestAutoComplete = findViewById(R.id.sourceDestAutoComplete)
        endDestAutoComplete = findViewById(R.id.endDestAutoComplete)
        bookTicket = findViewById(R.id.bookTicket)

        // Set up AutoCompleteTextView adapters
        val sourceDestAdapter = AutoCompleteAdapter(this, stopCollectionRef)
        sourceDestAutoComplete.setAdapter(sourceDestAdapter)

        val endDestAdapter = AutoCompleteAdapter(this, stopCollectionRef)
        endDestAutoComplete.setAdapter(endDestAdapter)

        // Preload data for AutoCompleteAdapters
        sourceDestAdapter.preloadData()
        endDestAdapter.preloadData()

        // Set up click listener for the "Book Ticket" button
        bookTicket.setOnClickListener {
            val source = sourceDestAutoComplete.text.toString()
            val destination = endDestAutoComplete.text.toString()

            // Validate that source and destination are not empty
            if (source.isNotEmpty() && destination.isNotEmpty()) {
                // Calculate amount based on distance (simplified)
                val amount: Double = 0.0
                calculateDistance(source, destination)
                CoroutineScope(Dispatchers.Main).launch {
                    delay(3000) // Delay for 3 seconds

                    val amount = calculateAmount(distance)

                    val areStopsPresent = checkStopsPresence(source, destination, stopCollectionRef)

                    if (areStopsPresent) {
                        // Save data to "inProcessTickets" collection
                        saveToInProcessTicketsCollection(source, destination, amount, inProcessTicketsCollectionRef)

                        Toast.makeText(this@BuyTicket, "Ticket details sent to payment!", Toast.LENGTH_SHORT).show()
//                  Log.d("Shitsnotreal","")
                        // Launch ActivityPayment with details
                        launchActivityPayment(source, destination, amount)
                    } else {
                        // Handle case where stops are not present
                        Toast.makeText(this@BuyTicket, "Source or destination not found in stops!", Toast.LENGTH_SHORT).show()
                    }
                }

                // Check if stops are present

            } else {
                // Show an error message or handle empty inputs
                Toast.makeText(this, "Source and destination cannot be empty!", Toast.LENGTH_SHORT).show()
            }
        }
    }



    private fun getStopID(source: String, destination: String) {
        // Create an array of source and destination
        val places = arrayOf(source, destination)
        var t = true

        // Iterate through the array to query each stop
        for (place in places) {
            firestore.collection("Stop")
                .whereEqualTo("stop_name", place)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    for (document in querySnapshot.documents) {
                        val stopId = document.id // This is the UUID of the stop
                        // Call the callback function with the place and stopId
                        if(t){
                            sourceID = stopId
                            t = false
                        }else{
                            destinationID = stopId
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("Error", "Error getting stop document", exception)
                }
        }
    }
    private fun calculateDistance(source: String, destination: String) {

        getStopID(source, destination)

        CoroutineScope(Dispatchers.Main).launch {
            delay(2000) // Delay for 3 seconds
            fetchStopsFromFirestore(sourceID, destinationID)

        }

    }

    private fun generateRouteId(sourceStop: String, destinationStop: String): String {
        return "$sourceStop-$destinationStop"
    }

    private fun fetchStopsFromFirestore(source: String, destination: String){
        var routeId = generateRouteId(source,destination)
        Log.i("my_tag", routeId)

        firestore.collection("Route")
            .document(routeId)
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

                        val sortedStopIds = stopIdsMap.toSortedMap().values.toList()

                        fetchStopCoordinates(sortedStopIds)
                    }
                }else{
                    Toast.makeText(this, "No route exists", Toast.LENGTH_SHORT).show()
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



    // Calculate amount based on distance
    private fun calculateAmount(distance: Float): Double {
        Log.i("my_tag", "d:"+distance.toString())
        // Define the base fare for the first 2 kilometers
        val baseFare = 10.0

        // Define the additional charge per kilometer after the first 2 kilometers
        val perKmRate = 1.5

        // If the distance is less than or equal to 2 kilometers, return the base fare
        if (distance <= 2.0) {
            return baseFare
        } else {
            // Calculate the fare for distances beyond 2 kilometers
            // Fare = base fare + (additional charge per kilometer * (distance - 2))
            return baseFare + perKmRate * (distance - 2.0)
        }
    }

    // Check if stops are present in the "Stop" collection
    private fun checkStopsPresence(source: String, destination: String, stopCollectionRef: CollectionReference): Boolean {
        // Implement your logic to check if stops are present in the collection
        // This is a placeholder, you need to replace it with actual logic
        return true
    }

    // Save data to "inProcess" collection
    private fun saveToInProcessCollection(source: String, destination: String, amount: Double, inProcessCollectionRef: CollectionReference) {
        // Implement your logic to save data to "inProcess" collection
        // This is a placeholder, you need to replace it with actual logic
        val data = hashMapOf(
            "source" to source,
            "destination" to destination,
            "amount" to amount,
            "dateTime" to Calendar.getInstance().time,  // Current date and time
            // Add other fields as needed
        )

        inProcessCollectionRef.add(data)
            .addOnSuccessListener {
                // Handle success
            }
            .addOnFailureListener { e ->
                // Handle failure
            }
    }

    // Save data to "inProcessTickets" collection
    private fun saveToInProcessTicketsCollection(source: String, destination: String, amount: Double, inProcessTicketsCollectionRef: CollectionReference) {
        // Implement your logic to save data to "inProcessTickets" collection
        // This is a placeholder, you need to replace it with actual logic
        val data = hashMapOf(
            "source" to source,
            "destination" to destination,
            "amount" to amount,
            "dateTime" to Calendar.getInstance().time,  // Current date and time
            // Add other fields as needed
        )

        inProcessTicketsCollectionRef.add(data)
            .addOnSuccessListener {
                // Handle success
            }
            .addOnFailureListener { e ->
                // Handle failur§e
            }
    }

    private fun launchActivityPayment(source: String, destination: String, amount: Double) {
        val intent = Intent(this, ActivityPayment::class.java)
        intent.putExtra("source", source)
        intent.putExtra("destination", destination)
        intent.putExtra("amount", amount)
        startActivity(intent)
    }

    override fun onResponse(
        call: Call<DirectionsResponse?>,
        response: Response<DirectionsResponse?>
    ) {
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
        val st =  String.format("%.2f", currentRoute.distance() / 1000)
        distance = st.toFloat()
        Log.i("my_tag", distance.toString())


    }

    override fun onFailure(call: Call<DirectionsResponse?>, t: Throwable) {}
}
