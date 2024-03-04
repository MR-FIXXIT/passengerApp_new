package com.example.passengerapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

import java.util.*
import kotlin.math.ceil

class BuyTicket : AppCompatActivity(){

    private val VIEW_STOPS_REQUEST_CODE: Int = 1
    private val firestore = FirebaseFirestore.getInstance()
    private lateinit var tvSource: TextView
    private lateinit var tvDest: TextView
    private lateinit var bookTicket: Button
    private lateinit var sourceID: String
    private lateinit var destinationID: String
    private var distance: Float = 0.0F
    private lateinit var ibSwitch: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ticket_booking)

        init()

        tvSource.setOnClickListener {
            selectStop("source")
        }

        tvDest.setOnClickListener {
            selectStop("dest")
        }

        ibSwitch.setOnClickListener {
            val temp = tvSource.text
            tvSource.text = tvDest.text
            tvDest.text = temp
        }

        bookTicket.setOnClickListener {
            val source = tvSource.text.toString()
            val destination = tvDest.text.toString()

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

    private fun selectStop(tag: String){
        val intent = Intent(this, ViewStops::class.java)
        intent.putExtra("tvTag", tag)
        intent.putExtra("true", true)
        startActivityForResult(intent, VIEW_STOPS_REQUEST_CODE)
    }

    private fun init(){
        tvSource = findViewById(R.id.tvSource_ticketBooking)
        tvDest = findViewById(R.id.tvDest_ticketBooking)
        bookTicket = findViewById(R.id.bookTicket)
        ibSwitch = findViewById(R.id.ibSwitch_BuyTicket)
    }

    private fun launchActivityPayment(source: String, destination: String, fare: Int) {
        val intent = Intent(this, ActivityPayment::class.java)
        intent.putExtra("source", source)
        intent.putExtra("destination", destination)
        Log.i("my_tag", "Fare to send : $fare")
        intent.putExtra("fare", fare)
        intent.putExtra("dist", distance)

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
                    Toast.makeText(this, "Route does not exist in DataBase", Toast.LENGTH_LONG).show()
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == VIEW_STOPS_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val stopName = data?.getStringExtra("stop_name")
            val tag = data?.getStringExtra("tvTag")

            when(tag){
                "source" -> tvSource.text = stopName
                "dest" -> tvDest.text = stopName
            }
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
}
