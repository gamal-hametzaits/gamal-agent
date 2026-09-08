package dev.hametzaits.gamal.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages ORDER BY timestamp ASC, id ASC")
    fun observeMessages(): Flow<List<MessageEntity>>

    @Insert
    suspend fun insert(message: MessageEntity): Long

    @Query("UPDATE messages SET rating = :rating WHERE id = :id")
    suspend fun setRating(id: Long, rating: Int)

    @Query("SELECT * FROM messages WHERE role = 'user' AND timestamp <= :ts ORDER BY timestamp DESC, id DESC LIMIT 1")
    suspend fun lastUserMessageBefore(ts: Long): MessageEntity?

    @Query("SELECT COUNT(*) FROM messages WHERE rating != 0")
    suspend fun ratedCount(): Int
}

@Dao
interface PreferenceDao {
    @Query("SELECT * FROM preferences")
    suspend fun all(): List<PreferenceEntity>

    @Query("SELECT score FROM preferences WHERE `key` = :key")
    suspend fun score(key: String): Double?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(preference: PreferenceEntity)
}

@Dao
interface NotificationDao {
    @Insert
    suspend fun insert(entry: NotificationEntity)

    @Query("SELECT COUNT(*) FROM notifications WHERE timestamp > :since")
    suspend fun countSince(since: Long): Int

    @Query("SELECT COUNT(*) FROM notifications")
    suspend fun totalCount(): Int

    @Query("SELECT appName, COUNT(*) AS cnt FROM notifications WHERE timestamp > :since GROUP BY appName ORDER BY cnt DESC LIMIT 1")
    suspend fun topAppSince(since: Long): AppCount?

    @Query("DELETE FROM notifications WHERE timestamp < :before")
    suspend fun pruneBefore(before: Long)
}
