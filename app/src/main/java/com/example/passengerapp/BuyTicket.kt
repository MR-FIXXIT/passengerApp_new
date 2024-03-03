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
import kotlinx.coroutines.tasks.await
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

import java.util.*
import kotlin.math.ceil

class BuyTicket : AppCompatActivity(){

    private val firestore = FirebaseFirestore.getInstance()
    private lateinit var sourceDestAutoComplete: AutoCompleteTextView
    private lateinit var endDestAutoComplete: AutoCompleteTextView
    private lateinit var bookTicket: Button
    private lateinit var sourceID: String
    private lateinit var destinationID: String
    private var distance: Float = 0.0F

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ticket_booking)

        init()

        val stopCollectionRef = firestore.collection("Stop")
        val inProcessTicketsCollectionRef = firestore.collection("inProcessTickets")


        // Set up click listener for the "Book Ticket" button
        bookTicket.setOnClickListener {
            val source = sourceDestAutoComplete.text.toString()
            val destination = endDestAutoComplete.text.toString()

            // Validate that source and destination are not empty
            if (source.isNotEmpty() && destination.isNotEmpty()) {
                CoroutineScope(Dispatchers.Main).launch {
                    val routeId = generateRouteId(source, destination)
                    getRouteDistance(routeId){
                        val fare = calculateRouteFare()
                        launchActivityPayment(source, destination, fare)
                    }
                }
            } else {
                Toast.makeText(this, "Source and destination cannot be empty!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun init(){
        sourceDestAutoComplete = findViewById(R.id.sourceDestAutoComplete)
        endDestAutoComplete = findViewById(R.id.endDestAutoComplete)
        bookTicket = findViewById(R.id.bookTicket)
    }

    private fun launchActivityPayment(source: String, destination: String, fare: Int) {
        val intent = Intent(this, ActivityPayment::class.java)
        intent.putExtra("source", source)
        intent.putExtra("destination", destination)
        Log.i("my_tag", "Fare to send : $fare")
        intent.putExtra("fare", fare)

        startActivity(intent)
    }

    private fun getRouteDistance(routeID: String, callback: (Float) -> Unit) {
        val collectionName = "RouteInfo"

        firestore.collection(collectionName)
            .document(routeID)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    val routeDistance = documentSnapshot.getString("routeDistance")
                    if (routeDistance != null) {
                        distance = routeDistance.toFloat()
                        Log.i("my_tag", "Distance : $distance")
                        callback(distance)
                    } else {
                        Log.e("getRouteDistance", "routeDistance field is null")
                        callback(0f)
                    }
                } else {
                    Log.e("getRouteDistance", "Document with routeID $routeID doesn't exist")
                }
            }
            .addOnFailureListener { exception ->
                Log.e("getRouteDistance", "Error getting document", exception)
                callback(0f)
            }
    }

    private suspend fun generateRouteId(sourceStop: String, destinationStop: String): String {
        getStopId(sourceStop, destinationStop)

        return "$sourceID-$destinationID"
    }

    private suspend fun getStopId(source: String, destination: String){
        val collectionName = "Stop"
        val fieldName = "stop_name"
        val stopNames = arrayOf(source, destination)
        var documentId = ""

        for(stopName in stopNames){
            val documents = firestore.collection(collectionName)
                .whereEqualTo(fieldName, stopName)
                .get()
                .await()

            for (document in documents) {
                val documentId = document.id

                if (stopName == source) {
                    sourceID = documentId
                } else {
                    destinationID = documentId
                }
            }
        }
    }

    private fun calculateRouteFare(): Int{
        val baseFare = 10
        val baseDist = 2.5f
        val routeDist = distance
        var routeFare: Int = baseFare

        val e = (routeDist / baseDist)

        val l: Int = ceil(e).toInt()

        for(i in 1 until l){
            routeFare += if(i%2 == 0) 2 else 3
        }

        Log.i("my_tag", "Fare : $routeFare")
        return routeFare
    }

    // Check if stops are present in the "Stop" collection
    private fun checkStopsPresence(source: String, destination: String, stopCollectionRef: CollectionReference): Boolean {
        // Implement your logic to check if stops are present in the collection
        // This is a placeholder, you need to replace it with actual logic
        return true
    }

    // Save data to "inProcess" collection
    private fun saveToInProcessCollection(source: String, destination: String, amount: Double, inProcessCollectionRef: CollectionReference) {
        val data = hashMapOf(
            "source" to source,
            "destination" to destination,
            "amount" to amount,
            "dateTime" to Calendar.getInstance().time,  // Current date and time
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
        )

        inProcessTicketsCollectionRef.add(data)
            .addOnSuccessListener {
                // Handle success
            }
            .addOnFailureListener { e ->
                // Handle failure
            }
    }


}
