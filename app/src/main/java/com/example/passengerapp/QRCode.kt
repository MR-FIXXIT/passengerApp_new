package com.example.passengerapp

import android.graphics.Bitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.common.BitMatrix
import com.journeyapps.barcodescanner.BarcodeEncoder

class QRCode {
    fun encode(data: String?): Bitmap {
        val barcodeEncoder = BarcodeEncoder()

        val bitMatrix: BitMatrix = barcodeEncoder.encode(data, BarcodeFormat.QR_CODE, 200, 200)

        val bitmap: Bitmap = Bitmap.createBitmap(bitMatrix.width, bitMatrix.height, Bitmap.Config.RGB_565)

        for (x in 0 until bitMatrix.width) {
            for (y in 0 until bitMatrix.height) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt())
            }
        }

        return bitmap
    }
}