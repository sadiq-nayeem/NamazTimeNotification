package com.example.namaztimenotification.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject
import org.threeten.bp.ZoneId

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class UserPreferences(private val context: Context) {
    companion object {
        private val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        private val SILENT_NOTIFICATIONS = booleanPreferencesKey("silent_notifications")
        private val TIME_ZONE = stringPreferencesKey("time_zone")
        
        // Individual prayer notification keys
        private val FAJR_NOTIFICATION = booleanPreferencesKey("fajr_notification")
        private val DHUHR_NOTIFICATION = booleanPreferencesKey("dhuhr_notification")
        private val ASR_NOTIFICATION = booleanPreferencesKey("asr_notification")
        private val MAGHRIB_NOTIFICATION = booleanPreferencesKey("maghrib_notification")
        private val ISHA_NOTIFICATION = booleanPreferencesKey("isha_notification")
    }

    val notificationsEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[NOTIFICATIONS_ENABLED] ?: true
        }

    val silentNotifications: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[SILENT_NOTIFICATIONS] ?: false
        }

    val timeZone: Flow<ZoneId> = context.dataStore.data
        .map { preferences ->
            ZoneId.of(preferences[TIME_ZONE] ?: "Asia/Dhaka")
        }

    val prayerNotifications: Flow<Map<String, Boolean>> = context.dataStore.data
        .map { preferences ->
            mapOf(
                "Fajr" to (preferences[FAJR_NOTIFICATION] ?: true),
                "Dhuhr" to (preferences[DHUHR_NOTIFICATION] ?: true),
                "Asr" to (preferences[ASR_NOTIFICATION] ?: true),
                "Maghrib" to (preferences[MAGHRIB_NOTIFICATION] ?: true),
                "Isha" to (preferences[ISHA_NOTIFICATION] ?: true)
            )
        }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun setSilentNotifications(silent: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SILENT_NOTIFICATIONS] = silent
        }
    }

    suspend fun setTimeZone(zoneId: ZoneId) {
        context.dataStore.edit { preferences ->
            preferences[TIME_ZONE] = zoneId.id
        }
    }

    suspend fun setPrayerNotification(prayerName: String, enabled: Boolean) {
        context.dataStore.edit { preferences ->
            when (prayerName) {
                "Fajr" -> preferences[FAJR_NOTIFICATION] = enabled
                "Dhuhr" -> preferences[DHUHR_NOTIFICATION] = enabled
                "Asr" -> preferences[ASR_NOTIFICATION] = enabled
                "Maghrib" -> preferences[MAGHRIB_NOTIFICATION] = enabled
                "Isha" -> preferences[ISHA_NOTIFICATION] = enabled
            }
        }
    }

    suspend fun exportToJson(): JSONObject {
        val json = JSONObject()
        context.dataStore.data.map { preferences ->
            json.put("notifications_enabled", preferences[NOTIFICATIONS_ENABLED] ?: true)
            json.put("silent_notifications", preferences[SILENT_NOTIFICATIONS] ?: false)
            json.put("time_zone", preferences[TIME_ZONE] ?: "Asia/Dhaka")
            json.put("fajr_notification", preferences[FAJR_NOTIFICATION] ?: true)
            json.put("dhuhr_notification", preferences[DHUHR_NOTIFICATION] ?: true)
            json.put("asr_notification", preferences[ASR_NOTIFICATION] ?: true)
            json.put("maghrib_notification", preferences[MAGHRIB_NOTIFICATION] ?: true)
            json.put("isha_notification", preferences[ISHA_NOTIFICATION] ?: true)
        }
        return json
    }

    suspend fun importFromJson(json: JSONObject) {
        context.dataStore.edit { preferences ->
            preferences[NOTIFICATIONS_ENABLED] = json.optBoolean("notifications_enabled", true)
            preferences[SILENT_NOTIFICATIONS] = json.optBoolean("silent_notifications", false)
            preferences[TIME_ZONE] = json.optString("time_zone", "Asia/Dhaka")
            preferences[FAJR_NOTIFICATION] = json.optBoolean("fajr_notification", true)
            preferences[DHUHR_NOTIFICATION] = json.optBoolean("dhuhr_notification", true)
            preferences[ASR_NOTIFICATION] = json.optBoolean("asr_notification", true)
            preferences[MAGHRIB_NOTIFICATION] = json.optBoolean("maghrib_notification", true)
            preferences[ISHA_NOTIFICATION] = json.optBoolean("isha_notification", true)
        }
    }
} 