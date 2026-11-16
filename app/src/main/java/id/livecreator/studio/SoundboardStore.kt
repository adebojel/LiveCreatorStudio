package id.livecreator.studio

import android.content.Context
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Persistent soundboard metadata.
 *
 * Existing SharedPreferences entries are migrated automatically. The decoder
 * accepts the legacy pipe-delimited format so upgrades preserve imported files.
 * URI permissions remain owned by Android's Storage Access Framework.
 */
internal data class SoundItem(val uri: String, val title: String)

private val Context.soundboardDataStore by preferencesDataStore(
    name = "livecreator_soundboard_store",
    produceMigrations = { context ->
        listOf(SharedPreferencesMigration(context, "livecreator_soundboard"))
    }
)

internal object SoundboardStore {
    private val ITEMS = stringSetPreferencesKey("items")

    fun observe(context: Context): Flow<List<SoundItem>> =
        context.soundboardDataStore.data.map { preferences ->
            decode(preferences[ITEMS].orEmpty())
        }

    suspend fun save(context: Context, sounds: List<SoundItem>) {
        context.soundboardDataStore.edit { preferences ->
            preferences[ITEMS] = encode(sounds)
        }
    }

    internal fun encode(sounds: List<SoundItem>): Set<String> =
        sounds.map { item ->
            "${item.uri}\t${item.title.replace("\t", " ").replace("\n", " ")}"
        }.toSet()

    internal fun decode(entries: Set<String>): List<SoundItem> =
        entries.mapNotNull { entry ->
            val tab = entry.indexOf('\t')
            val separator = if (tab > 0) tab else entry.indexOf('|')
            if (separator <= 0 || separator == entry.lastIndex) {
                null
            } else {
                val uri = entry.substring(0, separator)
                val title = entry.substring(separator + 1)
                if (uri.isBlank()) null else SoundItem(uri, title)
            }
        }.distinctBy { it.uri }.sortedBy { it.title.lowercase() }
}
