package id.livecreator.studio

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Local-only rule configuration. No provider connection or network access. */
internal object CreatorRuleStore {
    private const val PREFS = "creator_event_rules"
    private const val KEY_RULES = "rules"

    fun load(context: Context): List<CreatorEventRule> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_RULES, null)
            ?: return defaultRules()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    val type = runCatching { CreatorEventType.valueOf(item.getString("type")) }.getOrNull() ?: continue
                    add(CreatorEventRule(
                        id = item.optString("id"),
                        type = type,
                        enabled = item.optBoolean("enabled", false),
                        minQuantity = item.optInt("minQuantity", 1).coerceIn(1, 9999),
                        cooldownMs = item.optLong("cooldownMs", 0L).coerceIn(0L, 3_600_000L),
                        speechTemplate = item.optString("speechTemplate").takeIf { it.isNotBlank() }?.take(500),
                        soundUri = item.optString("soundUri").takeIf { it.startsWith("content://") }
                    ))
                }
            }
        }.getOrElse { defaultRules() }
    }

    fun save(context: Context, rules: List<CreatorEventRule>) {
        val array = JSONArray()
        rules.distinctBy { it.type }.forEach { rule ->
            array.put(JSONObject()
                .put("id", rule.id)
                .put("type", rule.type.name)
                .put("enabled", rule.enabled)
                .put("minQuantity", rule.minQuantity.coerceIn(1, 9999))
                .put("cooldownMs", rule.cooldownMs.coerceIn(0L, 3_600_000L))
                .put("speechTemplate", rule.speechTemplate.orEmpty().take(500))
                .put("soundUri", rule.soundUri.orEmpty().takeIf { it.startsWith("content://") }.orEmpty()))
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_RULES, array.toString()).apply()
    }

    private fun defaultRules() = listOf(
        CreatorEventType.FOLLOW to "Terima kasih sudah mengikuti, {name}!",
        CreatorEventType.GIFT to "Terima kasih {name} atas {quantity} hadiah!",
        CreatorEventType.LIKE to "Terima kasih atas dukungannya, {name}!",
        CreatorEventType.SHARE to "Terima kasih sudah membagikan live ini, {name}!",
        CreatorEventType.COMMENT to "{name} berkomentar: {message}",
        CreatorEventType.JOIN to "Selamat datang {name}!"
    ).map { (type, template) ->
        CreatorEventRule(id = "local-${type.name.lowercase()}", type = type, enabled = false, speechTemplate = template)
    }
}
