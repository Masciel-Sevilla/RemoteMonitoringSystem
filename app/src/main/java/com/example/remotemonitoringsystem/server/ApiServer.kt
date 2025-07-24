package com.example.remotemonitoringsystem.server

import android.content.Context
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.remotemonitoringsystem.database.AppDatabase
import com.example.remotemonitoringsystem.model.ApiResponse
import com.example.remotemonitoringsystem.model.DeviceStatus
import com.google.gson.Gson
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.runBlocking

class ApiServer(port: Int, private val context: Context) : NanoHTTPD(port) {
    private val database = AppDatabase.getInstance(context)
    private val gson = Gson()

    override fun serve(session: IHTTPSession): Response {
        // La autenticación ahora se comprueba para todas las rutas
        if (!isAuthenticated(session)) {
            return createErrorResponse(Response.Status.UNAUTHORIZED, "No autorizado: Token de API inválido o no proporcionado.")
        }

        return try {
            when {
                session.uri == "/api/sensor_data" && session.method == Method.GET -> {
                    handleSensorDataRequest(session)
                }
                session.uri == "/api/device_status" && session.method == Method.GET -> {
                    handleDeviceStatusRequest()
                }
                else -> {
                    createErrorResponse(Response.Status.NOT_FOUND, "Endpoint no encontrado")
                }
            }
        } catch (e: Exception) {
            createErrorResponse(Response.Status.INTERNAL_ERROR, "Error interno del servidor: ${e.message}")
        }
    }

    private fun handleSensorDataRequest(session: IHTTPSession): Response {
        val params = session.parms
        // Se usan los parámetros 'start_time' y 'end_time' para filtrar por rango
        val startTime = params["start_time"]?.toLongOrNull() ?: 0L
        val endTime = params["end_time"]?.toLongOrNull() ?: System.currentTimeMillis()

        return runBlocking {
            try {
                val data = database.appDao().getSensorDataByTimeRange(startTime, endTime)
                val response = ApiResponse(success = true, data = data)
                createJsonResponse(Response.Status.OK, response)
            } catch (e: Exception) {
                createErrorResponse(Response.Status.INTERNAL_ERROR, "Error al obtener datos: ${e.message}")
            }
        }
    }

    private fun handleDeviceStatusRequest(): Response {
        return try {
            val deviceStatus = getDeviceStatus()
            val response = ApiResponse(success = true, data = deviceStatus)
            createJsonResponse(Response.Status.OK, response)
        } catch (e: Exception) {
            createErrorResponse(Response.Status.INTERNAL_ERROR, "Error al obtener estado: ${e.message}")
        }
    }

    private fun isAuthenticated(session: IHTTPSession): Boolean {
        val authHeader = session.headers["authorization"]
        if (authHeader == null || !authHeader.lowercase().startsWith("bearer ")) {
            return false
        }

        val token = authHeader.substring(7)
        return runBlocking {
            try {
                // Se comprueba si el token proporcionado coincide con el almacenado
                val storedCredentials = database.appDao().getCredentials()
                storedCredentials?.apiToken == token && token.isNotBlank()
            } catch (e: Exception) {
                false
            }
        }
    }

    private fun getDeviceStatus(): DeviceStatus {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val batteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)

        val stat = StatFs(Environment.getDataDirectory().path)
        val availableBytes = stat.availableBytes

        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
        val isConnected = networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

        return DeviceStatus(
            batteryLevel = batteryLevel,
            deviceModel = Build.MODEL,
            osVersion = Build.VERSION.RELEASE,
            availableStorage = availableBytes,
            networkConnected = isConnected
        )
    }

    private fun createJsonResponse(status: Response.Status, data: Any): Response {
        val json = gson.toJson(data)
        return newFixedLengthResponse(status, "application/json", json)
    }

    private fun createErrorResponse(status: Response.Status, message: String): Response {
        val response = ApiResponse<Nothing>(success = false, message = message)
        return createJsonResponse(status, response)
    }
}