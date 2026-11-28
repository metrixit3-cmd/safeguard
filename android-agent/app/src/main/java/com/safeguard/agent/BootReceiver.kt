package com.safeguard.agent

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        SecureStore.initialize(context)
        if (intent.action != Intent.ACTION_BOOT_COMPLETED || !SecureStore.bool("location_consent") || SecureStore.agentToken().isBlank()) return
        runCatching { ContextCompat.startForegroundService(context, Intent(context, LocationForegroundService::class.java)) }
            .onFailure { Log.w("SafeguardAgent", "Location sharing could not resume after reboot; open SAFEGUARD Agent.", it) }
    }
}
