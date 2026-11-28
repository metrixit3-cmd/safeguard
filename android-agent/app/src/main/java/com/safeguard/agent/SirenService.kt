package com.safeguard.agent

import android.app.Service
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.app.NotificationManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.ServiceCompat

class SirenService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private var tone: ToneGenerator? = null
    private var oldAlarmVolume: Int? = null
    private var oldInterruptionFilter: Int? = null
    private val audio by lazy { getSystemService(AUDIO_SERVICE) as AudioManager }
    private val pulse = object : Runnable {
        override fun run() {
            runCatching { tone?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 1400) }
            handler.postDelayed(this, 1550)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = AgentNotifications.builder(this, "siren")
            .setContentTitle("SAFEGUARD alarm active")
            .setContentText("The phone owner has activated the device siren.")
            .setOngoing(true).setAutoCancel(false).build()
        val type = if (Build.VERSION.SDK_INT >= 29) android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else 0
        ServiceCompat.startForeground(this, 120, notification, type)
        runCatching {
            val savedVolume = SecureStore.get("siren_original_alarm_volume")
            if (savedVolume.isNotBlank()) runCatching { audio.setStreamVolume(AudioManager.STREAM_ALARM, savedVolume.toInt(), 0) }
            oldAlarmVolume = audio.getStreamVolume(AudioManager.STREAM_ALARM)
            SecureStore.put("siren_original_alarm_volume", oldAlarmVolume.toString())
            audio.setStreamVolume(AudioManager.STREAM_ALARM, audio.getStreamMaxVolume(AudioManager.STREAM_ALARM), 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val notifications = getSystemService(NotificationManager::class.java)
                if (notifications.isNotificationPolicyAccessGranted) {
                    SecureStore.get("siren_original_interruption_filter").takeIf(String::isNotBlank)?.toIntOrNull()?.let { previous ->
                        runCatching { notifications.setInterruptionFilter(previous) }
                    }
                    oldInterruptionFilter = notifications.currentInterruptionFilter
                    SecureStore.put("siren_original_interruption_filter", oldInterruptionFilter.toString())
                    notifications.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
                }
            }
            if (tone == null) tone = ToneGenerator(AudioManager.STREAM_ALARM, 100)
            handler.removeCallbacks(pulse)
            pulse.run()
        }.onFailure { Log.e("SafeguardAgent", "Siren playback could not start", it) }
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(pulse)
        runCatching { tone?.stopTone(); tone?.release() }
        tone = null
        oldAlarmVolume?.let { runCatching { audio.setStreamVolume(AudioManager.STREAM_ALARM, it, 0) } }
        SecureStore.put("siren_original_alarm_volume", "")
        oldAlarmVolume = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) oldInterruptionFilter?.let { previous -> runCatching { getSystemService(NotificationManager::class.java).setInterruptionFilter(previous) } }
        SecureStore.put("siren_original_interruption_filter", "")
        oldInterruptionFilter = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
