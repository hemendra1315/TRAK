package app.trak.core.data.db

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.trak.core.data.db.entity.AppMetaEntity
import app.trak.core.data.db.entity.AppMetaKeys
import app.trak.core.data.db.entity.ProfileEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Acceptance test A6 on a real SQLite database (emulator). */
@RunWith(AndroidJUnit4::class)
class TrakDatabaseTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val db = Room.inMemoryDatabaseBuilder(context, TrakDatabase::class.java).build()
    private val dao = db.profileDao()

    @get:Rule
    val migrationHelper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), TrakDatabase::class.java)

    @After fun close() = db.close()

    private fun row(name: String, modified: Long, state: String = "PENDING") = ProfileEntity(
        id = "11111111-1111-1111-1111-111111111111", displayName = name, primaryRole = "bowler", battingHand = null,
        bowlingStyles = "", weekStart = 1, weightUnit = "kg", timeZone = "Asia/Kolkata",
        adultConfirmedAt = 0, clientModifiedAt = modified, syncState = state, syncError = null, serverVersion = null,
    )

    @Test
    fun profileRoundTripAndObserve() = runTest {
        dao.upsert(row("Arjun", 1))
        assertEquals("Arjun", dao.observe("11111111-1111-1111-1111-111111111111").first()!!.displayName)
        dao.upsert(row("Arjun K", 2))
        assertEquals("Arjun K", dao.get("11111111-1111-1111-1111-111111111111")!!.displayName)
        assertEquals(1, dao.unsyncedCount())
    }

    @Test
    fun compareAndSetOnlyMarksUnchangedRows() = runTest {
        dao.upsert(row("v1", 100))
        assertEquals(0, dao.markSyncedIfUnchanged("11111111-1111-1111-1111-111111111111", 99, 1))
        assertEquals("PENDING", dao.get("11111111-1111-1111-1111-111111111111")!!.syncState)
        assertEquals(1, dao.markSyncedIfUnchanged("11111111-1111-1111-1111-111111111111", 100, 1))
        assertEquals("SYNCED", dao.get("11111111-1111-1111-1111-111111111111")!!.syncState)
        assertEquals(0, dao.unsyncedCount())
    }

    @Test
    fun adoptIfUnchangedRespectsNewerLocalEdits() = runTest {
        dao.upsert(row("local", 100))
        assertFalse(dao.adoptIfUnchanged(sentClientModifiedAt = 50, server = row("server", 200, "SYNCED")))
        assertEquals("local", dao.get("11111111-1111-1111-1111-111111111111")!!.displayName)
        assertTrue(dao.adoptIfUnchanged(sentClientModifiedAt = 100, server = row("server", 200, "SYNCED")))
        assertEquals("server", dao.get("11111111-1111-1111-1111-111111111111")!!.displayName)
    }

    @Test
    fun ownerMarkerAndWipe() = runTest {
        db.appMetaDao().put(AppMetaEntity(AppMetaKeys.OWNER_USER_ID, "owner-a"))
        dao.upsert(row("Arjun", 1))
        assertEquals("owner-a", db.appMetaDao().get(AppMetaKeys.OWNER_USER_ID))
        db.clearAllTables()
        assertNull(db.appMetaDao().get(AppMetaKeys.OWNER_USER_ID))
        assertNull(dao.get("11111111-1111-1111-1111-111111111111"))
    }

    @Test
    fun committedSchemaV1MatchesTheDatabase() {
        // Creates v1 from the committed JSON and validates it against Room's expectations.
        migrationHelper.createDatabase("schema-test.db", 1).close()
        migrationHelper.runMigrationsAndValidate("schema-test.db", 1, true)
    }
}
