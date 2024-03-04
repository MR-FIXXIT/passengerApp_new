package com.example.passengerapp

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth


class SplashScreen : AppCompatActivity() {

    private lateinit var image: ImageView
    private lateinit var logo: TextView
    private lateinit var slogan: TextView
    private lateinit var topAnim: Animation
    private lateinit var bottomAnim: Animation
    private lateinit var handler: Handler
    private lateinit var auth: FirebaseAuth


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        init()

        topAnim = AnimationUtils.loadAnimation(this, R.anim.top_animation)
        bottomAnim = AnimationUtils.loadAnimation(this, R.anim.bottom_animation)

        image.animation = topAnim
        logo.animation = bottomAnim
        slogan.animation = bottomAnim

        handler.postDelayed(
            {
                if (auth.currentUser != null){
                    startActivity(Intent(this , Home::class.java))
                    finish()
                }else{
                    startActivity(Intent(this, Login::class.java))
                    finish()
                }

            },
            5000
        )


    }

    private fun init() {
        image = findViewById(R.id.imageView)
        logo = findViewById(R.id.textView)
        slogan = findViewById(R.id.textView2)
        handler = Handler()
        auth = FirebaseAuth.getInstance()
    }
}