package com.nostudios.bruceapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "bruce_devices")
data class BruceDevice(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val category: String = "Detected Hardware",
    val dateAdded: Long = System.currentTimeMillis(),
    val savedPin: String? = null
) {
    val imageName: String
        get() = name.lowercase()
            .replace(" ", "_")
            .replace("-", "_")
            .replace("(", "")
            .replace(")", "")
            .replace("\"", "")
}
