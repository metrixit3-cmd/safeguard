package com.safeguard.agent

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class SafeguardApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        SecureStore.initialize(this)
        Api.initialize(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(NotificationChannel("protection", getString(R.string.notification_channel_protection), NotificationManager.IMPORTANCE_HIGH))
            manager.createNotificationChannel(NotificationChannel("siren", getString(R.string.notification_channel_siren), NotificationManager.IMPORTANCE_HIGH))
            manager.createNotificationChannel(NotificationChannel("location", getString(R.string.notification_channel_location), NotificationManager.IMPORTANCE_LOW))
        }
    }
}
