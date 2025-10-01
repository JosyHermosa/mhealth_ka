package com.example.myklinikadnin.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myklinikadnin.R
import com.example.myklinikadnin.data.Notification
import java.text.SimpleDateFormat
import java.util.*

class NotificationAdapter(private val notifications: List<Notification>) :
    RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.textNotifTitle)
        val message: TextView = itemView.findViewById(R.id.textNotifMessage)
        val date: TextView = itemView.findViewById(R.id.textNotifDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val notification = notifications[position]
        holder.title.text = notification.title
        holder.message.text = notification.message

        // format createdAt -> readable date
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        holder.date.text = sdf.format(Date(notification.createdAt))
    }

    override fun getItemCount(): Int = notifications.size
}
