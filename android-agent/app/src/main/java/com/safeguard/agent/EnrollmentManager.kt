package com.safeguard.agent

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.UserManager

class EnrollmentManager(private val context: Context) {
    private val policy = context.getSystemService(DevicePolicyManager::class.java)
    private val admin = ComponentName(context, SafeguardDeviceAdminReceiver::class.java)

    fun isDeviceOwner(): Boolean = policy.isDeviceOwnerApp(context.packageName)
    fun isAdminActive(): Boolean = policy.isAdminActive(admin)

    fun activationIntent(): Intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
        putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin)
        putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Allow SAFEGUARD to lock your phone when you activate Theft Lockdown.")
    }

    fun applyLockdown(): List<String> {
        val issues = mutableListOf<String>()
        if (isAdminActive()) runCatching { policy.lockNow() }.onFailure { issues += "Screen lock failed: ${it.message}" }
        SecureStore.putBool("protection_armed", true)
        if (!isDeviceOwner()) return issues
        val restrictions = listOf(
            UserManager.DISALLOW_USB_FILE_TRANSFER,
            UserManager.DISALLOW_DEBUGGING_FEATURES,
            UserManager.DISALLOW_FACTORY_RESET,
            UserManager.DISALLOW_SAFE_BOOT
        )
        restrictions.forEach { restriction ->
            runCatching { policy.addUserRestriction(admin, restriction) }
                .onFailure { issues += "Could not apply $restriction: ${it.message}" }
        }
        runCatching {
            policy.setLockTaskPackages(admin, arrayOf(context.packageName))
            if (android.os.Build.VERSION.SDK_INT >= 28) policy.setLockTaskFeatures(admin, DevicePolicyManager.LOCK_TASK_FEATURE_NONE)
        }.onFailure { issues += "Kiosk policy unavailable: ${it.message}" }
        return issues
    }

    fun removeLockdown() {
        if (isDeviceOwner()) {
            listOf(UserManager.DISALLOW_USB_FILE_TRANSFER, UserManager.DISALLOW_DEBUGGING_FEATURES,
                UserManager.DISALLOW_FACTORY_RESET, UserManager.DISALLOW_SAFE_BOOT)
                .forEach { restriction -> runCatching { policy.clearUserRestriction(admin, restriction) } }
            runCatching { policy.setLockTaskPackages(admin, emptyArray()) }
        }
        SecureStore.putBool("protection_armed", false)
        context.stopService(Intent(context, SirenService::class.java))
        context.sendBroadcast(Intent(LockdownActivity.ACTION_REMOTE_UNLOCK).setPackage(context.packageName))
    }

    fun enterLockTask(activity: android.app.Activity): Boolean {
        if (!isDeviceOwner() || !policy.isLockTaskPermitted(context.packageName)) return false
        return runCatching { activity.startLockTask(); true }.getOrDefault(false)
    }
}
