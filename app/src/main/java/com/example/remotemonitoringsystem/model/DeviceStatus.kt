package com.example.remotemonitoringsystem.model

data class DeviceStatus(
    val batteryLevel: Int = 0,
    val deviceModel: String = "",
    val osVersion: String = "",
    val availableStorage: Long = 0,
    val networkConnected: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)