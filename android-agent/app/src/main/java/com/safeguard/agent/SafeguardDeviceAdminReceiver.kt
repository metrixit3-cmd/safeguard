package com.safeguard.agent

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent

class SafeguardDeviceAdminReceiver : DeviceAdminReceiver() {
    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        SecureStore.initialize(context)
    }
}
