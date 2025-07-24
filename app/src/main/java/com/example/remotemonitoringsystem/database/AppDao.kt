package com.example.remotemonitoringsystem.database

import androidx.room.*

@Dao
interface AppDao {
    // Operaciones con SensorData
    @Query("SELECT * FROM sensor_data WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    suspend fun getSensorDataByTimeRange(startTime: Long, endTime: Long): List<SensorData>

    @Query("SELECT * FROM sensor_data ORDER BY timestamp DESC LIMIT 10")
    suspend fun getRecentSensorData(): List<SensorData>

    @Query("SELECT * FROM sensor_data ORDER BY timestamp DESC")
    suspend fun getAllSensorData(): List<SensorData>

    @Insert
    suspend fun insertSensorData(sensorData: SensorData)

    @Query("DELETE FROM sensor_data WHERE timestamp < :cutoffTime")
    suspend fun deleteOldData(cutoffTime: Long)

    @Query("DELETE FROM sensor_data")
    suspend fun deleteAllSensorData()

    // Operaciones con Credentials
    @Query("SELECT * FROM credentials LIMIT 1")
    suspend fun getCredentials(): Credentials?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCredentials(credentials: Credentials)

    @Update
    suspend fun updateCredentials(credentials: Credentials)

    @Query("DELETE FROM credentials")
    suspend fun deleteAllCredentials()

    // Consultas adicionales útiles
    @Query("SELECT COUNT(*) FROM sensor_data")
    suspend fun getSensorDataCount(): Int

    @Query("SELECT * FROM sensor_data ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestSensorData(): SensorData?

    @Query("SELECT * FROM sensor_data WHERE latitude BETWEEN :minLat AND :maxLat AND longitude BETWEEN :minLon AND :maxLon ORDER BY timestamp DESC")
    suspend fun getSensorDataByLocation(minLat: Double, maxLat: Double, minLon: Double, maxLon: Double): List<SensorData>
}