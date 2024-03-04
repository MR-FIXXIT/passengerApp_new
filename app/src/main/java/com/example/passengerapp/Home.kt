package com.example.passengerapp

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.example.passengerapp.student.StudentVerf
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth


class Home : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private var drawerLayout: DrawerLayout? = null
    private var navigationView: NavigationView? = null
    private var toolbar: Toolbar? = null
    private var menu: Menu? = null
    private lateinit var auth: FirebaseAuth
    private lateinit var btnTicket: Button
    private lateinit var btnMap: Button


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        init()

        navigationView!!.bringToFront()
        val toggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout!!.addDrawerListener(toggle)
        toggle.syncState()
        navigationView!!.setNavigationItemSelectedListener(this)
        navigationView!!.setCheckedItem(R.id.nav_home)
        menu = navigationView!!.menu


        btnMap.setOnClickListener{
            startActivity(Intent(this, Map::class.java))
        }

        btnTicket.setOnClickListener{
            startActivity(Intent(this, BuyTicket::class.java))
        }

    }

    override fun onBackPressed() {
        if (drawerLayout!!.isDrawerOpen(GravityCompat.START)) {
            drawerLayout!!.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onNavigationItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.nav_home -> {
                startActivity(Intent(this, Home::class.java))
            }

            R.id.nav_stuVer -> {
                startActivity(Intent(this, StudentVerf::class.java))
            }

            R.id.nav_logout -> {
                auth.signOut()
                startActivity(Intent(this, Login::class.java))
                finish()
            }

        }
        drawerLayout!!.closeDrawer(GravityCompat.START)
        return true
    }

    private fun init() {
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.nav_view)
        toolbar = findViewById(R.id.toolbar)
        auth = FirebaseAuth.getInstance()
        btnTicket = findViewById(R.id.btnTicket_home)
        btnMap = findViewById(R.id.btnMap_home)
    }


}