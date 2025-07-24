package com.example.remotemonitoringsystem

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.example.remotemonitoringsystem.service.LocationService
import java.util.*

class SchedulingReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences("SchedulePrefs", Context.MODE_PRIVATE)
        val isScheduleActive = prefs.getBoolean("is_active", false)

        if (!isScheduleActive) return // Si el horario no está activo, no hacemos nada

        val calendar = Calendar.getInstance()
        val currentDay = calendar.get(Calendar.DAY_OF_WEEK)
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)

        val isDayEnabled = prefs.getBoolean("day_$currentDay", false)
        if (!isDayEnabled) return // Si hoy no es un día seleccionado, no hacemos nada

        val startHour = prefs.getInt("start_hour", -1)
        val startMinute = prefs.getInt("start_minute", -1)
        val endHour = prefs.getInt("end_hour", -1)
        val endMinute = prefs.getInt("end_minute", -1)

        val startTimeInMinutes = startHour * 60 + startMinute
        val endTimeInMinutes = endHour * 60 + endMinute
        val currentTimeInMinutes = currentHour * 60 + currentMinute

        // Comprobamos si la hora actual está dentro del rango
        if (currentTimeInMinutes in startTimeInMinutes..endTimeInMinutes) {
            // ¡Es hora de trabajar! Iniciamos el servicio de localización.
            val serviceIntent = Intent(context, LocationService::class.java)
            ContextCompat.startForegroundService(context, serviceIntent)
        }
    }
}