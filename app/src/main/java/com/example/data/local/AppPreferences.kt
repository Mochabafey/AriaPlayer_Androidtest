package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.AudioQuality
import com.example.model.NeteaseUser
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "aria_preferences")

data class UserSettings(
    val apiBaseUrl: String = "",
    val cookie: String = "",
    val user: NeteaseUser? = null,
    val audioQuality: AudioQuality = AudioQuality.EXHIGH,
    val lyricFontScale: Float = 1.0f,
    val showTranslatedLyrics: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM
) {
    val isServerConfigured: Boolean get() = apiBaseUrl.isNotBlank()
    val isLoggedIn: Boolean get() = cookie.isNotBlank() && user != null
}

class AppPreferences(private val context: Context) {
    private object Keys {
        val API_BASE_URL = stringPreferencesKey("api_base_url")
        val COOKIE = stringPreferencesKey("netease_cookie")
        val USER_ID = longPreferencesKey("user_id")
        val USER_NICKNAME = stringPreferencesKey("user_nickname")
        val USER_AVATAR = stringPreferencesKey("user_avatar")
        val USER_SIGNATURE = stringPreferencesKey("user_signature")
        val AUDIO_QUALITY = stringPreferencesKey("audio_quality")
        val LYRIC_FONT_SCALE = floatPreferencesKey("lyric_font_scale")
        val SHOW_TRANSLATED_LYRICS = booleanPreferencesKey("show_translated_lyrics")
        val THEME_MODE = stringPreferencesKey("theme_mode")
    }

    val settingsFlow: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        val uid = prefs[Keys.USER_ID] ?: 0L
        val nickname = prefs[Keys.USER_NICKNAME].orEmpty()
        val avatar = prefs[Keys.USER_AVATAR].orEmpty()
        val signature = prefs[Keys.USER_SIGNATURE].orEmpty()
        val user = if (uid > 0L && nickname.isNotBlank()) {
            NeteaseUser(
                userId = uid,
                nickname = nickname,
                avatarUrl = avatar,
                signature = signature
            )
        } else null

        val qualityLevel = prefs[Keys.AUDIO_QUALITY] ?: AudioQuality.EXHIGH.level
        val quality = AudioQuality.entries.firstOrNull { it.level == qualityLevel } ?: AudioQuality.EXHIGH

        val themeName = prefs[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name
        val theme = ThemeMode.entries.firstOrNull { it.name == themeName } ?: ThemeMode.SYSTEM

        UserSettings(
            apiBaseUrl = prefs[Keys.API_BASE_URL].orEmpty(),
            cookie = prefs[Keys.COOKIE].orEmpty(),
            user = user,
            audioQuality = quality,
            lyricFontScale = (prefs[Keys.LYRIC_FONT_SCALE] ?: 1.0f).coerceIn(0.8f, 1.5f),
            showTranslatedLyrics = prefs[Keys.SHOW_TRANSLATED_LYRICS] ?: true,
            themeMode = theme
        )
    }

    suspend fun setApiBaseUrl(url: String) {
        val normalized = normalizeUrl(url)
        context.dataStore.edit { prefs ->
            prefs[Keys.API_BASE_URL] = normalized
        }
    }

    suspend fun saveSession(cookie: String, user: NeteaseUser?) {
        context.dataStore.edit { prefs ->
            prefs[Keys.COOKIE] = cookie
            if (user != null) {
                prefs[Keys.USER_ID] = user.userId
                prefs[Keys.USER_NICKNAME] = user.nickname
                prefs[Keys.USER_AVATAR] = user.avatarUrl
                prefs[Keys.USER_SIGNATURE] = user.signature
            }
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { prefs ->
            prefs.remove(Keys.COOKIE)
            prefs.remove(Keys.USER_ID)
            prefs.remove(Keys.USER_NICKNAME)
            prefs.remove(Keys.USER_AVATAR)
            prefs.remove(Keys.USER_SIGNATURE)
        }
    }

    suspend fun setAudioQuality(quality: AudioQuality) {
        context.dataStore.edit { it[Keys.AUDIO_QUALITY] = quality.level }
    }

    suspend fun setLyricFontScale(scale: Float) {
        context.dataStore.edit { it[Keys.LYRIC_FONT_SCALE] = scale.coerceIn(0.8f, 1.5f) }
    }

    suspend fun setShowTranslatedLyrics(show: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_TRANSLATED_LYRICS] = show }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    companion object {
        fun normalizeUrl(raw: String): String {
            val trimmed = raw.trim()
            if (trimmed.isEmpty()) return ""
            val withScheme = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                trimmed
            } else {
                "http://$trimmed"
            }
            return if (withScheme.endsWith("/")) withScheme else "$withScheme/"
        }
    }
}
