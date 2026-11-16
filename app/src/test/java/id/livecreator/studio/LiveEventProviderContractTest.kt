package id.livecreator.studio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveEventProviderContractTest {
    @Test
    fun providerStartsDisconnectedWithoutExposingCredentials() {
        val status = LiveProviderStatus()
        assertEquals(LiveConnectionState.DISCONNECTED, status.state)
        assertNull(status.uniqueId)
        assertEquals("Belum terhubung", status.message)
        assertEquals(0, status.reconnectAttempt)
        assertNull(status.connectedAtEpochMillis)
        assertNull(status.lastEventAtEpochMillis)
    }

    @Test
    fun connectionStatesCoverLifecycleWithoutAssumingOfficialApi() {
        assertTrue(LiveConnectionState.values().containsAll(
            listOf(
                LiveConnectionState.DISCONNECTED,
                LiveConnectionState.CONNECTING,
                LiveConnectionState.CONNECTED,
                LiveConnectionState.RECONNECTING,
                LiveConnectionState.ERROR
            )
        ))
    }

    @Test
    fun statusCanRepresentReconnectProgress() {
        val status = LiveProviderStatus(
            state = LiveConnectionState.RECONNECTING,
            uniqueId = "creator_id",
            message = "Mencoba menyambungkan kembali",
            reconnectAttempt = 2,
            connectedAtEpochMillis = 1000L,
            lastEventAtEpochMillis = 1500L
        )
        assertEquals(LiveConnectionState.RECONNECTING, status.state)
        assertEquals("creator_id", status.uniqueId)
        assertEquals(2, status.reconnectAttempt)
        assertEquals(1000L, status.connectedAtEpochMillis)
        assertEquals(1500L, status.lastEventAtEpochMillis)
    }
}
