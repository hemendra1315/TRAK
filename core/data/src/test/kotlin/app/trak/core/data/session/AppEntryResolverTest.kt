package app.trak.core.data.session

import app.trak.core.data.auth.AuthState
import app.trak.core.data.db.entity.ProfileEntity
import app.trak.core.data.fakes.FakeAccountStore
import app.trak.core.data.fakes.FakeAuthGateway
import app.trak.core.data.fakes.FakeClock
import app.trak.core.data.fakes.FakeProfileDao
import app.trak.core.data.fakes.FakeProfileRemote
import app.trak.core.data.profile.ProfileDto
import app.trak.core.data.profile.ProfileRepository
import app.trak.core.data.profile.RemoteResult
import app.trak.core.model.PrimaryRole
import app.trak.core.model.ProfileInput
import app.trak.core.model.UserId
import java.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/** Acceptance test A15 (session restore) and the routing rules around it. */
class AppEntryResolverTest {

    private val a = UserId("11111111-1111-1111-1111-111111111111")
    private val b = UserId("22222222-2222-2222-2222-222222222222")
    private val dao = FakeProfileDao()
    private val remote = FakeProfileRemote()
    private val auth = FakeAuthGateway()
    private val account = FakeAccountStore(dao)
    private val repo = ProfileRepository(dao, remote, FakeClock(), TestScope())
    private val resolver = AppEntryResolver(auth, SessionCoordinator(auth, account), repo)

    private suspend fun next(predicate: (AppEntryState) -> Boolean = { it != AppEntryState.Loading }) =
        resolver.state.first(predicate)

    private suspend fun localProfile(owner: UserId, state: String = "SYNCED") {
        account.ownerId = owner.value
        dao.upsert(ProfileEntity(owner.value, "Arjun", "bowler", null, "", 1, "kg", "Asia/Kolkata", 0, 1, state, null, 1))
    }

    @Test
    fun `A15 stored session with a local profile opens the main tabs without sign-in`() = runTest {
        localProfile(a)
        auth.state.value = AuthState.SignedIn(a, "a@x.in")
        assertEquals(AppEntryState.Ready(a, offline = false), next())
    }

    @Test
    fun `A15 offline launch with a stored session still opens the app`() = runTest {
        localProfile(a, state = "PENDING")
        remote.nextUpsertResult = RemoteResult.Network
        auth.state.value = AuthState.SignedIn(a, null, offline = true)
        assertEquals(AppEntryState.Ready(a, offline = true), next())
    }

    @Test
    fun `A15 expired session goes to sign-in and keeps local data`() = runTest {
        localProfile(a, state = "PENDING")
        auth.state.value = AuthState.SignedOut(byUser = false)
        assertEquals(AppEntryState.SignedOut(byUser = false), next())
        assertEquals("PENDING", dao.get(a.value)!!.syncState)
    }

    @Test
    fun `new device restores the server profile instead of onboarding`() = runTest {
        remote.server = ProfileDto(a.value, "Arjun", "bowler", null, emptyList(), 1, "kg", "Asia/Kolkata",
            "2026-10-01T09:00:00+00:00", "2026-10-11T09:00:00+00:00", 3)
        auth.state.value = AuthState.SignedIn(a, null)
        assertEquals(AppEntryState.Ready(a, offline = false), next())
    }

    @Test
    fun `no profile anywhere leads to onboarding, which then opens the app`() = runTest {
        auth.state.value = AuthState.SignedIn(a, null)
        assertEquals(AppEntryState.NeedsOnboarding(a), next())
        repo.save(a, ProfileInput("Arjun", PrimaryRole.BOWLER, timeZoneId = "Asia/Kolkata",
            adultConfirmedAt = Instant.parse("2026-10-12T09:00:00Z")))
        assertEquals(AppEntryState.Ready(a, offline = false), next { it is AppEntryState.Ready })
    }

    @Test
    fun `new device that can't reach the server does not offer onboarding`() = runTest {
        remote.fetchResult = RemoteResult.Network
        auth.state.value = AuthState.SignedIn(a, null)
        assertEquals(AppEntryState.ProfileCheckOffline(a), next())
    }

    @Test
    fun `another account with unsynced data on the phone is guarded`() = runTest {
        localProfile(a, state = "PENDING")
        auth.state.value = AuthState.SignedIn(b, null)
        assertEquals(AppEntryState.BlockedByOtherAccount(b, 1), next())
    }
}
