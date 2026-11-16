package id.livecreator.studio

import java.util.ArrayDeque

/** Normalized, provider-agnostic creator event. External providers must be authorized adapters. */
enum class CreatorEventType {
    FOLLOW, SUBSCRIPTION, GIFT, LIKE, SHARE, COMMENT,
    JOIN, QUESTION, EMOTE, ROOM_UPDATE, BATTLE, POLL, SYSTEM, UNKNOWN, CUSTOM
}

data class CreatorEvent(
    val id: String,
    val type: CreatorEventType,
    val displayName: String,
    val message: String = "",
    val quantity: Int = 1,
    val source: String = "unknown",
    val occurredAtEpochMillis: Long = 0L,
    val giftName: String? = null,
    val giftValueDiamonds: Long? = null,
    val viewerCount: Long? = null,
    val rawEventType: String? = null
)

data class CreatorEventRule(
    val id: String,
    val type: CreatorEventType,
    val enabled: Boolean = true,
    val minQuantity: Int = 1,
    val cooldownMs: Long = 0,
    val speechTemplate: String? = null,
    val soundUri: String? = null
) {
    init {
        require(id.isNotBlank()) { "Rule ID must not be blank." }
        require(minQuantity >= 1) { "Minimum quantity must be positive." }
        require(cooldownMs >= 0) { "Cooldown cannot be negative." }
    }
}

/** The app supplies concrete TTS and Soundboard adapters; the engine itself owns no Android resources. */
interface CreatorEventActionSink {
    fun speak(text: String)
    fun playSound(contentUri: String)
}

enum class CreatorEventResult {
    DISPATCHED, DUPLICATE, NO_MATCH, COOLDOWN, RATE_LIMITED, NO_ACTION, ACTION_FAILED
}

/**
 * Bounded, deterministic rule engine. It never connects to TikTok or another LIVE provider itself.
 * The injected clock must be monotonic (elapsed realtime), not wall-clock time.
 */
class CreatorEventEngine(
    rules: List<CreatorEventRule>,
    private val sink: CreatorEventActionSink,
    private val nowMillis: () -> Long,
    private val maxEventsPerMinute: Int = 20
) {
    private val rulesByType = rules.groupBy { it.type }
    private val lastDispatchByRule = mutableMapOf<String, Long>()
    private val recentDispatchTimes = ArrayDeque<Long>()
    private val recentEventIds = LinkedHashSet<String>()

    init {
        require(rules.map { it.id }.distinct().size == rules.size) { "Rule IDs must be unique." }
        require(maxEventsPerMinute in 1..120) { "Rate limit must be between 1 and 120 events/minute." }
    }

    @Synchronized
    fun dispatch(event: CreatorEvent): CreatorEventResult {
        if (event.id.isBlank()) return CreatorEventResult.NO_MATCH
        if (!recentEventIds.add(event.id)) return CreatorEventResult.DUPLICATE
        while (recentEventIds.size > MAX_REMEMBERED_EVENT_IDS) {
            val iterator = recentEventIds.iterator()
            if (iterator.hasNext()) {
                iterator.next()
                iterator.remove()
            }
        }

        val rule = rulesByType[event.type]
            ?.firstOrNull { it.enabled && event.quantity.coerceAtLeast(1) >= it.minQuantity }
            ?: return CreatorEventResult.NO_MATCH

        val now = nowMillis().coerceAtLeast(0L)
        while (recentDispatchTimes.peekFirst()?.let { now - it >= ONE_MINUTE_MS } == true) {
            recentDispatchTimes.removeFirst()
        }
        if (recentDispatchTimes.size >= maxEventsPerMinute) return CreatorEventResult.RATE_LIMITED

        val last = lastDispatchByRule[rule.id]
        if (last != null && now - last < rule.cooldownMs) return CreatorEventResult.COOLDOWN

        val safeName = sanitize(event.displayName, MAX_NAME_LENGTH).ifBlank { "Penonton" }
        val safeMessage = sanitize(event.message, MAX_MESSAGE_LENGTH)
        val quantity = event.quantity.coerceIn(1, MAX_QUANTITY)
        var succeeded = false
        var failed = false

        rule.speechTemplate?.let { template ->
            val text = template
                .replace("{name}", safeName)
                .replace("{message}", safeMessage)
                .replace("{quantity}", quantity.toString())
                .let { sanitize(it, MAX_SPEECH_LENGTH) }
            if (text.isNotBlank()) {
                runCatching { sink.speak(text) }
                    .onSuccess { succeeded = true }
                    .onFailure { failed = true }
            }
        }

        rule.soundUri?.takeIf { it.startsWith("content://") }?.let { uri ->
            runCatching { sink.playSound(uri) }
                .onSuccess { succeeded = true }
                .onFailure { failed = true }
        }

        if (!succeeded) return if (failed) CreatorEventResult.ACTION_FAILED else CreatorEventResult.NO_ACTION
        lastDispatchByRule[rule.id] = now
        recentDispatchTimes.addLast(now)
        return CreatorEventResult.DISPATCHED
    }

    private fun sanitize(value: String, maxLength: Int): String =
        value.filterNot { it.isISOControl() }.trim().take(maxLength)

    private companion object {
        const val ONE_MINUTE_MS = 60_000L
        const val MAX_REMEMBERED_EVENT_IDS = 256
        const val MAX_NAME_LENGTH = 80
        const val MAX_MESSAGE_LENGTH = 240
        const val MAX_SPEECH_LENGTH = 500
        const val MAX_QUANTITY = 9_999
    }
}
