package com.example.passengerapp

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Button
import android.widget.GridView
import android.widget.ImageView
import android.widget.SimpleAdapter
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
import kotlin.collections.Map

class ActivityPayment : AppCompatActivity(), PaymentResultWithDataListener, DialogInterface.OnClickListener {


    private lateinit var gv1: GridView
    private lateinit var details: Map<String, String>
    private lateinit var btnPay: Button
    private lateinit var qrCodeImageView: ImageView
    private var source: String? = null
    private lateinit var QR: ImageView
    private var distance: Float? = null
    private var destination: String? = null
    private var fare: Double = 0.0
    private var ticketId: String = ""

    private val TAG: String = ActivityPayment::class.java.simpleName
    private lateinit var alertDialogBuilder: AlertDialog.Builder

    private val db = FirebaseFirestore.getInstance()
    private val validTicketsCollection: CollectionReference = db.collection("validTickets")


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment)

        gv1 = findViewById(R.id.gridView)
        btnPay = findViewById(R.id.btnPay)
        QR = findViewById(R.id.ivQR)

        alertDialogBuilder = AlertDialog.Builder(this@ActivityPayment)
        alertDialogBuilder.setTitle("Payment Result")
        alertDialogBuilder.setCancelable(true)
        alertDialogBuilder.setPositiveButton("Ok", this)

        // Get details from the intent
        source = intent.getStringExtra("source")
        destination = intent.getStringExtra("destination")
        fare = intent.getIntExtra("fare", 0).toDouble()
        ticketId = UUID.randomUUID().toString()
        distance = intent.getFloatExtra("dist", 0.0f)

        // Display details
//        tvTicketDetails.text = "Ticket Details"

        details = mapOf(
            "Source" to "$source",
            "Destination" to "$destination",
            "Time Of Purchase" to "${getCurrentDateTime()}",
            "Ticket ID" to "$ticketId",
            "Distance" to String.format("%.2f KM", distance),
            "Fare" to "RS $fare"
        )

        val detailEntries = details.map { entry ->
            mapOf(
                "key" to entry.key,
                "value" to entry.value
            )
        }

        val adapter = SimpleAdapter(
            this,
            detailEntries,
            R.layout.cell,
            arrayOf("key", "value"),
            intArrayOf(R.id.textKey, R.id.textValue)
        )

        gv1.adapter = adapter

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
            saveToValidTicketsCollection(p0)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onPaymentError(p0: Int, p1: String?, p2: PaymentData?) {
        try {
            alertDialogBuilder.setMessage("Payment Successful : Ticket Purchased ")
            alertDialogBuilder.show()
            saveToValidTicketsCollection(p1)//remove this
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
    private fun saveToValidTicketsCollection(paymentId: String?) {

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
                generateQRCode()
            }
    }

    private fun generateQRCode() {
        if (ticketId != null) {
            val qrCode = QRCode()
            val bitmap: Bitmap = qrCode.encode("$ticketId")

            QR.setImageBitmap(bitmap)
        }
    }

}
