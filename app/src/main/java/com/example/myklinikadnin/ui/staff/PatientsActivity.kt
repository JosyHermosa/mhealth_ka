package com.example.myklinikadnin.ui.staff

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myklinikadnin.R
import com.example.myklinikadnin.data.Patient
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class PatientsActivity : AppCompatActivity() {

    private lateinit var recyclerPatients: RecyclerView
    private var adapter: PatientAdapter? = null
    private val patients = mutableListOf<Patient>()
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var role: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_patients)

        recyclerPatients = findViewById(R.id.recyclerPatients)
        recyclerPatients.layoutManager = LinearLayoutManager(this)

        loadRoleAndPatients()

        // Bottom navigation
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.selectedItemId = R.id.nav_patients

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> {
                    startActivity(Intent(this, StaffDashboardActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }

                R.id.nav_patients -> true // already here

                R.id.nav_staff_notifications -> {
                    startActivity(Intent(this, StaffNotificationsActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }

                R.id.nav_staff_profile -> {
                    startActivity(Intent(this, StaffProfileActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }

                else -> false
            }
        }
    }

    private fun loadRoleAndPatients() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).get()
            .addOnSuccessListener { doc ->
                role = doc.getString("role") ?: "Doctor"

                // ✅ Create adapter after we know the role
                adapter = PatientAdapter(patients, role!!)
                recyclerPatients.adapter = adapter

                loadPatients()
            }
    }

    private fun loadPatients() {
        db.collection("users")
            .whereEqualTo("role", "Patient")
            .get()
            .addOnSuccessListener { result ->
                patients.clear()
                for (doc in result) {
                    val patient = Patient(
                        uid = doc.getString("uid") ?: "",
                        name = doc.getString("name") ?: "",
                        email = doc.getString("email") ?: "",
                        phone = doc.getString("phone") ?: "",
                        nric = doc.getString("nric") ?: "",
                        bloodType = doc.getString("bloodType") ?: "",
                        bloodPressure = doc.getString("bloodPressure") ?: "",
                        height = doc.getLong("height")?.toInt() ?: 0,
                        weight = doc.getLong("weight")?.toInt() ?: 0,
                        createdAt = doc.getLong("createdAt") ?: 0
                    )
                    patients.add(patient)
                }
                adapter?.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load patients", Toast.LENGTH_SHORT).show()
            }
    }
}
