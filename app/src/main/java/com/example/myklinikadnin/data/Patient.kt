package com.example.myklinikadnin.data

data class Patient(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val nric: String = "",
    val bloodType: String = "",
    val bloodPressure: String = "",
    val height: Int = 0,
    val weight: Int = 0,
    val createdAt: Long = 0
)
