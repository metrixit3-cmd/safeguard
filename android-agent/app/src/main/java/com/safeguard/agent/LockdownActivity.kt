package com.safeguard.agent

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class LockdownActivity : ComponentActivity() {
    companion object { const val ACTION_REMOTE_UNLOCK = "com.safeguard.agent.REMOTE_UNLOCK" }
    private var email by mutableStateOf("")
    private var password by mutableStateOf("")
    private var status by mutableStateOf("Enter the SAFEGUARD account password paired with this phone to verify ownership.")
    private val unlockReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) { runCatching { stopLockTask() }; finish() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (android.os.Build.VERSION.SDK_INT >= 27) { setShowWhenLocked(true); setTurnScreenOn(true) }
        val filter = IntentFilter(ACTION_REMOTE_UNLOCK)
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            registerReceiver(unlockReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(unlockReceiver, filter)
        }
        EnrollmentManager(this).enterLockTask(this)
        email = SecureStore.get("owner_email")
        setContent {
            BackHandler(enabled = true) {}
            MaterialTheme(colorScheme = darkColorScheme(primary = Color.White, background = Color(0xFF660B16), surface = Color(0xFF8B1020))) {
                Surface(Modifier.fillMaxSize(), color = Color(0xFF660B16)) {
                    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
                        Text("THEFT LOCKDOWN", style = MaterialTheme.typography.headlineLarge, color = Color.White)
                        Text("This phone is protected by its SAFEGUARD owner.", color = Color.White, modifier = Modifier.padding(top = 12.dp, bottom = 26.dp))
                        OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth(), label = { Text("Owner email") }, singleLine = true)
                        OutlinedTextField(password, { password = it }, Modifier.fillMaxWidth().padding(top = 10.dp), label = { Text("Account password") }, visualTransformation = PasswordVisualTransformation(), singleLine = true)
                        Button(onClick = ::verifyOwner, Modifier.fillMaxWidth().padding(top = 18.dp), enabled = password.isNotBlank()) { Text("Verify and unlock") }
                        Text(status, color = Color.White, modifier = Modifier.padding(top = 18.dp))
                    }
                }
            }
        }
    }

    private fun verifyOwner() {
        val expected = SecureStore.get("owner_email").trim().lowercase()
        if (email.trim().lowercase() != expected || expected.isBlank()) { status = "Use the SAFEGUARD account email paired with this phone."; return }
        lifecycleScope.launch {
            status = "Verifying account…"
            try {
                val response = Api.service.login(LoginRequest(expected, password))
                password = ""
                if (!response.isSuccessful) throw IllegalStateException(Api.error(response))
                EnrollmentManager(this@LockdownActivity).removeLockdown()
                runCatching { stopLockTask() }
                finish()
            } catch (error: Exception) { status = error.message ?: "Could not verify. Check the network and try again." }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN && SecureStore.armed()) return true
        return super.onKeyDown(keyCode, event)
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(unlockReceiver) }
        super.onDestroy()
    }
}
