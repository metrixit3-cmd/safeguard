package com.safeguard.agent

import android.Manifest
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LocationForegroundService : Service() {
    companion object { const val EXTRA_IMMEDIATE = "immediate"; private const val NOTIFICATION_ID = 110 }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val client by lazy { LocationServices.getFusedLocationProviderClient(this) }
    private var callback: LocationCallback? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = AgentNotifications.builder(this, "location")
            .setContentTitle("SAFEGUARD location sharing")
            .setContentText("Sharing this phone’s location with your account")
            .setOngoing(true).setOnlyAlertOnce(true).build()
        val type = if (Build.VERSION.SDK_INT >= 29) android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION else 0
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, type)
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!fine && !coarse) { stopSelf(startId); return START_NOT_STICKY }
        if (SecureStore.agentToken().isBlank()) { stopSelf(startId); return START_NOT_STICKY }

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 30_000L)
            .setMinUpdateIntervalMillis(15_000L).setWaitForAccurateLocation(false).build()
        callback?.let { client.removeLocationUpdates(it) }
        callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    scope.launch {
                        val response = Api.service.location(LocationRequest(location.latitude, location.longitude, location.accuracy))
                        if (!response.isSuccessful) Log.w("SafeguardAgent", "GPS report rejected (${response.code()}).")
                    }
                }
            }
        }
        try {
            client.requestLocationUpdates(request, callback!!, mainLooper)
            if (intent?.getBooleanExtra(EXTRA_IMMEDIATE, false) == true) {
                scope.launch {
                    runCatching { client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).await() }
                        .onSuccess { location -> if (location != null) Api.service.location(LocationRequest(location.latitude, location.longitude, location.accuracy)) }
                        .onFailure { Log.w("SafeguardAgent", "Could not obtain immediate location.", it) }
                }
            }
        } catch (error: SecurityException) {
            Log.w("SafeguardAgent", "Location permission was revoked.", error)
            stopSelf(startId)
        }
        return START_STICKY
    }

    override fun onDestroy() {
        callback?.let { runCatching { client.removeLocationUpdates(it) } }
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
