package com.example.passengerapp

import android.content.Intent
import android.os.Bundle
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class BuyTicket : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var sourceDestAutoComplete: AutoCompleteTextView
    private lateinit var endDestAutoComplete: AutoCompleteTextView
    private lateinit var bookTicket: Button


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ticket_booking)

        val stopCollectionRef = db.collection("Stop")
        //val inProcessCollectionRef = db.collection("inProcess")
        val inProcessTicketsCollectionRef = db.collection("inProcessTickets")

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
                val distance = calculateDistance(source, destination)
                val amount = calculateAmount(distance)

                // Check if stops are present
                val areStopsPresent = checkStopsPresence(source, destination, stopCollectionRef)

                if (areStopsPresent) {
                    // Save data to "inProcessTickets" collection
                    saveToInProcessTicketsCollection(source, destination, amount, inProcessTicketsCollectionRef)

                    Toast.makeText(this, "Ticket details sent to payment!", Toast.LENGTH_SHORT).show()
//                  Log.d("Shitsnotreal","")
                    // Launch ActivityPayment with details
                    launchActivityPayment(source, destination, amount)
                } else {
                    // Handle case where stops are not present
                    Toast.makeText(this, "Source or destination not found in stops!", Toast.LENGTH_SHORT).show()
                }
            } else {
                // Show an error message or handle empty inputs
                Toast.makeText(this, "Source and destination cannot be empty!", Toast.LENGTH_SHORT).show()
            }
        }
    }


    // Calculate distance (simplified)
    private fun calculateDistance(source: String, destination: String): Double {
        // Implement your logic to calculate distance (for example, using maps API)
        // This is a placeholder, you need to replace it with actual distance calculation
        return 10.0
    }

    // Calculate amount based on distance
    private fun calculateAmount(distance: Double): Double {
        val baseFare = 10.0
        val perKmRate = 1.5
        return baseFare + (perKmRate * (distance - 2.0))
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
}
