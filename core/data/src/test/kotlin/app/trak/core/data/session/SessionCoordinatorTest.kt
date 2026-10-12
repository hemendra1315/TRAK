package app.trak.core.data.session

import app.trak.core.data.db.entity.ProfileEntity
import app.trak.core.data.fakes.FakeAccountStore
import app.trak.core.data.fakes.FakeAuthGateway
import app.trak.core.data.fakes.FakeProfileDao
import app.trak.core.model.UserId
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Account safety rules: tests A14 (data layer), A19, A20 and decision S1. */
class SessionCoordinatorTest {

    private val a = UserId("11111111-1111-1111-1111-111111111111")
    private val b = UserId("22222222-2222-2222-2222-222222222222")
    private val dao = FakeProfileDao()
    private val account = FakeAccountStore(dao)
    private val auth = FakeAuthGateway()
    private val coordinator = SessionCoordinator(auth, account)

    private suspend fun putProfile(owner: UserId, state: String) = dao.upsert(
        ProfileEntity(owner.value, "P", "bowler", null, "", 1, "kg", "Asia/Kolkata", 0, 1, state, null, null),
    )

    @Test
    fun `first sign-in claims the device`() = runTest {
        assertEquals(AccessDecision.Proceed, coordinator.onSignedIn(a))
        assertEquals(a.value, account.ownerId)
    }

    @Test
    fun `A20 same account signing back in keeps its unsynced data`() = runTest {
        account.ownerId = a.value
        putProfile(a, "PENDING")
        assertEquals(AccessDecision.Proceed, coordinator.onSignedIn(a))
        assertEquals(0, account.wipes)
        assertNotNull(dao.get(a.value))
    }

    @Test
    fun `A19 another account is blocked while the owner's changes are unsynced`() = runTest {
        account.ownerId = a.value
        putProfile(a, "PENDING")
        assertEquals(AccessDecision.BlockedByOtherAccount(1), coordinator.onSignedIn(b))
        assertEquals(0, account.wipes)
        assertEquals(a.value, account.ownerId)
        assertNotNull(dao.get(a.value))
    }

    @Test
    fun `another account replaces fully synced data from the previous owner`() = runTest {
        account.ownerId = a.value
        putProfile(a, "SYNCED")
        assertEquals(AccessDecision.Proceed, coordinator.onSignedIn(b))
        assertEquals(1, account.wipes)
        assertEquals(b.value, account.ownerId)
        assertEquals(null, dao.get(a.value))
    }

    @Test
    fun `failed changes count as unsynced and also block`() = runTest {
        account.ownerId = a.value
        putProfile(a, "FAILED")
        assertEquals(AccessDecision.BlockedByOtherAccount(1), coordinator.onSignedIn(b))
    }

    @Test
    fun `sign-out with nothing unsynced signs out and clears local data`() = runTest {
        account.ownerId = a.value
        putProfile(a, "SYNCED")
        assertEquals(SignOutCheck.SignedOut, coordinator.signOut())
        assertEquals(1, auth.signOutCalls)
        assertEquals(1, account.wipes)
    }

    @Test
    fun `S1 sign-out with unsynced changes does nothing until the player chooses`() = runTest {
        account.ownerId = a.value
        putProfile(a, "PENDING")
        assertEquals(SignOutCheck.HasUnsyncedChanges(1), coordinator.signOut())
        assertEquals(0, auth.signOutCalls)
        assertEquals(0, account.wipes)
    }

    @Test
    fun `keep on this phone ends the session but keeps the data owner-locked`() = runTest {
        account.ownerId = a.value
        putProfile(a, "PENDING")
        coordinator.signOutKeepingData()
        assertEquals(1, auth.signOutCalls)
        assertEquals(0, account.wipes)
        assertEquals(a.value, account.ownerId)
        assertEquals(AccessDecision.BlockedByOtherAccount(1), coordinator.onSignedIn(b))
    }

    @Test
    fun `deleting unsynced data requires the exact typed confirmation`() = runTest {
        account.ownerId = a.value
        putProfile(a, "PENDING")
        assertFalse(coordinator.deleteUnsyncedAndSignOut("delete"))
        assertFalse(coordinator.deleteUnsyncedAndSignOut(""))
        assertEquals(0, account.wipes)
        assertTrue(coordinator.deleteUnsyncedAndSignOut(" DELETE "))
        assertEquals(1, account.wipes)
        assertEquals(1, auth.signOutCalls)
    }

    @Test
    fun `new account can delete another account's unsynced data only with confirmation`() = runTest {
        account.ownerId = a.value
        putProfile(a, "PENDING")
        assertFalse(coordinator.deleteOtherAccountData("yes", b))
        assertEquals(a.value, account.ownerId)
        assertTrue(coordinator.deleteOtherAccountData("DELETE", b))
        assertEquals(b.value, account.ownerId)
        assertEquals(null, dao.get(a.value))
    }
}
