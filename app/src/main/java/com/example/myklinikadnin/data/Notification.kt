package com.example.myklinikadnin.data

data class Notification(
    val title: String = "",
    val message: String = "",
    val createdAt: Long = 0,
    val role: String = "All"
)
