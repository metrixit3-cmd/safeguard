package com.safeguard.agent

import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SafeguardFirebaseMessagingService : FirebaseMessagingService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        SecureStore.initialize(this)
        if (SecureStore.deviceId().isNotBlank() && SecureStore.agentToken().isNotBlank()) {
            scope.launch { runCatching { Api.service.updateFcmToken(mapOf("token" to token)) } }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        SecureStore.initialize(this)
        val action = message.data["action"] ?: message.data["command"] ?: message.data["type"].orEmpty()
        val enabled = message.data["enabled"]?.toBooleanStrictOrNull() ?: action.endsWith("_ON")
        val commandId = message.data["commandId"].orEmpty()
        val lockOn = action in setOf("THEFT_LOCK_ON", "full_lockdown_anti_flash") && enabled || action == "theft_mode" && enabled
        val lockOff = action in setOf("THEFT_LOCK_OFF", "full_lockdown_anti_flash") && !enabled || action == "theft_mode" && !enabled
        val sirenOn = action in setOf("SIREN_ON", "maximum_volume_siren") && enabled
        val sirenOff = action in setOf("SIREN_OFF", "maximum_volume_siren") && !enabled

        when {
            lockOn -> {
                val problems = EnrollmentManager(this).applyLockdown()
                showLockdownNotification(problems.joinToString().ifBlank { "Owner-authorized lockdown is active." })
            }
            lockOff -> EnrollmentManager(this).removeLockdown()
            sirenOn -> runCatching { ContextCompat.startForegroundService(this, Intent(this, SirenService::class.java)) }
                .onFailure { Log.w("SafeguardAgent", "Siren foreground service could not start.", it) }
            sirenOff -> stopService(Intent(this, SirenService::class.java))
            action == "sim_tamper_alert" || action == "simTamperAlertEnabled" -> SecureStore.putBool("sim_guard_enabled", enabled)
            action == "live_gps_polling" && enabled -> startLocationIfConsented(immediate = true)
            action == "live_gps_polling" && !enabled -> stopService(Intent(this, LocationForegroundService::class.java))
            action == "locate_now" -> startLocationIfConsented(immediate = true)
            else -> Log.i("SafeguardAgent", "Ignoring unsupported command: $action")
        }
        if (commandId.isNotBlank()) scope.launch { runCatching { Api.service.acknowledge(AckRequest(commandId)) } }
    }

    private fun startLocationIfConsented(immediate: Boolean) {
        if (!SecureStore.bool("location_consent")) {
            getSystemService(android.app.NotificationManager::class.java).notify(301, AgentNotifications.basic(this, "Location sharing is paused", "Open SAFEGUARD Agent to enable location sharing."))
            return
        }
        runCatching { ContextCompat.startForegroundService(this, Intent(this, LocationForegroundService::class.java).putExtra(LocationForegroundService.EXTRA_IMMEDIATE, immediate)) }
            .onFailure { Log.w("SafeguardAgent", "Location service could not start", it) }
    }

    private fun showLockdownNotification(details: String) {
        val intent = Intent(this, LockdownActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pending = android.app.PendingIntent.getActivity(this, 41, intent, android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE)
        val notification = AgentNotifications.builder(this, "protection")
            .setContentTitle(getString(R.string.lockdown_notification_title))
            .setContentText(details.take(160))
            .setCategory(androidx.core.app.NotificationCompat.CATEGORY_ALARM)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_MAX)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(pending, true)
            .setContentIntent(pending)
            .build()
        getSystemService(android.app.NotificationManager::class.java).notify(401, notification)
    }
}
