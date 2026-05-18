package com.nostudios.bruceapp.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nostudios.bruceapp.data.model.BruceDevice
import kotlinx.coroutines.flow.Flow

@Dao
interface BruceDeviceDao {
    @Query("SELECT * FROM bruce_devices ORDER BY dateAdded ASC")
    fun getAllDevices(): Flow<List<BruceDevice>>

    @Query("SELECT * FROM bruce_devices ORDER BY dateAdded ASC")
    suspend fun getAllDevicesList(): List<BruceDevice>

    @Query("SELECT * FROM bruce_devices WHERE id = :id")
    suspend fun getDeviceById(id: String): BruceDevice?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevice(device: BruceDevice)

    @Delete
    suspend fun deleteDevice(device: BruceDevice)

    @Query("DELETE FROM bruce_devices WHERE id = :id")
    suspend fun deleteDeviceById(id: String)

    @Query("SELECT savedPin FROM bruce_devices WHERE id = :id")
    suspend fun getSavedPin(id: String): String?
}
