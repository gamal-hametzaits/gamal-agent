package dev.hametzaits.gamal.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class Message(
    val id: Long,
    val role: String, // "user" or "agent"
    val text: String,
    val timestamp: Long,
    val rating: Int // 0 = unrated, 1 = thumbs up, -1 = thumbs down
)

data class NotificationEntry(
    val id: Long,
    val packageName: String,
    val appName: String?,
    val title: String?,
    val text: String?,
    val timestamp: Long
)

/**
 * On-device memory: one local SQLite database. No network, no sync.
 * messages = chat history + ratings; preferences = learned profile;
 * notifications = rolling 7-day notification archive (explicit permission only).
 */
class GamalStore private constructor(context: Context) :
    SQLiteOpenHelper(context, "gamal.db", null, 1) {

    @Volatile
    var onMessagesChanged: (() -> Unit)? = null

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE messages (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "role TEXT NOT NULL, " +
                "text TEXT NOT NULL, " +
                "timestamp INTEGER NOT NULL, " +
                "rating INTEGER NOT NULL DEFAULT 0)"
        )
        db.execSQL("CREATE TABLE preferences (`key` TEXT PRIMARY KEY, score REAL NOT NULL)")
        db.execSQL(
            "CREATE TABLE notifications (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "packageName TEXT NOT NULL, " +
                "appName TEXT, " +
                "title TEXT, " +
                "text TEXT, " +
                "timestamp INTEGER NOT NULL)"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}

    companion object {
        @Volatile private var INSTANCE: GamalStore? = null

        fun get(context: Context): GamalStore =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: GamalStore(context.applicationContext).also { INSTANCE = it }
            }
    }

    // ---- messages ----

    fun insertMessage(role: String, text: String, timestamp: Long): Long {
        val values = ContentValues().apply {
            put("role", role)
            put("text", text)
            put("timestamp", timestamp)
            put("rating", 0)
        }
        val id = writableDatabase.insert("messages", null, values)
        onMessagesChanged?.invoke()
        return id
    }

    fun allMessages(): List<Message> {
        val out = mutableListOf<Message>()
        readableDatabase.rawQuery(
            "SELECT id, role, text, timestamp, rating FROM messages ORDER BY timestamp ASC, id ASC",
            null
        ).use { c ->
            while (c.moveToNext()) {
                out.add(Message(c.getLong(0), c.getString(1), c.getString(2), c.getLong(3), c.getInt(4)))
            }
        }
        return out
    }

    fun setRating(id: Long, rating: Int) {
        val values = ContentValues().apply { put("rating", rating) }
        writableDatabase.update("messages", values, "id = ?", arrayOf(id.toString()))
        onMessagesChanged?.invoke()
    }

    fun lastUserMessageBefore(ts: Long): Message? {
        readableDatabase.rawQuery(
            "SELECT id, role, text, timestamp, rating FROM messages " +
                "WHERE role = 'user' AND timestamp <= ? ORDER BY timestamp DESC, id DESC LIMIT 1",
            arrayOf(ts.toString())
        ).use { c ->
            return if (c.moveToFirst()) {
                Message(c.getLong(0), c.getString(1), c.getString(2), c.getLong(3), c.getInt(4))
            } else null
        }
    }

    fun ratedCount(): Int {
        readableDatabase.rawQuery("SELECT COUNT(*) FROM messages WHERE rating != 0", null).use { c ->
            return if (c.moveToFirst()) c.getInt(0) else 0
        }
    }

    // ---- preferences ----

    fun prefScore(key: String): Double? {
        readableDatabase.rawQuery(
            "SELECT score FROM preferences WHERE `key` = ?",
            arrayOf(key)
        ).use { c ->
            return if (c.moveToFirst()) c.getDouble(0) else null
        }
    }

    fun allPrefs(): Map<String, Double> {
        val out = mutableMapOf<String, Double>()
        readableDatabase.rawQuery("SELECT `key`, score FROM preferences", null).use { c ->
            while (c.moveToNext()) out[c.getString(0)] = c.getDouble(1)
        }
        return out
    }

    fun upsertPref(key: String, score: Double) {
        val values = ContentValues().apply {
            put("key", key)
            put("score", score)
        }
        writableDatabase.insertWithOnConflict("preferences", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    // ---- notifications ----

    fun insertNotification(
        packageName: String,
        appName: String?,
        title: String?,
        text: String?,
        timestamp: Long
    ) {
        val values = ContentValues().apply {
            put("packageName", packageName)
            put("appName", appName)
            put("title", title)
            put("text", text)
            put("timestamp", timestamp)
        }
        writableDatabase.insert("notifications", null, values)
    }

    fun countNotificationsSince(since: Long): Int {
        readableDatabase.rawQuery(
            "SELECT COUNT(*) FROM notifications WHERE timestamp > ?",
            arrayOf(since.toString())
        ).use { c ->
            return if (c.moveToFirst()) c.getInt(0) else 0
        }
    }

    fun totalNotifications(): Int {
        readableDatabase.rawQuery("SELECT COUNT(*) FROM notifications", null).use { c ->
            return if (c.moveToFirst()) c.getInt(0) else 0
        }
    }

    fun topAppSince(since: Long): Pair<String, Int>? {
        readableDatabase.rawQuery(
            "SELECT appName, COUNT(*) AS cnt FROM notifications " +
                "WHERE timestamp > ? AND appName IS NOT NULL " +
                "GROUP BY appName ORDER BY cnt DESC LIMIT 1",
            arrayOf(since.toString())
        ).use { c ->
            return if (c.moveToFirst()) c.getString(0) to c.getInt(1) else null
        }
    }

    fun pruneNotificationsBefore(before: Long) {
        writableDatabase.delete("notifications", "timestamp < ?", arrayOf(before.toString()))
    }
}
