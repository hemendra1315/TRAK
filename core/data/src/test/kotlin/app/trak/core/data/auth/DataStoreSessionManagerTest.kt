package app.trak.core.data.auth

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import io.github.jan.supabase.auth.exception.NoSessionFoundException
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

/** Session storage follows the supabase-kt 3.8.0 SessionManager contract. */
class DataStoreSessionManagerTest {

    private val dir: File = Files.createTempDirectory("session-test").toFile()
    private val store = PreferenceDataStoreFactory.create(
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
        produceFile = { File(dir, "session.preferences_pb") },
    )
    private val manager = DataStoreSessionManager(store)

    @Test
    fun `empty storage throws NoSessionFoundException`() {
        assertThrows(NoSessionFoundException::class.java) { runBlocking { manager.loadSession() } }
    }

    @Test
    fun `loadSessionOrNull returns null when empty and after delete`() = runBlocking {
        assertNull(manager.loadSessionOrNull())
        manager.deleteSession()
        assertNull(manager.loadSessionOrNull())
    }
}
