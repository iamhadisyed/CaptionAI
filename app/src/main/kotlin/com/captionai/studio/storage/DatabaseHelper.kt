package com.captionai.studio.storage

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.captionai.studio.ai.CaptionResult

// ─────────────────────────────────────────────────────────────────────────────
// Database structure
// Tables: favorites, history, trending
// ─────────────────────────────────────────────────────────────────────────────

data class FavoriteCaption(
    val id: Long,
    val caption: String,
    val platform: String,
    val savedAt: Long
)

data class HistoryRow(
    val id: Long,
    val caption: String,
    val hashtags: String,
    val platform: String,
    val createdAt: Long
)

data class TrendingCaption(
    val id: Long,
    val caption: String,
    val syncedAt: Long
)

class DatabaseHelper private constructor(context: Context) :
    SQLiteOpenHelper(context, "caption_ai.db", null, 2) {

    companion object {
        private const val MAX_HISTORY_ROWS = 300
        @Volatile private var instance: DatabaseHelper? = null

        fun getInstance(context: Context): DatabaseHelper {
            return instance ?: synchronized(this) {
                instance ?: DatabaseHelper(context.applicationContext).also { instance = it }
            }
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE favorites (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                caption TEXT NOT NULL,
                platform TEXT NOT NULL,
                saved_at INTEGER NOT NULL
            )
        """)

        db.execSQL("""
            CREATE TABLE history (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                caption TEXT NOT NULL,
                hashtags TEXT NOT NULL,
                platform TEXT NOT NULL,
                created_at INTEGER NOT NULL
            )
        """)

        db.execSQL("""
            CREATE TABLE trending (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                caption TEXT NOT NULL,
                synced_at INTEGER NOT NULL
            )
        """)
    }

    // Never drop user data on upgrade. Add `if (oldVersion < N) { ALTER TABLE ... }` steps here
    // whenever DB_VERSION is bumped.
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_history_created ON history(created_at)")
        }
    }

    // ── Favorites ─────────────────────────────────────────────────────────────

    fun isFavorite(caption: String): Boolean =
        readableDatabase.query("favorites", arrayOf("id"), "caption = ?", arrayOf(caption), null, null, null, "1")
            .use { it.moveToFirst() }

    /** Saves a favorite; saving the same caption twice is a no-op that returns the existing row id. */
    fun saveFavorite(caption: String, platform: String): Long {
        val db = writableDatabase
        db.query("favorites", arrayOf("id"), "caption = ?", arrayOf(caption), null, null, null, "1").use {
            if (it.moveToFirst()) return it.getLong(0)
        }
        val values = ContentValues().apply {
            put("caption", caption)
            put("platform", platform)
            put("saved_at", System.currentTimeMillis())
        }
        return db.insert("favorites", null, values)
    }

    fun deleteFavorite(id: Long) {
        writableDatabase.delete("favorites", "id = ?", arrayOf(id.toString()))
    }

    fun getFavorites(): List<FavoriteCaption> {
        val list = mutableListOf<FavoriteCaption>()
        val cursor = readableDatabase.query(
            "favorites", null, null, null, null, null, "saved_at DESC"
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    FavoriteCaption(
                        id = it.getLong(it.getColumnIndexOrThrow("id")),
                        caption = it.getString(it.getColumnIndexOrThrow("caption")),
                        platform = it.getString(it.getColumnIndexOrThrow("platform")),
                        savedAt = it.getLong(it.getColumnIndexOrThrow("saved_at"))
                    )
                )
            }
        }
        return list
    }

    // ── History ───────────────────────────────────────────────────────────────

    fun saveCaptionToHistory(result: CaptionResult) {
        val db = writableDatabase
        val now = System.currentTimeMillis()
        db.beginTransaction()
        try {
            result.captions.forEach { caption ->
                val values = ContentValues().apply {
                    put("caption", caption)
                    put("hashtags", result.hashtags.joinToString(","))
                    put("platform", result.platform)
                    put("created_at", now)
                }
                db.insert("history", null, values)
            }
            // Keep the table bounded.
            db.execSQL(
                "DELETE FROM history WHERE id NOT IN (SELECT id FROM history ORDER BY created_at DESC, id DESC LIMIT $MAX_HISTORY_ROWS)"
            )
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun getHistory(): List<HistoryRow> {
        val list = mutableListOf<HistoryRow>()
        readableDatabase.query("history", null, null, null, null, null, "created_at DESC, id ASC").use {
            while (it.moveToNext()) {
                list.add(
                    HistoryRow(
                        id = it.getLong(it.getColumnIndexOrThrow("id")),
                        caption = it.getString(it.getColumnIndexOrThrow("caption")),
                        hashtags = it.getString(it.getColumnIndexOrThrow("hashtags")),
                        platform = it.getString(it.getColumnIndexOrThrow("platform")),
                        createdAt = it.getLong(it.getColumnIndexOrThrow("created_at"))
                    )
                )
            }
        }
        return list
    }

    fun deleteHistoryItem(id: Long) {
        writableDatabase.delete("history", "id = ?", arrayOf(id.toString()))
    }

    fun clearHistory() {
        writableDatabase.delete("history", null, null)
    }

    // ── Trending ──────────────────────────────────────────────────────────────

    fun saveTrendingCaptions(captions: List<String>) {
        val db = writableDatabase
        val now = System.currentTimeMillis()
        db.beginTransaction()
        try {
            db.delete("trending", null, null)  // replace old batch
            captions.forEach { caption ->
                val values = ContentValues().apply {
                    put("caption", caption)
                    put("synced_at", now)
                }
                db.insert("trending", null, values)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun getTrendingCaptions(): List<TrendingCaption> {
        val list = mutableListOf<TrendingCaption>()
        val cursor = readableDatabase.query(
            "trending", null, null, null, null, null, "id ASC"
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    TrendingCaption(
                        id = it.getLong(it.getColumnIndexOrThrow("id")),
                        caption = it.getString(it.getColumnIndexOrThrow("caption")),
                        syncedAt = it.getLong(it.getColumnIndexOrThrow("synced_at"))
                    )
                )
            }
        }
        return list
    }
}
