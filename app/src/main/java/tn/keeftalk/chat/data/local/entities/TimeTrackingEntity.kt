package tn.keeftalk.chat.data.local.entities

import androidx.room.Entity

@Entity(
    tableName = "time_tracking_logs",
    primaryKeys = ["epochDay", "featureKey"]
)
data class TimeTrackingEntity(
    val epochDay: Long,
    val featureKey: String,
    val durationSeconds: Long = 0L,
    val pendingSyncSeconds: Long = 0L,
    val updatedAt: Long = System.currentTimeMillis()
)
