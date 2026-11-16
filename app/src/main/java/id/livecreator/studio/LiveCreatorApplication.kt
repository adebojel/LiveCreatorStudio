package id.livecreator.studio

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Starts the small CC0 starter sound library download on the first app launch. */
class LiveCreatorApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            SoundPackInstaller.ensureStarterPacks(applicationContext)
        }
    }
}
