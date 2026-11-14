package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.gameProgressDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "tap_away_arrows_prefs"
)

data class PlayerProgress(
    val highestUnlockedLevel: Int = 1,
    val hintsRemaining: Int = 3,
    val levelStars: Map<Int, Int> = emptyMap(),
    val totalArrowsCleared: Int = 0,
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val showGridDots: Boolean = true,
    val darkTheme: Boolean = false
)

class GameProgressRepository(private val context: Context) {

    private object Keys {
        val HIGHEST_UNLOCKED = intPreferencesKey("highest_unlocked_level")
        val HINTS_REMAINING = intPreferencesKey("hints_remaining")
        val LEVEL_STARS = stringPreferencesKey("level_stars_map")
        val TOTAL_ARROWS_CLEARED = intPreferencesKey("total_arrows_cleared")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        val SHOW_GRID_DOTS = booleanPreferencesKey("show_grid_dots")
        val DARK_THEME = booleanPreferencesKey("dark_theme")
    }

    val progressFlow: Flow<PlayerProgress> = context.gameProgressDataStore.data.map { prefs ->
        PlayerProgress(
            highestUnlockedLevel = (prefs[Keys.HIGHEST_UNLOCKED] ?: 1).coerceAtLeast(1),
            hintsRemaining = prefs[Keys.HINTS_REMAINING] ?: 3,
            levelStars = parseStarsMap(prefs[Keys.LEVEL_STARS].orEmpty()),
            totalArrowsCleared = prefs[Keys.TOTAL_ARROWS_CLEARED] ?: 0,
            soundEnabled = prefs[Keys.SOUND_ENABLED] ?: true,
            hapticsEnabled = prefs[Keys.HAPTICS_ENABLED] ?: true,
            showGridDots = prefs[Keys.SHOW_GRID_DOTS] ?: true,
            darkTheme = prefs[Keys.DARK_THEME] ?: false
        )
    }

    suspend fun recordLevelComplete(levelNumber: Int, starsEarned: Int, arrowsCleared: Int) {
        context.gameProgressDataStore.edit { prefs ->
            val currentHighest = (prefs[Keys.HIGHEST_UNLOCKED] ?: 1).coerceAtLeast(1)
            if (levelNumber >= currentHighest) {
                prefs[Keys.HIGHEST_UNLOCKED] = (levelNumber + 1).coerceAtMost(250)
            }
            val currentMap = parseStarsMap(prefs[Keys.LEVEL_STARS].orEmpty()).toMutableMap()
            val prevStars = currentMap[levelNumber] ?: 0
            if (starsEarned > prevStars) {
                currentMap[levelNumber] = starsEarned.coerceIn(1, 3)
                prefs[Keys.LEVEL_STARS] = serializeStarsMap(currentMap)
            }
            prefs[Keys.TOTAL_ARROWS_CLEARED] = (prefs[Keys.TOTAL_ARROWS_CLEARED] ?: 0) + arrowsCleared

            // Reward +1 bonus hint on perfect 3-star first clear or every 3 levels
            val currentHints = prefs[Keys.HINTS_REMAINING] ?: 3
            if (prevStars == 0 && (starsEarned == 3 || levelNumber % 3 == 0)) {
                prefs[Keys.HINTS_REMAINING] = (currentHints + 1).coerceAtMost(99)
            }
        }
    }

    suspend fun consumeHint(): Boolean {
        var consumed = false
        context.gameProgressDataStore.edit { prefs ->
            val current = prefs[Keys.HINTS_REMAINING] ?: 3
            if (current > 0) {
                prefs[Keys.HINTS_REMAINING] = current - 1
                consumed = true
            }
        }
        return consumed
    }

    suspend fun refillFreeHint() {
        context.gameProgressDataStore.edit { prefs ->
            val current = prefs[Keys.HINTS_REMAINING] ?: 0
            if (current == 0) {
                prefs[Keys.HINTS_REMAINING] = 2
            }
        }
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.gameProgressDataStore.edit { it[Keys.SOUND_ENABLED] = enabled }
    }

    suspend fun setHapticsEnabled(enabled: Boolean) {
        context.gameProgressDataStore.edit { it[Keys.HAPTICS_ENABLED] = enabled }
    }

    suspend fun setShowGridDots(show: Boolean) {
        context.gameProgressDataStore.edit { it[Keys.SHOW_GRID_DOTS] = show }
    }

    suspend fun setDarkTheme(dark: Boolean) {
        context.gameProgressDataStore.edit { it[Keys.DARK_THEME] = dark }
    }

    private fun parseStarsMap(raw: String): Map<Int, Int> {
        if (raw.isBlank()) return emptyMap()
        val result = mutableMapOf<Int, Int>()
        raw.split(",").forEach { entry ->
            val parts = entry.split(":")
            if (parts.size == 2) {
                val lvl = parts[0].toIntOrNull()
                val stars = parts[1].toIntOrNull()
                if (lvl != null && stars != null) {
                    result[lvl] = stars
                }
            }
        }
        return result
    }

    private fun serializeStarsMap(map: Map<Int, Int>): String =
        map.entries.sortedBy { it.key }.joinToString(",") { "${it.key}:${it.value}" }
}
