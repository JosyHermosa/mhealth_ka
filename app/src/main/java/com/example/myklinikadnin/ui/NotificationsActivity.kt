package com.example.myklinikadnin.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myklinikadnin.R
import com.example.myklinikadnin.adapters.NotificationAdapter
import com.example.myklinikadnin.data.Notification
import com.example.myklinikadnin.ui.main.MainActivity
import com.example.myklinikadnin.ui.profile.ProfileActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.FirebaseFirestore

class NotificationsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: NotificationAdapter
    private val notificationsList = mutableListOf<Notification>()

    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notifications)

        recyclerView = findViewById(R.id.recyclerViewNotifications)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = NotificationAdapter(notificationsList)
        recyclerView.adapter = adapter

        db = FirebaseFirestore.getInstance()

        loadNotifications()

        // ✅ Bottom Navigation
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.selectedItemId = R.id.nav_notifications

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.nav_notifications -> true // already here
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
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
                notificationsList.clear()
                for (doc in result) {
                    val role = doc.getString("role") ?: "All"
                    if (role == "All" || role == "Patient") {
                        val notification = doc.toObject(Notification::class.java)
                        notificationsList.add(notification)
                    }
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load notifications", Toast.LENGTH_SHORT).show()
            }
    }
}
