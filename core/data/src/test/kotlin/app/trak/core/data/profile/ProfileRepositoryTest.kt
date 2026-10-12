package app.trak.core.data.profile

import app.trak.core.data.fakes.FakeClock
import app.trak.core.data.fakes.FakeProfileDao
import app.trak.core.data.fakes.FakeProfileRemote
import app.trak.core.model.PrimaryRole
import app.trak.core.model.ProfileInput
import app.trak.core.model.UserId
import java.time.Instant
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Acceptance tests A7–A10, A17, A18 for Phase 1 profile persistence and sync. */
class ProfileRepositoryTest {

    private val user = UserId("11111111-1111-1111-1111-111111111111")
    private val dao = FakeProfileDao()
    private val remote = FakeProfileRemote()
    private val repo = ProfileRepository(dao, remote, FakeClock(), TestScope())

    private fun input(name: String) = ProfileInput(
        displayName = name,
        primaryRole = PrimaryRole.BOWLER,
        timeZoneId = "Asia/Kolkata",
        adultConfirmedAt = Instant.parse("2026-10-12T09:00:00Z"),
    )

    private suspend fun row() = dao.get(user.value)!!

    @Test
    fun `A7 offline save is kept on the phone as PENDING`() = runTest {
        remote.nextUpsertResult = RemoteResult.Network
        assertEquals(SaveResult.Saved, repo.save(user, input("Arjun")))
        assertEquals(SyncOutcome.StillPending, repo.sync(user))
        assertEquals("PENDING", row().syncState)
        assertEquals("Arjun", row().displayName)
    }

    @Test
    fun `A8 retry after reconnecting syncs with exactly one more push`() = runTest {
        remote.nextUpsertResult = RemoteResult.Network
        repo.save(user, input("Arjun"))
        repo.sync(user)
        val before = remote.upserts.size
        assertEquals(SyncOutcome.Synced, repo.sync(user))
        assertEquals(before + 1, remote.upserts.size)
        assertEquals("SYNCED", row().syncState)
        assertEquals(1L, row().serverVersion)
    }

    @Test
    fun `A9 two quick saves give one local row and one push of the latest values`() = runTest {
        repo.save(user, input("Arjun"))
        repo.save(user, input("Arjun K"))
        assertEquals(1, dao.rows.value.size)
        assertEquals(SyncOutcome.Synced, repo.sync(user))
        assertEquals(listOf("Arjun K"), remote.upserts.map { it.displayName })
        assertEquals(user.value, remote.upserts.single().id)
    }

    @Test
    fun `A10 invalid input writes nothing and calls nothing`() = runTest {
        val result = repo.save(user, input("   "))
        assertTrue(result is SaveResult.Invalid)
        assertTrue(dao.rows.value.isEmpty())
        assertEquals(SyncOutcome.NothingToSync, repo.sync(user))
        assertTrue(remote.upserts.isEmpty())
    }

    @Test
    fun `A17 an edit made while a push is in flight still syncs`() = runTest {
        repo.save(user, input("v1"))
        remote.gate = CompletableDeferred()
        val firstSync = async { repo.sync(user) }
        remote.pushStarted.await()
        repo.save(user, input("v2"))          // edited while v1 is in flight
        remote.gate!!.complete(Unit)
        assertEquals(SyncOutcome.Synced, firstSync.await())
        assertEquals(listOf("v1", "v2"), remote.upserts.map { it.displayName })
        assertEquals("v2", row().displayName)
        assertEquals("SYNCED", row().syncState)
        assertEquals("v2", remote.server!!.displayName)
    }

    @Test
    fun `A18 a newer server copy replaces the local row`() = runTest {
        // Another device already saved a newer version.
        remote.server = ProfileDto(
            id = user.value, displayName = "From tablet", primaryRole = "batter", battingHand = "left",
            bowlingStyles = listOf("off_spin"), weekStart = 7, weightUnit = "lb", timeZone = "Europe/London",
            adultConfirmedAt = "2026-10-01T09:00:00+00:00", clientModifiedAt = "2026-10-13T08:00:00+00:00", version = 4,
        )
        repo.save(user, input("Stale phone edit"))   // clock is 2026-10-12, older than the server copy
        assertEquals(SyncOutcome.AdoptedServerCopy, repo.sync(user))
        val local = row()
        assertEquals("From tablet", local.displayName)
        assertEquals("SYNCED", local.syncState)
        assertEquals(4L, local.serverVersion)
        assertEquals("Europe/London", local.timeZone)
    }

    @Test
    fun `server refusal marks FAILED, keeps the data, and a new edit makes it PENDING again`() = runTest {
        remote.nextUpsertResult = RemoteResult.Rejected("violates check constraint")
        repo.save(user, input("Arjun"))
        assertEquals(SyncOutcome.Failed("violates check constraint"), repo.sync(user))
        assertEquals("FAILED", row().syncState)
        assertEquals("Arjun", row().displayName)
        // FAILED rows are not retried automatically...
        assertEquals(SyncOutcome.Failed("violates check constraint"), repo.sync(user))
        assertEquals(1, remote.upserts.size)
        // ...but an edit or a manual "Sync now" retries.
        repo.save(user, input("Arjun 2"))
        assertEquals(SyncOutcome.Synced, repo.sync(user))
    }

    @Test
    fun `optional details stay not recorded`() = runTest {
        repo.save(user, input("Arjun"))
        val r = row()
        assertNull(r.battingHand)
        assertEquals("", r.bowlingStyles)
        assertTrue(remote.upserts.isEmpty())
        repo.sync(user)
        assertEquals(emptyList<String>(), remote.upserts.single().bowlingStyles)
        assertNull(remote.upserts.single().battingHand)
    }

    @Test
    fun `restore on a new device brings the server profile down as SYNCED`() = runTest {
        remote.server = ProfileDto(
            id = user.value, displayName = "Arjun", primaryRole = "bowler", weekStart = 1, weightUnit = "kg",
            timeZone = "Asia/Kolkata", adultConfirmedAt = "2026-10-01T09:00:00+00:00",
            clientModifiedAt = "2026-10-11T09:00:00.123+00:00", version = 2,
        )
        assertEquals(RestoreResult.Restored, repo.restoreFromServer(user))
        assertEquals("SYNCED", row().syncState)
        assertEquals(Instant.parse("2026-10-11T09:00:00.123Z").toEpochMilli(), row().clientModifiedAt)
        assertEquals(RestoreResult.HasLocalProfile, repo.restoreFromServer(user))
    }

    @Test
    fun `restore reports offline without touching local data`() = runTest {
        remote.fetchResult = RemoteResult.Network
        assertEquals(RestoreResult.Offline, repo.restoreFromServer(user))
        assertTrue(dao.rows.value.isEmpty())
    }
}
