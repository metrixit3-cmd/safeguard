package com.safeguard.agent

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SimStateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        SecureStore.initialize(context)
        if (!SecureStore.armed() || !SecureStore.bool("sim_guard_enabled")) return
        val state = intent.getStringExtra("ss") ?: intent.getStringExtra(TelephonyManager.EXTRA_STATE).orEmpty()
        if (state !in setOf("ABSENT", "CARD_IO_ERROR", "NOT_READY")) return
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                EnrollmentManager(context).applyLockdown()
                context.getSystemService(android.app.NotificationManager::class.java).notify(402,
                    AgentNotifications.basic(context, "SIM change detected", "SAFEGUARD detected that the SIM is unavailable while protection is armed."))
                val deviceId = SecureStore.deviceId()
                if (deviceId.isNotBlank() && SecureStore.agentToken().isNotBlank()) {
                    val response = Api.service.reportSimAlert(deviceId, mapOf("state" to state, "reportedAt" to java.time.Instant.now().toString()))
                    if (!response.isSuccessful) Log.w("SafeguardAgent", "SIM alert endpoint unavailable (${response.code()}); backend route is required.")
                }
            } catch (error: Exception) { Log.e("SafeguardAgent", "Could not process SIM state alert.", error) }
            finally { pending.finish() }
        }
    }
}
