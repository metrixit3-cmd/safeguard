package com.safeguard.agent

import android.Manifest
import android.app.admin.DevicePolicyManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.provider.Settings.Secure
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID
import java.security.MessageDigest

class MainActivity : ComponentActivity() {
    private val enrollment by lazy { EnrollmentManager(this) }
    private var email by mutableStateOf("")
    private var password by mutableStateOf("")
    private var deviceName by mutableStateOf("${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}".take(60))
    private var message by mutableStateOf("Sign in with the SAFEGUARD account that owns this phone.")
    private var busy by mutableStateOf(false)
    private var signedIn by mutableStateOf(false)
    private var enrollmentStatus by mutableStateOf("Checking enrollment…")
    private val locationPermission = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        val granted = results[Manifest.permission.ACCESS_FINE_LOCATION] == true || results[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) requestBackgroundLocation() else message = "Location permission is required before location sharing can start."
    }
    private val backgroundPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) startLocationService() else message = "Allow background location in system settings to keep sharing while the screen is off."
    }
    private val simPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        SecureStore.putBool("sim_guard_enabled", granted)
        message = if (granted) "SIM-change alerts are enabled while Theft Mode is armed." else "SIM-change permission was not granted."
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        signedIn = SecureStore.agentToken().isNotBlank() && SecureStore.deviceId().isNotBlank()
        refreshEnrollmentStatus()
        if (signedIn) lifecycleScope.launch {
            val state = runCatching { Api.service.state() }.getOrNull()
            if (state?.code() == 401) {
                SecureStore.clearAgent()
                signedIn = false
                message = "Your phone session expired. Sign in again to reconnect this device."
            } else if (state == null) {
                message = "Could not check the saved phone session. Check network access; saved credentials were kept."
            }
        }
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = Color(0xFF25D6C5), background = Color(0xFF07111F), surface = Color(0xFF101D2D))) {
                Surface(Modifier.fillMaxSize(), color = Color(0xFF07111F)) {
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("SAFEGUARD", style = MaterialTheme.typography.labelLarge, color = Color(0xFF25D6C5))
                        Text("Your phone.\nYour control.", style = MaterialTheme.typography.headlineLarge, color = Color.White)
                        Text("Pair this Android phone with your SAFEGUARD account. Device Owner enrollment unlocks supported management policies.", color = Color(0xFFB5C2D4))
                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF101D2D))) {
                            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(if (signedIn) "Phone linked" else "Owner sign in", style = MaterialTheme.typography.titleLarge)
                                if (!signedIn) {
                                    OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth(), label = { Text("Email") }, singleLine = true)
                                    OutlinedTextField(password, { password = it }, Modifier.fillMaxWidth(), label = { Text("Password") }, visualTransformation = PasswordVisualTransformation(), singleLine = true)
                                    OutlinedTextField(deviceName, { deviceName = it.take(60) }, Modifier.fillMaxWidth(), label = { Text("Device name") }, singleLine = true)
                                    Button(onClick = ::signInAndRegister, Modifier.fillMaxWidth(), enabled = !busy) { Text(if (busy) "Connecting…" else "Sign in and connect phone") }
                                } else {
                                    Text("${SecureStore.deviceName()} · Android ${Build.VERSION.RELEASE}", color = Color(0xFFB5C2D4))
                                    Text("Enrollment: $enrollmentStatus")
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(onClick = { runCatching { startActivity(enrollment.activationIntent()) }.onFailure { message = it.message ?: "Could not open admin setup." } }) { Text("Activate Device Admin") }
                                        OutlinedButton(onClick = ::beginLocationPermissions) { Text("Start GPS sharing") }
                                    }
                                    OutlinedButton(onClick = { openNotificationPolicyAccess() }, Modifier.fillMaxWidth()) { Text("Allow alarm / Do Not Disturb access") }
                                    if (Build.VERSION.SDK_INT >= 34) OutlinedButton(onClick = ::openFullScreenAlertAccess, Modifier.fillMaxWidth()) { Text("Allow full-screen lockdown alerts") }
                                    OutlinedButton(onClick = { simPermission.launch(Manifest.permission.READ_PHONE_STATE) }, Modifier.fillMaxWidth()) { Text("Enable SIM-change guard") }
                                    OutlinedButton(onClick = ::unpair, Modifier.fillMaxWidth()) { Text("Sign out and unpair") }
                                }
                            }
                        }
                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF101D2D))) {
                            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Enrollment options", style = MaterialTheme.typography.titleMedium)
                                Text("Standard Device Admin can request a screen lock. Device Owner mode is required for USB/debugging, factory-reset, safe-boot, and kiosk restrictions.", color = Color(0xFFB5C2D4))
                                TextButton(onClick = ::showProvisioningInstructions) { Text("Show Device Owner ADB instructions") }
                            }
                        }
                        Text(message, color = Color(0xFFB5C2D4), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }

    private fun signInAndRegister() {
        val cleanEmail = email.trim().lowercase()
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches() || password.isBlank() || deviceName.trim().length < 2) {
            message = "Enter a valid email, password, and device name."; return
        }
        busy = true
        lifecycleScope.launch {
            try {
                val login = Api.service.login(LoginRequest(cleanEmail, password))
                if (!login.isSuccessful) throw IllegalStateException(Api.error(login))
                SecureStore.put("owner_email", cleanEmail)
                val clientKey = SecureStore.get("client_key").ifBlank { stableClientKey().also { SecureStore.put("client_key", it) } }
                val paired = Api.service.pair(PairRequest(cleanEmail, password, deviceName.trim(), clientKey = clientKey))
                if (!paired.isSuccessful) throw IllegalStateException(Api.error(paired))
                val deviceId = paired.body()?.device?.id ?: throw IllegalStateException("The server did not return a device ID.")
                SecureStore.put("device_id", deviceId)
                SecureStore.put("device_name", paired.body()?.device?.name ?: deviceName.trim())
                SecureStore.put("owner_email", cleanEmail)
                val fcmToken = FirebaseMessaging.getInstance().token.await()
                if (fcmToken.length < 20) throw IllegalStateException("Firebase did not provide a push token. Check google-services.json and Firebase setup.")
                val registration = Api.service.registerAgent(RegisterAgentRequest(
                    deviceId = deviceId,
                    osVersion = "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
                    fcmToken = fcmToken,
                    enrollmentMode = if (enrollment.isDeviceOwner()) "DEVICE_OWNER" else "STANDARD"
                ))
                if (!registration.isSuccessful) throw IllegalStateException(Api.error(registration))
                val token = registration.body()?.agentToken.orEmpty()
                if (token.isBlank()) throw IllegalStateException("Registration succeeded but no phone token was returned.")
                SecureStore.put("agent_token", token)
                signedIn = true
                password = ""
                message = "Phone linked and push registration completed. Grant location and notification permissions to continue."
            } catch (error: Exception) {
                message = error.message ?: "Could not connect to SAFEGUARD. Check your network and app setup."
            } finally { busy = false; password = "" }
        }
    }

    private fun beginLocationPermissions() {
        val permissions = mutableListOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
        if (Build.VERSION.SDK_INT >= 33) permissions += Manifest.permission.POST_NOTIFICATIONS
        locationPermission.launch(permissions.toTypedArray())
    }

    private fun requestBackgroundLocation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            backgroundPermission.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        } else startLocationService()
    }

    private fun startLocationService() {
        SecureStore.putBool("location_consent", true)
        val intent = Intent(this, LocationForegroundService::class.java).putExtra(LocationForegroundService.EXTRA_IMMEDIATE, true)
        runCatching { ContextCompat.startForegroundService(this, intent); message = "GPS sharing is active. Android displays an ongoing location notification." }
            .onFailure { message = "Could not start GPS sharing: ${it.message}" }
    }

    private fun openNotificationPolicyAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) runCatching { startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)) }
            .onFailure { message = "Open system settings to grant notification policy access." }
    }

    private fun openFullScreenAlertAccess() {
        if (Build.VERSION.SDK_INT >= 34) runCatching {
            startActivity(Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:$packageName")))
        }.onFailure { message = "Open this app's notification settings and allow full-screen alerts." }
    }

    private fun stableClientKey(): String {
        // Android ID is scoped/resettable and avoids collecting IMEI, serial, or phone number.
        val androidId = Secure.getString(contentResolver, Secure.ANDROID_ID).orEmpty()
        val bytes = MessageDigest.getInstance("SHA-256").digest("${BuildConfig.APPLICATION_ID}:$androidId".toByteArray())
        bytes[6] = ((bytes[6].toInt() and 0x0f) or 0x40).toByte()
        bytes[8] = ((bytes[8].toInt() and 0x3f) or 0x80).toByte()
        val buffer = java.nio.ByteBuffer.wrap(bytes)
        return UUID(buffer.long, buffer.long).toString()
    }

    private fun unpair() {
        lifecycleScope.launch {
            val serverUnpaired = runCatching { Api.service.unpair().isSuccessful }.getOrDefault(false)
            stopService(Intent(this@MainActivity, LocationForegroundService::class.java))
            stopService(Intent(this@MainActivity, SirenService::class.java))
            SecureStore.clearAgent()
            signedIn = false
            message = if (serverUnpaired) "This phone was unpaired from the SAFEGUARD account." else "Local agent credentials were cleared, but the server could not confirm unpairing. Reconnect and unpair again to revoke its remote token."
        }
    }

    override fun onResume() {
        super.onResume()
        refreshEnrollmentStatus()
    }

    private fun refreshEnrollmentStatus() {
        enrollmentStatus = when {
            enrollment.isDeviceOwner() -> "Device Owner"
            enrollment.isAdminActive() -> "Device Admin (screen lock only)"
            else -> "Not activated"
        }
    }

    private fun showProvisioningInstructions() {
        val component = "adb shell dpm set-device-owner com.safeguard.agent/.SafeguardDeviceAdminReceiver"
        android.app.AlertDialog.Builder(this)
            .setTitle("Device Owner enrollment")
            .setMessage("For development devices only: install this APK on a device with no accounts and no existing Device Owner, then run:\n\n$component\n\nThis is a factory-setup level enrollment. It cannot be granted by the normal sign-in screen. Production deployment should use Android Enterprise QR or zero-touch enrollment.")
            .setPositiveButton("Copy ADB command") { _, _ ->
                val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                clipboard.setPrimaryClip(android.content.ClipData.newPlainText("SAFEGUARD Device Owner command", component))
                message = "ADB command copied. Run it only on an eligible development device during setup."
            }
            .setNegativeButton("Close", null).show()
    }
}
