package com.captionai.studio.storage

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import android.content.pm.ApplicationInfo
import java.text.SimpleDateFormat
import java.util.*

// ─────────────────────────────────────────────────────────────────────────────
// PrefsManager
// Handles: API key (obfuscated), language, dark mode, daily count, Pro status
// ─────────────────────────────────────────────────────────────────────────────

class PrefsManager private constructor(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        "caption_ai_prefs", Context.MODE_PRIVATE
    )

    companion object {
        @Volatile private var instance: PrefsManager? = null

        fun init(context: Context) {
            if (instance == null) {
                synchronized(this) {
                    if (instance == null) {
                        instance = PrefsManager(context.applicationContext)
                    }
                }
            }
        }

        const val FREE_DAILY_LIMIT = 3
        const val REWARDED_BONUS = 3

        fun getInstance(): PrefsManager =
            instance ?: throw IllegalStateException("Call PrefsManager.init(context) first")

        // Simple XOR + Base64 obfuscation — keeps key out of plain text storage
        private const val XOR_KEY = 0x42.toByte()

        private fun obfuscate(input: String): String {
            val bytes = input.toByteArray()
            val xored = bytes.map { (it.toInt() xor XOR_KEY.toInt()).toByte() }.toByteArray()
            return Base64.encodeToString(xored, Base64.NO_WRAP)
        }

        private fun deobfuscate(input: String): String {
            val bytes = Base64.decode(input, Base64.NO_WRAP)
            val xored = bytes.map { (it.toInt() xor XOR_KEY.toInt()).toByte() }.toByteArray()
            return String(xored)
        }
    }

    // ── API Key ───────────────────────────────────────────────────────────────

    fun setApiKey(key: String) {
        val trimmedKey = key.trim()
        if (trimmedKey.isBlank()) {
            prefs.edit().remove("api_key_obf").apply()
            return
        }
        prefs.edit().putString("api_key_obf", obfuscate(trimmedKey)).apply()
    }

    fun getApiKey(): String? {
        val stored = prefs.getString("api_key_obf", null) ?: return null
        return try {
            deobfuscate(stored)
        } catch (e: Exception) {
            null
        }
    }

    fun hasApiKey(): Boolean = getApiKey()?.isNotBlank() == true

    // ── Language ──────────────────────────────────────────────────────────────
    // "English" | "اردو" | "Roman Urdu"

    fun setLanguage(lang: String) = prefs.edit().putString("language", lang).apply()
    fun getLanguage(): String = prefs.getString("language", "English") ?: "English"

    // ── Dark Mode ─────────────────────────────────────────────────────────────

    private val _isDarkMode = androidx.compose.runtime.mutableStateOf(isDarkMode())
    val darkModeState: androidx.compose.runtime.State<Boolean> = _isDarkMode

    fun setDarkMode(enabled: Boolean) {
        prefs.edit().putBoolean("dark_mode", enabled).apply()
        _isDarkMode.value = enabled
    }

    fun isDarkMode(): Boolean = prefs.getBoolean("dark_mode", true)

    // ── Daily generation counter ──────────────────────────────────────────────

    fun getDailyCount(): Int {
        val today = todayKey()
        val storedDate = prefs.getString("daily_date", null)
        if (storedDate != today) {
            prefs.edit()
                .putString("daily_date", today)
                .putInt("daily_count", 0)
                .apply()
            return 0
        }
        return prefs.getInt("daily_count", 0)
    }

    fun incrementDailyCount() {
        val count = getDailyCount()
        prefs.edit().putInt("daily_count", count + 1).apply()
        _quotaTick.value++
    }

    private fun todayKey(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    // ── Bonus generations ─────────────────────────────────────────────────────

    private val _quotaTick = androidx.compose.runtime.mutableStateOf(0)
    /** Changes whenever the quota changes; read it in composables to recompose. */
    val quotaTick: androidx.compose.runtime.State<Int> = _quotaTick

    fun addBonusGenerations(n: Int) {
        val current = prefs.getInt("extra_gens", 0)
        prefs.edit().putInt("extra_gens", current + n).apply()
        _quotaTick.value++
    }

    fun getExtraGenerations(): Int {
        return prefs.getInt("extra_gens", 0)
    }

    /** Generations the user can still run today (daily allowance + bonus). Int.MAX_VALUE for Pro. */
    fun remainingGenerations(): Int {
        if (isPro()) return Int.MAX_VALUE
        return (FREE_DAILY_LIMIT - getDailyCount()).coerceAtLeast(0) + getExtraGenerations()
    }

    fun canGenerate(): Boolean = remainingGenerations() > 0

    /** Call this only AFTER a successful generation so failed requests don't cost quota. */
    @Synchronized
    fun consumeGeneration(): Boolean {
        if (isPro()) return true
        
        val daily = getDailyCount()
        val extra = getExtraGenerations()

        return if (daily < FREE_DAILY_LIMIT) {
            incrementDailyCount()
            true
        } else if (extra > 0) {
            prefs.edit().putInt("extra_gens", extra - 1).apply()
            _quotaTick.value++
            true
        } else {
            false
        }
    }

    // ── Pro status ────────────────────────────────────────────────────────────

    private val _isPro = androidx.compose.runtime.mutableStateOf(prefs.getBoolean("is_pro", false))
    /** Observable Pro flag so the UI updates as soon as a purchase completes. */
    val proState: androidx.compose.runtime.State<Boolean> = _isPro

    fun setPro(enabled: Boolean) {
        prefs.edit().putBoolean("is_pro", enabled).apply()
        _isPro.value = enabled
    }
    fun isPro(): Boolean = _isPro.value

    // ── Trending sync timestamp ───────────────────────────────────────────────

    fun getLastSyncTime(): Long = prefs.getLong("last_sync_time", 0L)
    fun setLastSyncTime(time: Long) = prefs.edit().putLong("last_sync_time", time).apply()

    fun canSync(): Boolean {
        val elapsed = System.currentTimeMillis() - getLastSyncTime()
        return elapsed > 12 * 60 * 60 * 1000L  // 12 hours
    }

    fun nextSyncInHours(): Int {
        val elapsed = System.currentTimeMillis() - getLastSyncTime()
        val remaining = 12 * 60 * 60 * 1000L - elapsed
        return if (remaining <= 0) 0 else (remaining / (60 * 60 * 1000L)).toInt()
    }
}
