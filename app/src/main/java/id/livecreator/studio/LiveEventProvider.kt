package id.livecreator.studio

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Read-only contract between a LIVE source adapter and the app's event pipeline.
 *
 * Implementations must not send chat messages or perform account actions. A provider may
 * expose only a subset of TikTok events; unsupported/unknown events must remain identifiable.
 */
interface LiveEventProvider {
    val providerId: String
    val status: StateFlow<LiveProviderStatus>
    val events: Flow<CreatorEvent>

    suspend fun connect(uniqueId: String)
    suspend fun disconnect()
}

enum class LiveConnectionState {
    DISCONNECTED, CONNECTING, CONNECTED, RECONNECTING, ERROR
}

data class LiveProviderStatus(
    val state: LiveConnectionState = LiveConnectionState.DISCONNECTED,
    val uniqueId: String? = null,
    val message: String = "Belum terhubung",
    val reconnectAttempt: Int = 0,
    val connectedAtEpochMillis: Long? = null,
    val lastEventAtEpochMillis: Long? = null
)
