package id.livecreator.studio

import android.content.Context
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

/**
 * Central preferences store. Existing SharedPreferences files are migrated once
 * so existing user settings survive the storage upgrade.
 */
val Context.liveCreatorSettings by preferencesDataStore(
    name = "livecreator_settings",
    produceMigrations = { context ->
        listOf(
            SharedPreferencesMigration(context, "livecreator_prefs"),
            SharedPreferencesMigration(context, "livecreator_tts")
        )
    }
)

internal object AppSettingKeys {
    val AUTO_START_ON_LAUNCH = booleanPreferencesKey("auto_start_on_launch")
    val RESUME_REMINDER_ENABLED = booleanPreferencesKey("resume_reminder_enabled")
    val SPEECH_RATE = floatPreferencesKey("rate")
    val SPEECH_PITCH = floatPreferencesKey("pitch")
}

internal fun sanitizeSpeechRate(value: Float): Float =
    if (value.isFinite()) value.coerceIn(0.5f, 2.0f) else 1.0f

internal fun sanitizeSpeechPitch(value: Float): Float =
    if (value.isFinite()) value.coerceIn(0.5f, 2.0f) else 1.0f

internal fun sanitizeFloatingBubbleSize(value: Float): Float =
    if (value.isFinite()) value.coerceIn(44f, 84f) else 58f

internal fun sanitizeFloatingBubbleOpacity(value: Float): Float =
    if (value.isFinite()) value.coerceIn(0.25f, 1.0f) else 0.82f
