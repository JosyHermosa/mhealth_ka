package com.example.myklinikadnin.ui.staff

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myklinikadnin.R

class StaffNotificationAdapter(private val items: List<Map<String, Any>>) :
    RecyclerView.Adapter<StaffNotificationAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.textNotifTitle)
        val message: TextView = view.findViewById(R.id.textNotifMessage)
        val date: TextView = view.findViewById(R.id.textNotifDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val notif = items[position]
        holder.title.text = notif["title"] as? String ?: ""
        holder.message.text = notif["message"] as? String ?: ""

        val createdAt = notif["createdAt"]
        val dateText = when (createdAt) {
            is Long -> java.text.SimpleDateFormat("d/M/yyyy h:mm a")
                .format(java.util.Date(createdAt))
            is com.google.firebase.Timestamp -> java.text.SimpleDateFormat("d/M/yyyy h:mm a")
                .format(createdAt.toDate())
            else -> ""
        }
        holder.date.text = dateText
    }
}
