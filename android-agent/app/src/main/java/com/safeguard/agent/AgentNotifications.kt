package com.safeguard.agent

import android.content.Context
import androidx.core.app.NotificationCompat

object AgentNotifications {
    fun builder(context: Context, channel: String) = NotificationCompat.Builder(context, channel)
        .setSmallIcon(R.drawable.ic_safeguard)
        .setColor(0xFF25D6C5.toInt())
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

    fun basic(context: Context, title: String, body: String) = builder(context, "protection")
        .setContentTitle(title).setContentText(body).setOngoing(false).setAutoCancel(true).build()
}
