package com.example.passengerapp

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.razorpay.Checkout
import com.razorpay.PaymentData
import com.razorpay.PaymentResultWithDataListener
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

class ActivityPayment : AppCompatActivity(), PaymentResultWithDataListener, DialogInterface.OnClickListener {

    private lateinit var tvTicketDetails: TextView
    private lateinit var tvPrice: TextView
    private lateinit var tvSource: TextView
    private lateinit var tvDestination: TextView
    private lateinit var tvTimeOfPurchase: TextView
    private lateinit var btnPay: Button
    private lateinit var qrCodeImageView: ImageView
    private var source: String? = null
    private var destination: String? = null
    private var fare: Double = 0.0

    private val TAG: String = ActivityPayment::class.java.simpleName
    private lateinit var alertDialogBuilder: AlertDialog.Builder

    private val db = FirebaseFirestore.getInstance()
    private val validTicketsCollection: CollectionReference = db.collection("validTickets")


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment)

        tvTicketDetails = findViewById(R.id.tvTicketDetails)
        tvPrice = findViewById(R.id.tvPrice)
        tvSource = findViewById(R.id.tvSource)
        tvDestination = findViewById(R.id.tvDestination)
        tvTimeOfPurchase = findViewById(R.id.tvTimeOfPurchase)
        btnPay = findViewById(R.id.btnPay)

        alertDialogBuilder = AlertDialog.Builder(this@ActivityPayment)
        alertDialogBuilder.setTitle("Payment Result")
        alertDialogBuilder.setCancelable(true)
        alertDialogBuilder.setPositiveButton("Ok", this)

        // Get details from the intent
        source = intent.getStringExtra("source")
        destination = intent.getStringExtra("destination")
        fare = intent.getIntExtra("fare", 0).toDouble()

        // Display details
        tvTicketDetails.text = "Ticket Details"

        tvPrice.text = "Price: ₹$fare"
        tvSource.text = "Source: $source"
        tvDestination.text = "Destination: $destination"
        tvTimeOfPurchase.text = "Time of Purchase: ${getCurrentDateTime()}"

        // Set up click listener for the "Pay Now" button
        btnPay.setOnClickListener {
            startRazorpayPayment(fare)
        }
    }

    private fun startRazorpayPayment(amount: Double) {
        val checkout = Checkout()
        checkout.setKeyID("LY5XuZrr1qs6ww1WrJR5EShd") // Replace with your actual Razorpay API key

        val options = JSONObject()
        options.put("name", "Razorpay Corp")
        options.put("description", "Demoing Charges")
        options.put("image", "https://s3.amazonaws.com/rzp-mobile/images/rzp.png")
        options.put("currency", "INR")
        options.put("amount", (amount * 100).toInt())

        val prefill = JSONObject()
        prefill.put("email", "test@razorpay.com")
        prefill.put("contact", "9021066696")

        options.put("prefill", prefill)

        checkout.open(this, options)
    }

    override fun onPaymentSuccess(p0: String?, p1: PaymentData?) {
        try {
            alertDialogBuilder.setMessage("Payment Successful : Payment ID: $p0\nPayment Data: ${p1?.data}")
            alertDialogBuilder.show()
            saveToValidTicketsCollection(p0, p1)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onPaymentError(p0: Int, p1: String?, p2: PaymentData?) {
        try {
            alertDialogBuilder.setMessage("Payment Successful : Ticket Purchased ")
            alertDialogBuilder.show()
            saveToValidTicketsCollection(p1, p2)//remove this
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun onExternalWalletSelected(p0: String?, p1: PaymentData?) {
        try {
            alertDialogBuilder.setMessage("External wallet was selected : Payment Data: ${p1?.data}")
            alertDialogBuilder.show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onClick(dialog: DialogInterface?, which: Int) {
    }

    private fun getCurrentDateTime(): String {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("dd-MMM-yyyy hh:mm a", Locale.getDefault())
        return dateFormat.format(calendar.time)
    }
    private fun saveToValidTicketsCollection(paymentId: String?, paymentData: PaymentData?) {
        val ticketId = UUID.randomUUID().toString()
        val ticketDetails = hashMapOf(
            "ticketId" to ticketId,
            "source" to source,
            "destination" to destination,
            "fare" to fare,
            "timeOfPurchase" to getCurrentDateTime(),
            "paymentId" to paymentId
        )

        // Save data to "validTickets" collection
        validTicketsCollection.add(ticketDetails)
            .addOnSuccessListener {
                val intent = Intent(this, ActivityTicketOutputView::class.java)
                intent.putExtra("ticketId", ticketId)
                startActivity(intent)
            }
            .addOnFailureListener { e ->
                // Handle failure
            }
    }

}
