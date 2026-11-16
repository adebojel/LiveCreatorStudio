package id.livecreator.studio

import org.junit.Assert.assertEquals
import org.junit.Test

class CreatorEventEngineTest {
    private class Sink : CreatorEventActionSink {
        val speech = mutableListOf<String>()
        val sounds = mutableListOf<String>()
        var failSpeech = false
        override fun speak(text: String) {
            if (failSpeech) error("TTS unavailable")
            speech += text
        }
        override fun playSound(contentUri: String) { sounds += contentUri }
    }

    @Test
    fun dispatchesTemplateAndSoundForMatchingEvent() {
        val sink = Sink()
        val engine = CreatorEventEngine(
            listOf(CreatorEventRule("gift", CreatorEventType.GIFT, speechTemplate = "Terima kasih {name}, {quantity} hadiah", soundUri = "content://sound/1")),
            sink,
            nowMillis = { 100L }
        )
        assertEquals(CreatorEventResult.DISPATCHED, engine.dispatch(CreatorEvent("e1", CreatorEventType.GIFT, "Ayu", quantity = 3)))
        assertEquals(listOf("Terima kasih Ayu, 3 hadiah"), sink.speech)
        assertEquals(listOf("content://sound/1"), sink.sounds)
    }

    @Test
    fun ignoresDisabledAndBelowThresholdRules() {
        val sink = Sink()
        val engine = CreatorEventEngine(
            listOf(
                CreatorEventRule("disabled", CreatorEventType.FOLLOW, enabled = false, speechTemplate = "ignored"),
                CreatorEventRule("threshold", CreatorEventType.GIFT, minQuantity = 5, speechTemplate = "ignored")
            ),
            sink,
            nowMillis = { 100L }
        )
        assertEquals(CreatorEventResult.NO_MATCH, engine.dispatch(CreatorEvent("e1", CreatorEventType.FOLLOW, "A")))
        assertEquals(CreatorEventResult.NO_MATCH, engine.dispatch(CreatorEvent("e2", CreatorEventType.GIFT, "A", quantity = 2)))
        assertEquals(emptyList<String>(), sink.speech)
    }

    @Test
    fun preventsDuplicateEventsAndHonorsCooldown() {
        var now = 1_000L
        val sink = Sink()
        val engine = CreatorEventEngine(
            listOf(CreatorEventRule("follow", CreatorEventType.FOLLOW, cooldownMs = 5_000, speechTemplate = "Halo {name}")),
            sink,
            nowMillis = { now }
        )
        val event = CreatorEvent("same-id", CreatorEventType.FOLLOW, "Budi")
        assertEquals(CreatorEventResult.DISPATCHED, engine.dispatch(event))
        assertEquals(CreatorEventResult.DUPLICATE, engine.dispatch(event))
        assertEquals(CreatorEventResult.COOLDOWN, engine.dispatch(event.copy(id = "new-id")))
        now += 5_000
        assertEquals(CreatorEventResult.DISPATCHED, engine.dispatch(event.copy(id = "third-id")))
    }

    @Test
    fun rateLimitsAndBoundsUntrustedText() {
        var now = 0L
        val sink = Sink()
        val engine = CreatorEventEngine(
            listOf(CreatorEventRule("comment", CreatorEventType.COMMENT, speechTemplate = "{name}: {message}")),
            sink,
            nowMillis = { now },
            maxEventsPerMinute = 1
        )
        assertEquals(CreatorEventResult.DISPATCHED, engine.dispatch(CreatorEvent("1", CreatorEventType.COMMENT, "N".repeat(120), "M".repeat(500))))
        assertEquals(322, sink.speech.single().length)
        assertEquals(CreatorEventResult.RATE_LIMITED, engine.dispatch(CreatorEvent("2", CreatorEventType.COMMENT, "A", "B")))
        now = 60_000
        assertEquals(CreatorEventResult.DISPATCHED, engine.dispatch(CreatorEvent("3", CreatorEventType.COMMENT, "A", "B")))
    }

    @Test
    fun invalidSoundUriIsNeverDispatchedAndTtsFailureIsContained() {
        val sink = Sink().apply { failSpeech = true }
        val engine = CreatorEventEngine(
            listOf(CreatorEventRule("safe", CreatorEventType.CUSTOM, speechTemplate = "test", soundUri = "https://example.invalid/audio.mp3")),
            sink,
            nowMillis = { 1L }
        )
        assertEquals(CreatorEventResult.ACTION_FAILED, engine.dispatch(CreatorEvent("1", CreatorEventType.CUSTOM, "User")))
        assertEquals(emptyList<String>(), sink.sounds)
    }
}
