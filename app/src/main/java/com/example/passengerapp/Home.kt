package com.example.passengerapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import com.example.passengerapp.student.StudentVerf
import com.google.firebase.auth.FirebaseAuth

class Home : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var btnSignOut: Button
    private lateinit var btnDisplayMap: Button
    private lateinit var btnAddStop: Button
    private lateinit var btnViewStop: Button
    private lateinit var btnStuPass: Button

    private fun init(){
        auth = FirebaseAuth.getInstance()
        btnSignOut = findViewById(R.id.btnSignOut)
        btnDisplayMap = findViewById(R.id.btnDisplayMap)
        btnAddStop = findViewById(R.id.btnAddStop)
        btnViewStop = findViewById(R.id.btnViewStop)
        btnStuPass = findViewById(R.id.btnSV)
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
            val intent = Intent(this@Home, Map::class.java)
            startActivity(intent)
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


    }

}