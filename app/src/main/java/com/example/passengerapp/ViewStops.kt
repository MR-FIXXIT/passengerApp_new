package com.example.passengerapp

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore


class ViewStops : AppCompatActivity(), StopAdapter.OnItemClickListener {
    private lateinit var recyclerView: RecyclerView
    private lateinit var db: FirebaseFirestore
    private lateinit var adapter: StopAdapter
    private lateinit var list: MutableList<Modal>
    private var isFromAddRoute: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_stops)

        recyclerView = findViewById(R.id.rvShowStops)
        db = FirebaseFirestore.getInstance()

        isFromAddRoute = intent.getBooleanExtra("true", false)

        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(this)

        list = mutableListOf()
        adapter = StopAdapter(this, list, this)
        recyclerView.adapter = adapter

        showData()
    }



    private fun showData() {
        db.collection("Stop").get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    list.clear()
                    /*a DocumentSnapshot is a snapshot of a single document in a firestore database
                     it represents the current state of the document, including its fields and data*/
                    for (snapshot in task.result!!) {
                        val id = snapshot.id
                        val stopName = snapshot.getString("stop_name")
                        val lat = snapshot.getString("lat")
                        val long = snapshot.getString("long")

                        if (stopName != null && lat != null && long != null) {
                            val modal = Modal(
                                id = id,
                                lat = lat,
                                long = long,
                                name = stopName
                            )

                            list.add(modal)
                        } else {
                            // handle this later to deal with corrupt or incomplete data in firestore
                            Toast.makeText(this@ViewStops, "Some data was not able to be fetched", Toast.LENGTH_SHORT).show()
                        }
                    }
                    adapter.notifyDataSetChanged()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this@ViewStops, "Oops ... something went wrong", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onItemClick(modal: Modal) {
        if(isFromAddRoute){
            val nameIntent: List<String> = listOf(modal.name.toString(), modal.lat.toString(), modal.long.toString(), modal.id.toString())
            val resultIntent = intent.putStringArrayListExtra("selectedStop", ArrayList(nameIntent))
            setResult(Activity.RESULT_OK, resultIntent)
            finish()

        }
    }
}