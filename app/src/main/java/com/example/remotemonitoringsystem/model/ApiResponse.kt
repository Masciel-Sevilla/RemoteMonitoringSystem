package com.example.remotemonitoringsystem.model

data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis()
)