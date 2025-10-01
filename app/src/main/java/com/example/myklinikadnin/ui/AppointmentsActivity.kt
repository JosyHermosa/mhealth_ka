package com.example.myklinikadnin.ui

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.myklinikadnin.R
import com.example.myklinikadnin.ui.main.MainActivity
import com.example.myklinikadnin.ui.profile.ProfileActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class AppointmentsActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private lateinit var doctorAdapter: ArrayAdapter<String>
    private val doctorList = mutableListOf<String>()

    private lateinit var calendarView: CalendarView
    private lateinit var searchDoctor: AutoCompleteTextView
    private lateinit var upcomingText: TextView
    private lateinit var saveButton: Button

    private var selectedDate: String? = null
    private var selectedTime: String? = null
    private val timeButtons = mutableListOf<Button>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_appointments)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Bind views
        searchDoctor = findViewById(R.id.searchDoctor)
        calendarView = findViewById(R.id.calendarView)
        upcomingText = findViewById(R.id.upcomingAppointmentText)
        saveButton = findViewById(R.id.btnSaveAppointment)

        // Set default selectedDate = today
        val today = Calendar.getInstance()
        val day = today.get(Calendar.DAY_OF_MONTH)
        val month = today.get(Calendar.MONTH) + 1
        val year = today.get(Calendar.YEAR)
        selectedDate = "$day/$month/$year"

        // Prepare doctor auto-complete adapter
        doctorAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, doctorList)
        searchDoctor.setAdapter(doctorAdapter)
        searchDoctor.threshold = 1

        // Fetch doctor names from Firestore
        db.collection("users")
            .whereEqualTo("role", "Doctor")
            .get()
            .addOnSuccessListener { result ->
                for (doc in result) {
                    val name = doc.getString("name")
                    if (name != null) doctorList.add(name)
                }
                doctorAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load doctors", Toast.LENGTH_SHORT).show()
            }

        // Collect time slot buttons
        val times = listOf(
            "9:00 AM" to R.id.btn9am,
            "10:00 AM" to R.id.btn10am,
            "11:00 AM" to R.id.btn11am,
            "12:00 PM" to R.id.btn12pm,
            "1:00 PM" to R.id.btn1pm,
            "2:00 PM" to R.id.btn2pm,
            "3:00 PM" to R.id.btn3pm,
            "4:00 PM" to R.id.btn4pm,
            "5:00 PM" to R.id.btn5pm
        )

        for ((time, id) in times) {
            val btn = findViewById<Button>(id)
            btn.setOnClickListener { selectTimeSlot(time, btn) }
            timeButtons.add(btn)
        }

        // Handle date selection
        calendarView.setOnDateChangeListener { _, year, month, day ->
            selectedDate = "$day/${month + 1}/$year"
            loadBookedSlots()
        }

        // Save appointment
        saveButton.setOnClickListener { saveAppointment() }

        // Load user’s upcoming appointments on start (just for text display)
        loadUserAppointments()

        // ✅ Bottom Navigation
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
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
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                else -> false
            }
        }
    }

    private fun selectTimeSlot(time: String, button: Button) {
        if (button.isEnabled) {
            // Reset all to default style
            for (btn in timeButtons) {
                btn.setBackgroundColor(ContextCompat.getColor(this, android.R.color.darker_gray))
            }
            // Highlight selected
            button.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_green_light))
            selectedTime = time
        }
    }

    private fun loadBookedSlots() {
        val doctorName = searchDoctor.text.toString().trim()
        if (doctorName.isEmpty() || selectedDate == null) return

        // Reset all slots
        for (btn in timeButtons) {
            btn.isEnabled = true
            btn.setBackgroundColor(ContextCompat.getColor(this, android.R.color.darker_gray))
        }

        // Fetch booked slots for this doctor + date
        db.collection("appointments")
            .whereEqualTo("doctor", doctorName)
            .whereEqualTo("date", selectedDate)
            .get()
            .addOnSuccessListener { result ->
                for (doc in result) {
                    val bookedTime = doc.getString("time")
                    val bookedBtn = timeButtons.find { it.text == bookedTime }
                    bookedBtn?.let {
                        it.isEnabled = false
                        it.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_red_light))
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load booked slots", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadUserAppointments() {
        val userId = auth.currentUser?.uid ?: return
        val now = System.currentTimeMillis()

        db.collection("appointments")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { result ->
                val futureAppointments = result.documents
                    .mapNotNull { doc ->
                        val doctor = doc.getString("doctor")
                        val date = doc.getString("date")
                        val time = doc.getString("time")
                        val timestamp = doc.getLong("timestamp")
                        if (doctor != null && date != null && time != null && timestamp != null && timestamp > now) {
                            Triple(doctor, date, time) to timestamp
                        } else null
                    }
                    .sortedBy { it.second }

                if (futureAppointments.isNotEmpty()) {
                    val (doctor, date, time) = futureAppointments.first().first
                    upcomingText.text = "Upcoming appointment: Dr. $doctor on $date at $time"
                } else {
                    upcomingText.text = "No upcoming appointments"
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load your appointments", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveAppointment() {
        val doctorName = searchDoctor.text.toString().trim()
        val userId = auth.currentUser?.uid
        val email = auth.currentUser?.email ?: "Unknown"

        if (doctorName.isEmpty() || !doctorList.contains(doctorName)) {
            Toast.makeText(this, "Please search and select a valid doctor name", Toast.LENGTH_SHORT).show()
            return
        }
        if (selectedDate == null || selectedTime == null) {
            Toast.makeText(this, "Please select date and time", Toast.LENGTH_SHORT).show()
            return
        }

        // Convert date+time into timestamp
        val sdf = SimpleDateFormat("dd/M/yyyy hh:mm a", Locale.getDefault())
        val dateTime = sdf.parse("$selectedDate $selectedTime")
        val timestamp = dateTime?.time ?: System.currentTimeMillis()

        val appointment = hashMapOf(
            "userId" to userId,
            "username" to email,
            "doctor" to doctorName,
            "date" to selectedDate,
            "time" to selectedTime,
            "createdAt" to System.currentTimeMillis(),
            "timestamp" to timestamp
        )

        db.collection("appointments")
            .add(appointment)
            .addOnSuccessListener {
                Toast.makeText(this, "Appointment booked!", Toast.LENGTH_SHORT).show()
                upcomingText.text = "Upcoming appointment: Dr. $doctorName on $selectedDate at $selectedTime"
                loadBookedSlots()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
