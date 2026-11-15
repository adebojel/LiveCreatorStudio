package id.livecreator.studio

import org.junit.Assert.assertEquals
import org.junit.Test

class AppSettingsStoreTest {
    @Test
    fun speechRateIsClampedToSupportedRange() {
        assertEquals(0.5f, sanitizeSpeechRate(-4.0f), 0.0f)
        assertEquals(2.0f, sanitizeSpeechRate(4.0f), 0.0f)
        assertEquals(1.25f, sanitizeSpeechRate(1.25f), 0.0f)
    }

    @Test
    fun invalidSpeechRateFallsBackToNormal() {
        assertEquals(1.0f, sanitizeSpeechRate(Float.NaN), 0.0f)
        assertEquals(1.0f, sanitizeSpeechRate(Float.POSITIVE_INFINITY), 0.0f)
    }

    @Test
    fun speechPitchIsClampedToSupportedRange() {
        assertEquals(0.5f, sanitizeSpeechPitch(0.1f), 0.0f)
        assertEquals(2.0f, sanitizeSpeechPitch(3.0f), 0.0f)
        assertEquals(1.4f, sanitizeSpeechPitch(1.4f), 0.0f)
    }

    @Test
    fun invalidSpeechPitchFallsBackToNormal() {
        assertEquals(1.0f, sanitizeSpeechPitch(Float.NEGATIVE_INFINITY), 0.0f)
    }
}
