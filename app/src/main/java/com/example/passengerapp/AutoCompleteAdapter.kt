package com.example.passengerapp

import android.content.Context
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Filter
import com.google.firebase.firestore.CollectionReference

class AutoCompleteAdapter(
    context: Context,
    private val stopCollectionRef: CollectionReference
) : ArrayAdapter<String>(context, android.R.layout.simple_dropdown_item_1line) {

    private val suggestions = mutableListOf<String>()

    init {
        // Preload data from Firestore and store it locally
        preloadData()
    }

    fun preloadData() {
        stopCollectionRef.orderBy("stop_name")
            .get()
            .addOnSuccessListener { querySnapshot ->
                Log.d("LOADEDBOYO","GOTTEM")
                suggestions.clear()
                for (document in querySnapshot.documents) {
                    val stopName = document.getString("stop_name")
                    if (stopName != null) {
                        suggestions.add(stopName)
                    }
                }
                notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Log.d("AINTLOADINGBOYO","Error getting documents: ${e.message}")
            }
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val filterResults = FilterResults()
                if (constraint != null) {
                    val filteredSuggestions = suggestions.filter {
                        it.startsWith(constraint.toString(), ignoreCase = true)
                    }

                    filterResults.values = filteredSuggestions
                    filterResults.count = filteredSuggestions.size
                }
                return filterResults
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                results?.let {
                    notifyDataSetChanged()
                }
            }

            override fun convertResultToString(resultValue: Any?): CharSequence {
                return resultValue as String
            }
        }
    }
}

