package id.livecreator.studio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveEventProviderContractTest {
    @Test
    fun eventEnvelopeKeepsNormalizedFieldsAndNoRawPayload() {
        val event = LiveEventEnvelope(
            eventId = "evt-1",
            sourceId = "tiktok-live",
            type = LiveEventType.GIFT_COMBO,
            occurredAtEpochMillis = 1234L,
            displayName = "Ayu",
            giftName = "Rose",
            quantity = 5L
        )
        assertEquals(LiveEventType.GIFT_COMBO, event.type)
        assertEquals("Ayu", event.displayName)
        assertEquals(5L, event.quantity)
        assertEquals(null, event.comment)
    }

    @Test
    fun capabilityFlagsDoNotAssumeOfficialApi() {
        val capabilities = LiveProviderCapabilities(
            supportedEvents = setOf(LiveEventType.CHAT, LiveEventType.GIFT),
            supportsReconnect = true,
            supportsRoomInfo = true,
            isOfficialPlatformApi = false
        )
        assertTrue(capabilities.supportsReconnect)
        assertFalse(capabilities.isOfficialPlatformApi)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsBlankEventId() {
        LiveEventEnvelope("", "provider", LiveEventType.CHAT, 1L)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsNegativeCounts() {
        LiveEventEnvelope("evt", "provider", LiveEventType.LIKE, 1L, quantity = -1L)
    }
}
