package com.example.passengerapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import com.example.passengerapp.student.StudentVerf
import com.google.firebase.auth.FirebaseAuth

class Home : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var btnSignOut: ImageButton
    private lateinit var btnDisplayMap: ImageButton
    private lateinit var btnAddStop: ImageButton
    private lateinit var btnViewStop: ImageButton
    private lateinit var btnStuPass: ImageButton
    private lateinit var btnBuyTicket: ImageButton

    private fun init(){
        auth = FirebaseAuth.getInstance()
        btnSignOut = findViewById(R.id.btnSignOut_home)
        btnDisplayMap = findViewById(R.id.btnDisplayMap_home)
        btnAddStop = findViewById(R.id.btnAddStop_home)
        btnViewStop = findViewById(R.id.btnViewStop_home)
        btnStuPass = findViewById(R.id.btnSV_home)
        btnBuyTicket = findViewById(R.id.btnBuyTicket_home)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        init()

        btnSignOut.setOnClickListener{
            auth.signOut()
            startActivity(Intent(this@Home, Login::class.java))
        }

        btnDisplayMap.setOnClickListener{
            startActivity(Intent(this@Home, Map::class.java))
        }

        btnAddStop.setOnClickListener{
            startActivity(Intent(this@Home, AddStops::class.java))
        }

        btnViewStop.setOnClickListener{
            startActivity(Intent(this@Home, ViewStops::class.java))
        }

        btnStuPass.setOnClickListener{
            startActivity(Intent(this@Home, StudentVerf::class.java))
        }

        btnBuyTicket.setOnClickListener{
            startActivity(Intent(this@Home, StudentVerf::class.java))
        }



    }

}