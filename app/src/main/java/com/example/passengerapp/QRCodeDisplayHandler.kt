package com.example.passengerapp

import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class ActivityTicketOutputView : AppCompatActivity() {

    private lateinit var qrCodeImageView: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ticket_output_view)
        qrCodeImageView = findViewById(R.id.qrCodeImageView)
        val intent = intent
        if (intent != null && intent.hasExtra("ticketId")) {
            val ticketId = intent.getStringExtra("ticketId")
            Log.d("shownQRQRQR","QRSHOWNBOYOnonon")
            generateQRCode(ticketId)
        }
        Log.d("shownQRQRQR","QRSHOWNBOYO")

    }

    private fun generateQRCode(ticketId: String?) {
        if (ticketId != null) {
            val qrCode = QRCode()
            val bitmap: Bitmap? = qrCode.encode("$ticketId")
            Log.d("shownQRQRQR","theshitwasshownQRSHOWNBOYO {$ticketId}")

            if (bitmap != null) {
                Log.d("shownQRQRQR","theshitwasshownQRSHOWNBOYO{$ticketId}\"")

                // Set the generated QR code to the ImageView
                qrCodeImageView.setImageBitmap(bitmap)
            }
        }
    }
}
