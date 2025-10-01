package com.example.myklinikadnin.ui.profile

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.myklinikadnin.R
import com.example.myklinikadnin.ui.NotificationsActivity
import com.example.myklinikadnin.ui.main.MainActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private lateinit var tvName: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvNric: TextView
    private lateinit var etHeight: EditText
    private lateinit var etWeight: EditText
    private lateinit var tvBmi: TextView
    private lateinit var spinnerBloodType: Spinner
    private lateinit var etBloodPressure: EditText
    private lateinit var btnSave: Button

    private var userId: String? = null
    private var userRole: String? = null

    private val bloodTypes = arrayOf("Select Blood Type", "A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        userId = auth.currentUser?.uid

        // Bind views
        tvName = findViewById(R.id.tvName)
        tvEmail = findViewById(R.id.tvEmail)
        tvNric = findViewById(R.id.tvNric)
        etHeight = findViewById(R.id.etHeight)
        etWeight = findViewById(R.id.etWeight)
        tvBmi = findViewById(R.id.tvBmi)
        spinnerBloodType = findViewById(R.id.spinnerBloodType)
        etBloodPressure = findViewById(R.id.etBloodPressure)
        btnSave = findViewById(R.id.btnSaveProfile)

        // Spinner setup
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, bloodTypes)
        spinnerBloodType.adapter = adapter

        // Fetch user data
        loadUserProfile()

        // Auto calculate BMI
        val bmiWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { calculateBMI() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }
        etHeight.addTextChangedListener(bmiWatcher)
        etWeight.addTextChangedListener(bmiWatcher)

        btnSave.setOnClickListener { saveUserProfile() }

        // Bottom navigation
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.selectedItemId = R.id.nav_profile
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.nav_notifications -> {
                    startActivity(Intent(this, NotificationsActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.nav_profile -> true
                else -> false
            }
        }
    }

    private fun loadUserProfile() {
        userId?.let { uid ->
            db.collection("users").document(uid).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        tvName.text = "Name: ${doc.getString("name")}"
                        tvEmail.text = "Email: ${doc.getString("email")}"
                        tvNric.text = "NRIC: ${doc.getString("nric")}"

                        etHeight.setText(doc.getDouble("height")?.toString() ?: "")
                        etWeight.setText(doc.getDouble("weight")?.toString() ?: "")
                        etBloodPressure.setText(doc.getString("bloodPressure") ?: "")
                        userRole = doc.getString("role")

                        // Enable/disable blood pressure input
                        etBloodPressure.isEnabled = (userRole == "Doctor" || userRole == "Nurse")

                        // Set blood type in spinner
                        val bloodType = doc.getString("bloodType")
                        val index = bloodTypes.indexOf(bloodType)
                        if (index >= 0) spinnerBloodType.setSelection(index)

                        calculateBMI()
                    }
                }
        }
    }

    private fun calculateBMI() {
        val heightStr = etHeight.text.toString()
        val weightStr = etWeight.text.toString()
        if (heightStr.isNotEmpty() && weightStr.isNotEmpty()) {
            val heightM = heightStr.toDouble() / 100
            val weight = weightStr.toDouble()
            if (heightM > 0) {
                val bmi = weight / (heightM * heightM)
                val category = when {
                    bmi < 18.5 -> "Underweight"
                    bmi < 24.9 -> "Normal"
                    bmi < 29.9 -> "Overweight"
                    else -> "Obese"
                }
                tvBmi.text = "BMI: %.1f (%s)".format(bmi, category)
            }
        } else {
            tvBmi.text = "BMI: "
        }
    }

    private fun saveUserProfile() {
        val height = etHeight.text.toString().toDoubleOrNull()
        val weight = etWeight.text.toString().toDoubleOrNull()
        val bloodType = if (spinnerBloodType.selectedItemPosition > 0) spinnerBloodType.selectedItem.toString() else null
        val bloodPressure = etBloodPressure.text.toString()

        val updates = hashMapOf<String, Any>()
        if (height != null) updates["height"] = height
        if (weight != null) updates["weight"] = weight
        if (bloodType != null) updates["bloodType"] = bloodType
        if (userRole == "Doctor" || userRole == "Nurse") {
            if (bloodPressure.isNotEmpty()) updates["bloodPressure"] = bloodPressure
        }

        userId?.let { uid ->
            db.collection("users").document(uid).update(updates)
                .addOnSuccessListener {
                    Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
