package com.example.myklinikadnin.ui.staff

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.myklinikadnin.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso
import java.text.SimpleDateFormat
import java.util.*

class StaffDashboardActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var welcomeText: TextView
    private lateinit var todayAppointmentsContainer: LinearLayout

    // New appointment form
    private lateinit var newAppointmentForm: LinearLayout
    private lateinit var btnPickDate: Button
    private lateinit var timeSlotsContainer: LinearLayout
    private lateinit var spinnerPatients: Spinner
    private lateinit var btnConfirmAppointment: Button

    // Articles section
    private lateinit var articlesForm: LinearLayout
    private lateinit var articlesContainer: LinearLayout
    private lateinit var btnAddArticle: Button

    private var selectedDate: String? = null
    private var selectedTime: String? = null
    private var selectedPatient: String? = null
    private var selectedPatientId: String? = null
    private var selectedPatientEmail: String? = null
    private var doctorName: String? = null
    private var role: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_staff_dashboard)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        welcomeText = findViewById(R.id.welcomeText)
        todayAppointmentsContainer = findViewById(R.id.todayAppointmentsContainer)

        // Appointment form
        newAppointmentForm = findViewById(R.id.newAppointmentForm)
        btnPickDate = findViewById(R.id.btnPickDate)
        timeSlotsContainer = findViewById(R.id.timeSlotsContainer)
        spinnerPatients = findViewById(R.id.spinnerPatients)
        btnConfirmAppointment = findViewById(R.id.btnConfirmAppointment)

        // Articles form
        articlesForm = findViewById(R.id.articlesForm)
        articlesContainer = findViewById(R.id.articlesContainer)
        btnAddArticle = findViewById(R.id.btnAddArticle)

        // Fetch staff info
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).get()
            .addOnSuccessListener { doc ->
                role = doc.getString("role") ?: "Staff"
                doctorName = doc.getString("name") ?: "Staff"

                if (role == "Doctor") {
                    welcomeText.text = "Welcome, Dr. $doctorName"
                    loadTodayAppointmentsForDoctor(doctorName!!)
                    loadArticles()
                    findViewById<CardView>(R.id.cardNewAppointment).visibility = View.VISIBLE
                    findViewById<CardView>(R.id.cardArticles).visibility = View.VISIBLE
                } else if (role == "Nurse") {
                    welcomeText.text = "Welcome, Nurse $doctorName"
                    loadTodayAppointmentsForNurse()
                    findViewById<CardView>(R.id.cardNewAppointment).visibility = View.GONE
                    findViewById<CardView>(R.id.cardArticles).visibility = View.GONE
                }
            }

        // Toggle expand for New Appointment card
        findViewById<CardView>(R.id.cardNewAppointment).setOnClickListener {
            newAppointmentForm.visibility =
                if (newAppointmentForm.visibility == View.GONE) View.VISIBLE else View.GONE
        }

        // Toggle expand for Articles card
        findViewById<CardView>(R.id.cardArticles).setOnClickListener {
            articlesForm.visibility =
                if (articlesForm.visibility == View.GONE) View.VISIBLE else View.GONE
        }

        // Load patients into spinner
        loadPatients()

        // Date picker
        btnPickDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(
                this,
                { _, year, month, day ->
                    val picked = Calendar.getInstance()
                    picked.set(year, month, day)
                    val sdf = SimpleDateFormat("d/M/yyyy", Locale.getDefault())
                    selectedDate = sdf.format(picked.time)
                    btnPickDate.text = "Date: $selectedDate"
                    loadTimeSlots(selectedDate!!)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        btnConfirmAppointment.setOnClickListener { createAppointment() }
        btnAddArticle.setOnClickListener { promptAddOrEditArticle(null) }

        // Bottom navigation
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.selectedItemId = R.id.nav_dashboard
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> true
                R.id.nav_patients -> {
                    startActivity(Intent(this, PatientsActivity::class.java))
                    overridePendingTransition(0, 0); true
                }
                R.id.nav_staff_notifications -> {
                    startActivity(Intent(this, StaffNotificationsActivity::class.java))
                    overridePendingTransition(0, 0); true
                }
                R.id.nav_staff_profile -> {
                    startActivity(Intent(this, StaffProfileActivity::class.java))
                    overridePendingTransition(0, 0); true
                }
                else -> false
            }
        }
    }

    // ------------------- Doctor's Appointments ----------------------
    private fun loadTodayAppointmentsForDoctor(doctorName: String) {
        todayAppointmentsContainer.removeAllViews()
        val today = SimpleDateFormat("d/M/yyyy", Locale.getDefault()).format(Date())

        db.collection("appointments")
            .whereEqualTo("doctor", doctorName)
            .whereEqualTo("date", today)
            .get()
            .addOnSuccessListener { result ->
                showAppointments(result, showDoctorName = false, allowDone = true)
            }
    }

    // ------------------- Nurse's Appointments (Grouped by Doctor) ----------------------
    private fun loadTodayAppointmentsForNurse() {
        todayAppointmentsContainer.removeAllViews()
        val today = SimpleDateFormat("d/M/yyyy", Locale.getDefault()).format(Date())

        db.collection("appointments")
            .whereEqualTo("date", today)
            .get()
            .addOnSuccessListener { result ->
                if (result.isEmpty) {
                    val tv = TextView(this)
                    tv.text = "No appointments today"
                    todayAppointmentsContainer.addView(tv)
                    return@addOnSuccessListener
                }

                // Group appointments by doctor
                val grouped = result.groupBy { it.getString("doctor") ?: "Unknown" }

                for ((doctor, appointments) in grouped) {
                    val doctorTitle = TextView(this)
                    doctorTitle.text = "Dr. $doctor"
                    doctorTitle.textSize = 18f
                    doctorTitle.setPadding(0, 16, 0, 8)
                    todayAppointmentsContainer.addView(doctorTitle)

                    for (doc in appointments) {
                        val patient = doc.getString("username") ?: "Unknown"
                        val time = doc.getString("time") ?: ""

                        val row = LinearLayout(this)
                        row.orientation = LinearLayout.HORIZONTAL
                        row.setPadding(0, 4, 0, 4)

                        val tv = TextView(this)
                        tv.text = "$time - $patient"
                        tv.layoutParams =
                            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

                        row.addView(tv)
                        todayAppointmentsContainer.addView(row)
                    }
                }
            }
    }

    private fun showAppointments(
        result: com.google.firebase.firestore.QuerySnapshot,
        showDoctorName: Boolean,
        allowDone: Boolean
    ) {
        if (result.isEmpty) {
            val tv = TextView(this)
            tv.text = "No appointments today"
            todayAppointmentsContainer.addView(tv)
        } else {
            for (doc in result) {
                val patient = doc.getString("username") ?: "Unknown"
                val time = doc.getString("time") ?: ""
                val doctor = doc.getString("doctor") ?: "Unknown"
                val appointmentId = doc.id

                val row = LinearLayout(this)
                row.orientation = LinearLayout.HORIZONTAL
                row.setPadding(0, 8, 0, 8)

                val tv = TextView(this)
                tv.text = if (showDoctorName) "$time - $patient (Dr. $doctor)" else "$time - $patient"
                tv.layoutParams =
                    LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

                val btn = Button(this)
                btn.text = "Done"
                btn.isEnabled = allowDone
                btn.setOnClickListener {
                    db.collection("appointments").document(appointmentId).delete()
                        .addOnSuccessListener {
                            Toast.makeText(this, "Appointment marked done", Toast.LENGTH_SHORT).show()
                            if (showDoctorName) loadTodayAppointmentsForNurse()
                            else loadTodayAppointmentsForDoctor(doctorName!!)
                        }
                }

                row.addView(tv)
                if (allowDone) row.addView(btn) // Nurse can't see Done
                todayAppointmentsContainer.addView(row)
            }
        }
    }

    // ------------------- Patients for Appointment ----------------------
    private fun loadPatients() {
        db.collection("users")
            .whereEqualTo("role", "Patient")
            .get()
            .addOnSuccessListener { result ->
                val patients = result.map {
                    Triple(
                        it.id,
                        it.getString("email") ?: "unknown",
                        it.getString("name") ?: "Unnamed"
                    )
                }

                val names = patients.map { it.third }
                val adapter =
                    ArrayAdapter(this, android.R.layout.simple_spinner_item, names)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerPatients.adapter = adapter

                spinnerPatients.onItemSelectedListener =
                    object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(
                            parent: AdapterView<*>,
                            view: View?,
                            pos: Int,
                            id: Long
                        ) {
                            selectedPatientId = patients[pos].first
                            selectedPatientEmail = patients[pos].second
                            selectedPatient = patients[pos].third
                        }

                        override fun onNothingSelected(p0: AdapterView<*>?) {}
                    }
            }
    }

    private fun loadTimeSlots(date: String) {
        timeSlotsContainer.removeAllViews()
        val allSlots = listOf(
            "09:00 AM", "10:00 AM", "11:00 AM", "12:00 PM",
            "02:00 PM", "03:00 PM", "04:00 PM"
        )

        db.collection("appointments").whereEqualTo("date", date).get()
            .addOnSuccessListener { result ->
                val takenTimes = result.mapNotNull { it.getString("time") }
                for (slot in allSlots) {
                    val btn = Button(this)
                    btn.text = slot
                    if (takenTimes.contains(slot)) {
                        btn.isEnabled = false
                        btn.setBackgroundColor(Color.RED)
                    }
                    btn.setOnClickListener {
                        selectedTime = slot
                        Toast.makeText(this, "Selected $slot", Toast.LENGTH_SHORT).show()
                    }
                    timeSlotsContainer.addView(btn)
                }
            }
    }

    private fun createAppointment() {
        if (selectedDate == null || selectedTime == null || selectedPatientId == null) {
            Toast.makeText(this, "Please select date, time and patient", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("appointments")
            .whereEqualTo("userId", selectedPatientId)
            .whereEqualTo("date", selectedDate)
            .whereEqualTo("time", selectedTime)
            .get()
            .addOnSuccessListener { result ->
                if (!result.isEmpty) {
                    Toast.makeText(this, "This patient already has an appointment at that time.", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val sdf = SimpleDateFormat("d/M/yyyy h:mm a", Locale.getDefault())
                val dateTime = sdf.parse("$selectedDate $selectedTime")
                val timestamp = dateTime?.time ?: System.currentTimeMillis()

                val appointment = hashMapOf(
                    "createdAt" to System.currentTimeMillis(),
                    "date" to selectedDate,
                    "time" to selectedTime,
                    "doctor" to doctorName,
                    "username" to selectedPatientEmail,
                    "userId" to selectedPatientId,
                    "timestamp" to timestamp
                )

                db.collection("appointments").add(appointment)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Appointment created", Toast.LENGTH_SHORT).show()
                        newAppointmentForm.visibility = View.GONE
                        loadTodayAppointmentsForDoctor(doctorName!!)
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Failed: ${it.message}", Toast.LENGTH_SHORT).show()
                    }
            }
    }

    // ------------------- Articles ----------------------
    private fun loadArticles() {
        articlesContainer.removeAllViews()
        db.collection("articles")
            .get()
            .addOnSuccessListener { result ->
                for (doc in result) {
                    val title = doc.getString("title") ?: ""
                    val description = doc.getString("description") ?: ""
                    val imageUrl = doc.getString("imageUrl") ?: ""
                    val link = doc.getString("link") ?: ""
                    val articleId = doc.id

                    val card = CardView(this)
                    card.radius = 12f
                    card.setContentPadding(16, 16, 16, 16)
                    card.useCompatPadding = true

                    val layout = LinearLayout(this)
                    layout.orientation = LinearLayout.VERTICAL

                    val titleView = TextView(this)
                    titleView.text = title
                    titleView.textSize = 18f
                    titleView.setTextColor(Color.BLACK)

                    val descView = TextView(this)
                    descView.text = description

                    val imgView = ImageView(this)
                    if (imageUrl.isNotEmpty()) {
                        Picasso.get().load(imageUrl).into(imgView)
                        imgView.layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            400
                        )
                    }

                    val btnEdit = Button(this)
                    btnEdit.text = "Edit"
                    btnEdit.setOnClickListener {
                        promptAddOrEditArticle(articleId)
                    }

                    val btnDelete = Button(this)
                    btnDelete.text = "Delete"
                    btnDelete.setOnClickListener {
                        db.collection("articles").document(articleId).delete()
                            .addOnSuccessListener {
                                Toast.makeText(this, "Article deleted", Toast.LENGTH_SHORT).show()
                                loadArticles()
                            }
                    }

                    layout.addView(titleView)
                    layout.addView(descView)
                    layout.addView(imgView)
                    layout.addView(btnEdit)
                    layout.addView(btnDelete)

                    card.addView(layout)
                    articlesContainer.addView(card)
                }
            }
    }

    private fun promptAddOrEditArticle(articleId: String?) {
        db.collection("articles").get().addOnSuccessListener {
            if (articleId == null && it.size() >= 5) {
                Toast.makeText(this, "Maximum 5 articles allowed", Toast.LENGTH_SHORT).show()
                return@addOnSuccessListener
            }

            val dialogView = layoutInflater.inflate(R.layout.dialog_article, null)
            val titleInput = dialogView.findViewById<EditText>(R.id.editTitle)
            val descInput = dialogView.findViewById<EditText>(R.id.editDescription)
            val linkInput = dialogView.findViewById<EditText>(R.id.editLink)
            val imageInput = dialogView.findViewById<EditText>(R.id.editImageUrl)

            if (articleId != null) {
                db.collection("articles").document(articleId).get()
                    .addOnSuccessListener { doc ->
                        titleInput.setText(doc.getString("title"))
                        descInput.setText(doc.getString("description"))
                        linkInput.setText(doc.getString("link"))
                        imageInput.setText(doc.getString("imageUrl"))
                    }
            }

            AlertDialog.Builder(this)
                .setTitle(if (articleId == null) "Add Article" else "Edit Article")
                .setView(dialogView)
                .setPositiveButton("Save") { _, _ ->
                    val data = hashMapOf(
                        "title" to titleInput.text.toString(),
                        "description" to descInput.text.toString(),
                        "link" to linkInput.text.toString(),
                        "imageUrl" to imageInput.text.toString()
                    )

                    if (articleId == null) {
                        db.collection("articles").add(data)
                    } else {
                        db.collection("articles").document(articleId).set(data)
                    }
                    loadArticles()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }
}
