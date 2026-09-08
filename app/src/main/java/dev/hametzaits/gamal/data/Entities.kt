package dev.hametzaits.gamal.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val role: String, // "user" or "agent"
    val text: String,
    val timestamp: Long,
    val rating: Int = 0 // 0 = unrated, 1 = thumbs up, -1 = thumbs down
)

@Entity(tableName = "preferences")
data class PreferenceEntity(
    @PrimaryKey val key: String,
    val score: Double
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val appName: String?,
    val title: String?,
    val text: String?,
    val timestamp: Long
)

data class AppCount(
    val appName: String?,
    val cnt: Int
)
