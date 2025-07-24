package com.example.remotemonitoringsystem.service

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.*
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.example.remotemonitoringsystem.R
import com.example.remotemonitoringsystem.database.AppDatabase
import com.example.remotemonitoringsystem.database.SensorData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LocationService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var collectionRunnable: Runnable

    companion object {
        const val LOCATION_UPDATE_INTERVAL_MS = 30000L // 30 segundos para modo continuo
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        startForeground(1, createNotification())

        val mode = intent?.getStringExtra("MODE")

        if (mode == "CONTINUOUS") {
            // Inicia recolección repetitiva
            setupContinuousCollection()
            handler.post(collectionRunnable)
        } else {
            // Modo programado: recolecta una vez y se detiene
            requestSingleLocationUpdate()
        }

        return START_STICKY
    }

    private fun setupContinuousCollection() {
        collectionRunnable = Runnable {
            requestSingleLocationUpdate(stopAfter = false) // No se detiene
            handler.postDelayed(collectionRunnable, LOCATION_UPDATE_INTERVAL_MS)
        }
    }

    private fun requestSingleLocationUpdate(stopAfter: Boolean = true) {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (stopAfter) stopSelf()
            return
        }

        val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

        location?.let {
            saveSensorData(it, stopAfter)
        } ?: run { if (stopAfter) stopSelf() }
    }

    private fun saveSensorData(location: android.location.Location, stopAfter: Boolean) {
        val deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        val sensorData = SensorData(deviceId = deviceId, latitude = location.latitude, longitude = location.longitude, timestamp = System.currentTimeMillis())

        serviceScope.launch {
            AppDatabase.getInstance(applicationContext).appDao().insertSensorData(sensorData)
            if (stopAfter) {
                stopSelf()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Asegurarse de remover los callbacks para el modo continuo
        if(::collectionRunnable.isInitialized) {
            handler.removeCallbacks(collectionRunnable)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, "LocationServiceChannel")
            .setContentTitle("Recolectando ubicación...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "LocationServiceChannel",
                "Location Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
