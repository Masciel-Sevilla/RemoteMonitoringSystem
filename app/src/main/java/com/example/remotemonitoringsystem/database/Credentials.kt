package com.example.remotemonitoringsystem.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "credentials")
data class Credentials(
    @PrimaryKey
    val id: Long = 1, // Usamos un ID fijo para que siempre sea el mismo registro

    @ColumnInfo(name = "api_token")
    val apiToken: String
)