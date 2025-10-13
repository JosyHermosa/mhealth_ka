package com.example.myklinikadnin.ui.main

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.cardview.widget.CardView
import com.example.myklinikadnin.R
import com.example.myklinikadnin.adapters.ArticleAdapter
import com.example.myklinikadnin.data.Article
import com.example.myklinikadnin.ui.AppointmentsActivity
import com.example.myklinikadnin.ui.DoctorDashboardActivity
import com.example.myklinikadnin.ui.NurseDashboardActivity
import com.example.myklinikadnin.ui.auth.LoginActivity
import com.example.myklinikadnin.ui.profile.ProfileActivity
import com.example.myklinikadnin.ui.NotificationsActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var mainTitle: TextView
    private lateinit var appointmentPreview: TextView

    // RecyclerView for articles
    private lateinit var recyclerView: RecyclerView
    private lateinit var articleAdapter: ArticleAdapter
    private val articleList = mutableListOf<Article>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        mainTitle = findViewById(R.id.mainTitle)
        appointmentPreview = findViewById(R.id.appointmentPreview)

        // Setup RecyclerView
        recyclerView = findViewById(R.id.articlesRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        articleAdapter = ArticleAdapter(articleList)
        recyclerView.adapter = articleAdapter

        // CardView navigation
        val cardAppointments = findViewById<CardView>(R.id.cardAppointments)
        cardAppointments.setOnClickListener {
            startActivity(Intent(this, AppointmentsActivity::class.java))
        }

        // Get current user and role
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val userId = currentUser.uid // ✅ use UID instead of email

            db.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val name = document.getString("name") ?: "User"
                        val role = document.getString("role") ?: "Patient"

                        when (role.lowercase()) {
                            "doctor" -> {
                                startActivity(Intent(this, DoctorDashboardActivity::class.java))
                                finish()
                                return@addOnSuccessListener
                            }
                            "nurse" -> {
                                startActivity(Intent(this, NurseDashboardActivity::class.java))
                                finish()
                                return@addOnSuccessListener
                            }
                            else -> {
                                // Patient dashboard
                                mainTitle.text = "Hello, $name 👋"
                                loadArticles()
                                loadUpcomingAppointment(userId) // ✅ pass UID
                            }
                        }
                    } else {
                        mainTitle.text = "Hello, User 👋"
                        loadArticles()
                        loadUpcomingAppointment(userId)
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to load user data", Toast.LENGTH_SHORT).show()
                }
        } else {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        // Bottom Navigation
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.selectedItemId = R.id.nav_home
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_notifications -> {
                    startActivity(Intent(this, NotificationsActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                else -> false
            }
        }
    }

    private fun loadArticles() {
        db.collection("articles")
            .limit(5) // only latest 5
            .get()
            .addOnSuccessListener { result ->
                articleList.clear()
                for (doc in result) {
                    val article = doc.toObject(Article::class.java)
                    articleList.add(article)
                }
                articleAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load articles", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadUpcomingAppointment(userId: String) {
        val today = System.currentTimeMillis()
        val sdf = SimpleDateFormat("d/M/yyyy", Locale.getDefault())

        db.collection("appointments")
            .whereEqualTo("userId", userId) // ✅ matches Firestore
            .get()
            .addOnSuccessListener { result ->
                var nearestDoc: com.google.firebase.firestore.DocumentSnapshot? = null
                var nearestDate: Long = Long.MAX_VALUE

                for (doc in result) {
                    val dateStr = doc.getString("date") ?: continue
                    try {
                        val dateObj = sdf.parse(dateStr) ?: continue
                        val dateMillis = dateObj.time

                        if (dateMillis >= today && dateMillis < nearestDate) {
                            nearestDate = dateMillis
                            nearestDoc = doc
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                if (nearestDoc != null) {
                    val date = nearestDoc.getString("date") ?: ""
                    val time = nearestDoc.getString("time") ?: ""
                    val doctor = nearestDoc.getString("doctor") ?: ""
                    appointmentPreview.text =
                        "Upcoming Appointment: $date at $time with Dr. $doctor"
                } else {
                    appointmentPreview.text = "Upcoming Appointment: None"
                }
            }
            .addOnFailureListener {
                appointmentPreview.text = "Upcoming Appointment: None"
                Toast.makeText(this, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
