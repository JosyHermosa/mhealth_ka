package com.example.myklinikadnin.ui.staff

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.myklinikadnin.R
import com.example.myklinikadnin.ui.NotificationsActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class StaffProfileActivity : AppCompatActivity() {

    private lateinit var etName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etNric: EditText
    private lateinit var etPhone: EditText

    private lateinit var changePasswordForm: LinearLayout
    private lateinit var etCurrentPassword: EditText
    private lateinit var etNewPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var btnSavePassword: Button
    private lateinit var btnSaveProfile: Button

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_staff_profile)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Bind profile views
        etName = findViewById(R.id.etName)
        etEmail = findViewById(R.id.etEmail)
        etNric = findViewById(R.id.etNric)
        etPhone = findViewById(R.id.etPhone)

        // Bind password form
        changePasswordForm = findViewById(R.id.changePasswordForm)
        etCurrentPassword = findViewById(R.id.etCurrentPassword)
        etNewPassword = findViewById(R.id.etNewPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        btnSavePassword = findViewById(R.id.btnSavePassword)
        btnSaveProfile = findViewById(R.id.btnSaveProfile)

        // Toggle change password form
        findViewById<androidx.cardview.widget.CardView>(R.id.cardChangePassword)
            .setOnClickListener {
                changePasswordForm.visibility =
                    if (changePasswordForm.visibility == View.GONE) View.VISIBLE else View.GONE
            }

        btnSavePassword.setOnClickListener { changePassword() }
        btnSaveProfile.setOnClickListener { saveProfile() }

        loadProfile()
        setupBottomNav()
    }

    private fun loadProfile() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).get()
            .addOnSuccessListener { doc ->
                etName.setText(doc.getString("name") ?: "")
                etEmail.setText(doc.getString("email") ?: "")
                etNric.setText(doc.getString("nric") ?: "")
                etPhone.setText(doc.getString("phone") ?: "-")
            }
    }

    private fun saveProfile() {
        val userId = auth.currentUser?.uid ?: return
        val updates = hashMapOf(
            "name" to etName.text.toString(),
            "phone" to etPhone.text.toString()
        )

        db.collection("users").document(userId).update(updates as Map<String, Any>)
            .addOnSuccessListener {
                Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun changePassword() {
        val currentPass = etCurrentPassword.text.toString()
        val newPass = etNewPassword.text.toString()
        val confirmPass = etConfirmPassword.text.toString()

        if (currentPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (newPass != confirmPass) {
            Toast.makeText(this, "New passwords do not match", Toast.LENGTH_SHORT).show()
            return
        }

        val user = auth.currentUser ?: return
        val email = user.email ?: return
        val credential = EmailAuthProvider.getCredential(email, currentPass)

        user.reauthenticate(credential).addOnSuccessListener {
            user.updatePassword(newPass).addOnSuccessListener {
                Toast.makeText(this, "Password updated", Toast.LENGTH_SHORT).show()
                changePasswordForm.visibility = View.GONE
            }.addOnFailureListener {
                Toast.makeText(this, "Failed: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Current password incorrect", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupBottomNav() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.selectedItemId = R.id.nav_staff_profile

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> {
                    startActivity(Intent(this, StaffDashboardActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.nav_patients -> {
                    startActivity(Intent(this, PatientsActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.nav_staff_notifications -> {
                    startActivity(Intent(this, StaffNotificationsActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.nav_staff_profile -> true
                else -> false
            }
        }
    }
}
