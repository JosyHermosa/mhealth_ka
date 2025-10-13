package com.example.myklinikadnin.ui.staff

import android.content.Intent
import android.app.AlertDialog
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myklinikadnin.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class StaffNotificationsActivity : AppCompatActivity() {

    private lateinit var recyclerNotifications: RecyclerView
    private lateinit var adapter: StaffNotificationAdapter
    private val notifications = mutableListOf<Map<String, Any>>()
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_staff_notifications)

        recyclerNotifications = findViewById(R.id.recyclerNotifications)
        recyclerNotifications.layoutManager = LinearLayoutManager(this)

        adapter = StaffNotificationAdapter(notifications)
        recyclerNotifications.adapter = adapter

        loadNotifications()

        // Floating button to add new announcement
        findViewById<FloatingActionButton>(R.id.fabAddNotification).setOnClickListener {
            showAddNotificationDialog()
        }

        // Bottom navigation
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.selectedItemId = R.id.nav_staff_notifications
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> {
                    finish()
                    true
                }
                R.id.nav_patients -> {
                    startActivity(Intent(this, PatientsActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.nav_staff_notifications -> true
                R.id.nav_staff_profile -> {
                    startActivity(Intent(this, StaffProfileActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                else -> false
            }
        }
    }

    private fun loadNotifications() {
        db.collection("notifications")
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                notifications.clear()
                for (doc in result) {
                    val role = doc.getString("role") ?: "All"
                    if (role == "All" || role == "Doctor" || role == "Nurse") {
                        val notif = doc.data
                        notifications.add(notif)
                    }
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load notifications", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showAddNotificationDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_notification, null)
        val titleInput = dialogView.findViewById<EditText>(R.id.editNotifTitle)
        val messageInput = dialogView.findViewById<EditText>(R.id.editNotifMessage)

        AlertDialog.Builder(this)
            .setTitle("Add Announcement")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val title = titleInput.text.toString().trim()
                val message = messageInput.text.toString().trim()
                if (title.isEmpty() || message.isEmpty()) {
                    Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val notif = hashMapOf(
                    "title" to title,
                    "message" to message,
                    "role" to "All", // broadcast
                    "createdAt" to System.currentTimeMillis()
                )

                db.collection("notifications").add(notif)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Notification added", Toast.LENGTH_SHORT).show()
                        loadNotifications()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
