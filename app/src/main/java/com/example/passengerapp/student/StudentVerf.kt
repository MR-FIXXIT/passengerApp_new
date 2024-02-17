package com.example.passengerapp.student

import android.app.DatePickerDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import com.example.passengerapp.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class StudentVerf : AppCompatActivity() {
    private lateinit var dOB: String
    private lateinit var gender: String
    private lateinit var name: String
    private lateinit var btnToStuDocs: Button
    private lateinit var etName: EditText
    private lateinit var spnrGender: Spinner
    private lateinit var etDOB: EditText
    private lateinit var ibCalendar: ImageButton
    private val calendar: Calendar = Calendar.getInstance()
    private val dateFormat: SimpleDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_verf)

        init()

        val genderAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.gender_array,
            R.layout.spinner_dropdown_item
        )

        genderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spnrGender.adapter = genderAdapter

        spnrGender.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateButtonState()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        etName.addTextChangedListener {
            updateButtonState()
        }

        ibCalendar.setOnClickListener{
            showDatePickerDialog()

        }

        btnToStuDocs.setOnClickListener {
            getUserInfo()

            val intent = Intent(this@StudentVerf, SVDoc::class.java)
            intent.putExtra("userName", name)
            intent.putExtra("userGender", gender)
            intent.putExtra("userDOB", dOB)

            startActivity(intent)
        }
    }

    private fun showDatePickerDialog() {
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                // Update calendar with selected date
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                // Update EditText text with selected date

                if(isAgeGreaterThanSeven(dateFormat.format(calendar.time))){
                    etDOB.isEnabled = true
                    etDOB.setText(dateFormat.format(calendar.time))
                    etDOB.isEnabled = false
                    updateButtonState()
                }else{
                    Toast.makeText(this, "Student must be older than 7 years", Toast.LENGTH_LONG).show()
                }
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }
    private fun isAgeGreaterThanSeven(selectedDob: String): Boolean {
        val dobDate = dateFormat.parse(selectedDob)

        val currentCalendar = Calendar.getInstance()

        currentCalendar.add(Calendar.YEAR, -7)

        return dobDate?.before(currentCalendar.time) ?: false
    }


    private fun updateButtonState() {
        btnToStuDocs.isEnabled = etName.text.isNotEmpty() && etDOB.text.isNotEmpty()
    }

    private fun getUserInfo() {
        name = etName.text.toString()
        gender = spnrGender.selectedItem.toString()
        dOB = etDOB.text.toString()
    }

    private fun init(){
        btnToStuDocs = findViewById(R.id.btnToStuDoc_StudentVerf)
        etName = findViewById(R.id.etName_StudentVerf)
        spnrGender = findViewById(R.id.spnrGender_StudentVerf)
        etDOB = findViewById(R.id.etDOB_StudentVerf)
        ibCalendar = findViewById(R.id.ibCalendar_StudentVerf)
    }
}