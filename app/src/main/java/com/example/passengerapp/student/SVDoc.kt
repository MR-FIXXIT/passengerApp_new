package com.example.passengerapp.student

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import com.example.passengerapp.R
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

class SVDoc : AppCompatActivity() {
    private lateinit var btnPhoto: ImageButton
    private lateinit var btnID: ImageButton
    private lateinit var btnDOBP: ImageButton
    private lateinit var btnUpload: Button
    private var P: Boolean = false
    private var I: Boolean = false
    private var D: Boolean = false
    private var isPhotoSelected: Boolean = false
    private var isIDSelected: Boolean = false
    private var isDOBPSelected: Boolean = false
    private lateinit var storageRef: StorageReference
    private lateinit var firebaseFirestore: FirebaseFirestore
    private var userPhotoUri: Uri? = null
    private var IDUri: Uri? = null
    private var DOBPUri: Uri? = null
    private lateinit var name: String
    private lateinit var gender: String
    private lateinit var dOB: String
    private lateinit var _user_Photo_Ref: StorageReference
    lateinit var _ID_Photo_Ref: StorageReference
    lateinit var _DOB_Proof_Ref: StorageReference

    var i: Int = 0
    var j: Int = 0
    var k: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sv_doc)

        name = intent.getStringExtra("userName")!!
        gender = intent.getStringExtra("userGender")!!
        dOB = intent.getStringExtra("userDOB")!!

        init()

        btnPhoto.setOnClickListener {
            P = true
            I = false
            D = false

            uploadImg()
        }

        btnID.setOnClickListener {
            P = false
            I = true
            D = false

            uploadImg()
        }

        btnDOBP.setOnClickListener {
            P = false
            I = false
            D = true

            uploadImg()
        }

        btnUpload.setOnClickListener {
            uploadToFirebase()
        }

    }

    private fun uploadToFirebase() {
        // Upload photo
        val folderName = "${name}_${System.currentTimeMillis()}"

        userPhotoUri?.let { uri ->
            _user_Photo_Ref = storageRef.child("${folderName}/${name}_photo")
            yo(folderName, uri, "photo")
        }

        IDUri?.let {uri ->
            _ID_Photo_Ref = storageRef.child("${folderName}/${name}_id_photo")
            yo(folderName, uri, "id_photo")
        }

        DOBPUri?.let {uri ->
            _DOB_Proof_Ref = storageRef.child("${folderName}/${name}_DOB_proof_photo")
            yo(folderName, uri, "DOB_proof_photo")
        }

        // Upload ID
        // Repeat the same process for IDUri and DOBPUri

    }

    fun yo(folder: String, uri: Uri, fileName: String){
        val photoRef = storageRef.child("${folder}/${name}_${fileName}")
        photoRef.putFile(uri).addOnCompleteListener { task ->
            if (task.isSuccessful) {

                checkAllImagesUploaded(folder)
                Log.i("tag", "${name}_${fileName}")

            } else {
                Toast.makeText(this, "Error uploading photo: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkAllImagesUploaded(folder: String) {
        if (userPhotoUri != null && IDUri != null && DOBPUri != null) {
            uploadDataToFirestore(folder)
        }
    }

    private fun uploadDataToFirestore(folder: String) {


//        _user_Photo_Ref.downloadUrl.addOnSuccessListener { it ->
//            _user_Photo_Url = it
//
//        }.addOnFailureListener { exception ->
//            Toast.makeText(this, "Error getting photo download URL: ${exception.message}", Toast.LENGTH_SHORT).show()
//        }
//

        val data = hashMapOf(
            "name" to name,
            "gender" to gender,
            "dob" to dOB,
            "user_Photo" to "${storageRef.child("$folder/${name}_photo").path}",
            "ID_Photo" to "${storageRef.child("$folder/${name}_id_photo").path}",
            "DOB_Proof_Photo" to "${storageRef.child("$folder/${name}_DOB_proof_photo").path}"
        )

        firebaseFirestore.collection("students").document(name+dOB)
            .set(data)
            .addOnSuccessListener {
                Toast.makeText(this, "Data uploaded successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error uploading data to Firestore: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun init(){
        btnPhoto = findViewById(R.id.btnPhoto_SVDoc)
        btnID = findViewById(R.id.btnID_SVDoc)
        btnDOBP = findViewById(R.id.btnDOBP_SVDoc)
        btnUpload = findViewById(R.id.btnUpload_SVDoc)
        storageRef = FirebaseStorage.getInstance().reference.child("StudentsData")
        firebaseFirestore = FirebaseFirestore.getInstance()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (resultCode) {
            Activity.RESULT_OK -> {
                //Image Uri will not be null for RESULT_OK
                val uri: Uri = data?.data!!

                if(P){
                    // Use Uri object instead of File to avoid storage permissions
                    btnPhoto.setImageURI(uri)
                    userPhotoUri = uri
                    isPhotoSelected = true
                }

                if(I){
                    // Use Uri object instead of File to avoid storage permissions
                    btnID.setImageURI(uri)
                    IDUri = uri
                    isIDSelected = true
                }

                if(D){
                    // Use Uri object instead of File to avoid storage permissions
                    btnDOBP.setImageURI(uri)
                    DOBPUri = uri
                    isDOBPSelected = true
                }

                if(isDOBPSelected && isIDSelected && isPhotoSelected){
                    btnUpload.isEnabled = true
                }
            }
            ImagePicker.RESULT_ERROR -> {
                Toast.makeText(this, ImagePicker.getError(data), Toast.LENGTH_SHORT).show()
            }
            else -> {
                Toast.makeText(this, "Task Cancelled", Toast.LENGTH_SHORT).show()
            }
        }
    }



    private fun uploadImg(){
        ImagePicker.with(this)
            .crop()	    			                //Crop image(Optional), Check Customization for more option
            .compress(1024)			        //Final image size will be less than 1 MB(Optional)
            .maxResultSize(1080, 1080)	//Final image resolution will be less than 1080 x 1080(Optional)
            .start()
    }
}