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

data class TrendingCaption(
    val id: Long,
    val caption: String,
    val syncedAt: Long
)

class DatabaseHelper private constructor(context: Context) :
    SQLiteOpenHelper(context, "caption_ai.db", null, 1) {

    companion object {
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

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS favorites")
        db.execSQL("DROP TABLE IF EXISTS history")
        db.execSQL("DROP TABLE IF EXISTS trending")
        onCreate(db)
    }

    // ── Favorites ─────────────────────────────────────────────────────────────

    fun saveFavorite(caption: String, platform: String): Long {
        val db = writableDatabase
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
        result.captions.forEach { caption ->
            val values = ContentValues().apply {
                put("caption", caption)
                put("hashtags", result.hashtags.joinToString(","))
                put("platform", result.platform)
                put("created_at", System.currentTimeMillis())
            }
            db.insert("history", null, values)
        }
    }

    fun clearHistory() {
        writableDatabase.delete("history", null, null)
    }

    // ── Trending ──────────────────────────────────────────────────────────────

    fun saveTrendingCaptions(captions: List<String>) {
        val db = writableDatabase
        db.delete("trending", null, null)  // replace old batch
        val now = System.currentTimeMillis()
        captions.forEach { caption ->
            val values = ContentValues().apply {
                put("caption", caption)
                put("synced_at", now)
            }
            db.insert("trending", null, values)
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
