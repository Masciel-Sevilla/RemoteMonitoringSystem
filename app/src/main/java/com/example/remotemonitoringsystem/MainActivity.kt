package com.example.remotemonitoringsystem

import android.app.AlarmManager
import android.app.AlertDialog
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.remotemonitoringsystem.database.AppDatabase
import com.example.remotemonitoringsystem.database.Credentials
import com.example.remotemonitoringsystem.server.ApiServer
import com.example.remotemonitoringsystem.service.LocationService
import com.example.remotemonitoringsystem.utils.NetworkUtils
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    // Controles
    private lateinit var modeSelectorGroup: RadioGroup
    private lateinit var continuousControls: LinearLayout
    private lateinit var scheduledControls: LinearLayout
    private lateinit var statusText: TextView

    // Modo Continuo
    private lateinit var continuousButton: Button

    // Modo Programado
    private lateinit var scheduleButton: Button
    private lateinit var startTimeButton: Button
    private lateinit var endTimeButton: Button
    private lateinit var configButton: Button // <-- VISTA AÑADIDA
    private val checkBoxes by lazy {
        mapOf(
            Calendar.SUNDAY to findViewById<CheckBox>(R.id.cbSunday),
            Calendar.MONDAY to findViewById<CheckBox>(R.id.cbMonday),
            Calendar.TUESDAY to findViewById<CheckBox>(R.id.cbTuesday),
            Calendar.WEDNESDAY to findViewById<CheckBox>(R.id.cbWednesday),
            Calendar.THURSDAY to findViewById<CheckBox>(R.id.cbThursday),
            Calendar.FRIDAY to findViewById<CheckBox>(R.id.cbFriday),
            Calendar.SATURDAY to findViewById<CheckBox>(R.id.cbSaturday)
        )
    }

    // Herramientas y Estado
    private lateinit var showDeviceStatusButton: Button
    private lateinit var deviceStatusText: TextView
    private lateinit var ipAddressText: TextView
    private lateinit var lastLocationText: TextView
    private lateinit var dataCountText: TextView

    // Dependencias
    private lateinit var database: AppDatabase
    private var apiServer: ApiServer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        database = AppDatabase.getInstance(this)

        initViews()
        setupListeners()
        initializeTokenIfNeeded()
        loadState()
    }

    override fun onResume() {
        super.onResume()
        updateInfoViews()
    }

    private fun initViews() {
        modeSelectorGroup = findViewById(R.id.modeSelectorGroup)
        continuousControls = findViewById(R.id.continuousControls)
        scheduledControls = findViewById(R.id.scheduledControls)
        statusText = findViewById(R.id.statusText)
        continuousButton = findViewById(R.id.continuousButton)
        scheduleButton = findViewById(R.id.scheduleButton)
        startTimeButton = findViewById(R.id.startTimeButton)
        endTimeButton = findViewById(R.id.endTimeButton)
        showDeviceStatusButton = findViewById(R.id.showDeviceStatusButton)
        deviceStatusText = findViewById(R.id.deviceStatusText)
        ipAddressText = findViewById(R.id.ipAddressText)
        lastLocationText = findViewById(R.id.lastLocationText)
        dataCountText = findViewById(R.id.dataCountText)
        configButton = findViewById(R.id.configButton)
    }

    private fun setupListeners() {
        modeSelectorGroup.setOnCheckedChangeListener { _, checkedId ->
            val selectedMode = if (checkedId == R.id.rbContinuous) "continuous" else "scheduled"
            saveMode(selectedMode)
            updateUIVisibility()
        }
        continuousButton.setOnClickListener { toggleContinuousService() }
        scheduleButton.setOnClickListener { toggleScheduledService() }
        startTimeButton.setOnClickListener { showTimePicker(true) }
        endTimeButton.setOnClickListener { showTimePicker(false) }
        showDeviceStatusButton.setOnClickListener { showDeviceStatus() }
        configButton.setOnClickListener { showTokenDialog() }
    }
    private fun initializeTokenIfNeeded() {
        lifecycleScope.launch {
            if (database.appDao().getCredentials() == null) {
                val defaultToken = UUID.randomUUID().toString()
                database.appDao().insertCredentials(Credentials(apiToken = defaultToken))
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Se ha generado un nuevo token de API.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    private fun showTokenDialog() {
        lifecycleScope.launch {
            val credentials = database.appDao().getCredentials()
            val token = credentials?.apiToken ?: "No se ha generado ningún token."

            AlertDialog.Builder(this@MainActivity)
                .setTitle("Tu Token de API")
                .setMessage(token)
                .setPositiveButton("Entendido", null)
                .show()
        }
    }

    private fun updateUIVisibility() {
        val prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        when (prefs.getString("collection_mode", "continuous")) {
            "continuous" -> {
                continuousControls.visibility = View.VISIBLE
                scheduledControls.visibility = View.GONE
            }
            "scheduled" -> {
                continuousControls.visibility = View.GONE
                scheduledControls.visibility = View.VISIBLE
            }
        }
        updateStatusText()
    }

    private fun toggleContinuousService() {
        val prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val isRunning = prefs.getBoolean("continuous_running", false)
        val serviceIntent = Intent(this, LocationService::class.java).apply {
            putExtra("MODE", "CONTINUOUS")
        }

        if (isRunning) {
            stopService(serviceIntent)
            stopApiServer()
            prefs.edit().putBoolean("continuous_running", false).apply()
            Toast.makeText(this, "Recolección continua detenida", Toast.LENGTH_SHORT).show()
        } else {
            ContextCompat.startForegroundService(this, serviceIntent)
            startApiServer()
            prefs.edit().putBoolean("continuous_running", true).apply()
            Toast.makeText(this, "Recolección continua iniciada", Toast.LENGTH_SHORT).show()
        }
        updateStatusText()
    }

    private fun toggleScheduledService() {
        val prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val isScheduled = prefs.getBoolean("schedule_active", false)

        if (isScheduled) {
            cancelAlarm()
            stopApiServer()
            prefs.edit().putBoolean("schedule_active", false).apply()
            Toast.makeText(this, "Horario desactivado", Toast.LENGTH_SHORT).show()
        } else {
            saveScheduleToPrefs()
            setRepeatingAlarm()
            startApiServer()
            prefs.edit().putBoolean("schedule_active", true).apply()
            Toast.makeText(this, "Horario activado", Toast.LENGTH_SHORT).show()
        }
        updateStatusText()
    }

    private fun saveMode(mode: String) {
        getSharedPreferences("AppPrefs", Context.MODE_PRIVATE).edit().putString("collection_mode", mode).apply()
    }

    private fun loadState() {
        val prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val mode = prefs.getString("collection_mode", "continuous")
        if (mode == "continuous") {
            modeSelectorGroup.check(R.id.rbContinuous)
        } else {
            modeSelectorGroup.check(R.id.rbScheduled)
        }

        checkBoxes.forEach { (day, checkBox) ->
            checkBox.isChecked = prefs.getBoolean("day_$day", false)
        }
        val startHour = prefs.getInt("start_hour", 8)
        val startMinute = prefs.getInt("start_minute", 0)
        val endHour = prefs.getInt("end_hour", 18)
        val endMinute = prefs.getInt("end_minute", 0)
        startTimeButton.text = String.format(Locale.getDefault(), "%02d:%02d", startHour, startMinute)
        endTimeButton.text = String.format(Locale.getDefault(), "%02d:%02d", endHour, endMinute)

        updateUIVisibility()
    }

    private fun updateStatusText() {
        val prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val mode = prefs.getString("collection_mode", "continuous")
        var status = "Servicio Inactivo"

        if (mode == "continuous" && prefs.getBoolean("continuous_running", false)) {
            status = "Modo Continuo: ACTIVO"
            continuousButton.text = "Detener Recolección Continua"
        } else {
            continuousButton.text = "Iniciar Recolección Continua"
        }

        if (mode == "scheduled" && prefs.getBoolean("schedule_active", false)) {
            status = "Modo Programado: ACTIVO"
            scheduleButton.text = "Desactivar Horario"
        } else {
            scheduleButton.text = "Activar Horario"
        }

        statusText.text = status
    }

    private fun saveScheduleToPrefs() {
        val prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE).edit()
        checkBoxes.forEach { (day, checkBox) ->
            prefs.putBoolean("day_$day", checkBox.isChecked)
        }
        val startTime = startTimeButton.text.toString().split(":")
        val endTime = endTimeButton.text.toString().split(":")
        prefs.putInt("start_hour", startTime[0].toInt())
        prefs.putInt("start_minute", startTime[1].toInt())
        prefs.putInt("end_hour", endTime[0].toInt())
        prefs.putInt("end_minute", endTime[1].toInt())
        prefs.apply()
    }

    private fun setRepeatingAlarm() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, SchedulingReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis(),
            AlarmManager.INTERVAL_FIFTEEN_MINUTES,
            pendingIntent
        )
    }

    private fun cancelAlarm() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, SchedulingReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        alarmManager.cancel(pendingIntent)
    }

    private fun showTimePicker(isStartTime: Boolean) {
        val buttonToUpdate = if (isStartTime) startTimeButton else endTimeButton
        val timeParts = buttonToUpdate.text.toString().split(":")
        val currentHour = timeParts[0].toInt()
        val currentMinute = timeParts[1].toInt()

        TimePickerDialog(this, { _, hourOfDay, minute ->
            buttonToUpdate.text = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute)
        }, currentHour, currentMinute, true).show()
    }

    private fun showDeviceStatus() {
        val batteryManager = getSystemService(BATTERY_SERVICE) as BatteryManager
        val batteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val stat = StatFs(Environment.getDataDirectory().path)
        val availableStorageMB = stat.availableBytes / (1024 * 1024)
        val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
        val isConnected = networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        val networkStatus = if (isConnected) "Conectado" else "Desconectado"

        val statusInfo = """
            Nivel de Batería: $batteryLevel%
            Modelo: ${Build.MODEL}
            Versión de Android: ${Build.VERSION.RELEASE}
            Almacenamiento Libre: ${availableStorageMB} MB
            Red: $networkStatus
        """.trimIndent()

        deviceStatusText.text = statusInfo
        Toast.makeText(this, "Estado del dispositivo actualizado", Toast.LENGTH_SHORT).show()
    }

    private fun updateInfoViews() {
        ipAddressText.text = "IP Local: ${NetworkUtils.getLocalIpAddress()}"

        lifecycleScope.launch {
            val count = database.appDao().getSensorDataCount()
            val latest = database.appDao().getLatestSensorData()

            dataCountText.text = "Registros guardados: $count"
            if (latest != null) {
                val sdf = SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault())
                val formattedDate = sdf.format(Date(latest.timestamp))
                lastLocationText.text = "Última ubicación: ${latest.latitude}, ${latest.longitude} ($formattedDate)"
            } else {
                lastLocationText.text = "Última ubicación: Ninguna"
            }
        }
    }

    private fun startApiServer() {
        if (apiServer == null) {
            try {
                apiServer = ApiServer(8080, this).apply { start() }
            } catch (e: Exception) {
                Toast.makeText(this, "Error al iniciar API Server: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun stopApiServer() {
        apiServer?.stop()
        apiServer = null
    }
}