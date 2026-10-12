package app.trak.core.data.fakes

import app.trak.core.data.TimeSource
import app.trak.core.data.auth.AuthGateway
import app.trak.core.data.auth.AuthResult
import app.trak.core.data.auth.AuthState
import app.trak.core.data.db.dao.ProfileDao
import app.trak.core.data.db.entity.ProfileEntity
import app.trak.core.data.profile.ProfileDto
import app.trak.core.data.profile.ProfileRemote
import app.trak.core.data.profile.ProfileWriteDto
import app.trak.core.data.profile.RemoteResult
import app.trak.core.data.session.AccountStore
import java.time.Instant
import java.time.OffsetDateTime
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** In-memory ProfileDao with the same compare-and-set semantics as the Room queries. */
class FakeProfileDao : ProfileDao {
    val rows = MutableStateFlow<Map<String, ProfileEntity>>(emptyMap())

    override fun observe(userId: String): Flow<ProfileEntity?> = rows.map { it[userId] }
    override suspend fun get(userId: String): ProfileEntity? = rows.value[userId]
    override suspend fun upsert(profile: ProfileEntity) { rows.value = rows.value + (profile.id to profile) }

    override suspend fun markSyncedIfUnchanged(userId: String, sentClientModifiedAt: Long, serverVersion: Long): Int {
        val r = rows.value[userId] ?: return 0
        if (r.clientModifiedAt != sentClientModifiedAt) return 0
        upsert(r.copy(syncState = "SYNCED", syncError = null, serverVersion = serverVersion)); return 1
    }

    override suspend fun markFailedIfUnchanged(userId: String, sentClientModifiedAt: Long, error: String): Int {
        val r = rows.value[userId] ?: return 0
        if (r.clientModifiedAt != sentClientModifiedAt) return 0
        upsert(r.copy(syncState = "FAILED", syncError = error)); return 1
    }

    override suspend fun unsyncedCount(): Int = rows.value.values.count { it.syncState != "SYNCED" }
    override fun observeUnsyncedCount(): Flow<Int> = rows.map { m -> m.values.count { it.syncState != "SYNCED" } }

    fun clear() { rows.value = emptyMap() }
}

/**
 * Scriptable remote. By default behaves like the real server (upsert stores and returns the row,
 * ignoring stale writes). [gate] lets a test hold a push in flight.
 */
class FakeProfileRemote : ProfileRemote {
    val upserts = mutableListOf<ProfileWriteDto>()
    var server: ProfileDto? = null
    var nextUpsertResult: RemoteResult<ProfileDto?>? = null
    var fetchResult: RemoteResult<ProfileDto?>? = null
    var gate: CompletableDeferred<Unit>? = null
    val pushStarted = CompletableDeferred<Unit>()

    override suspend fun upsertOwn(row: ProfileWriteDto): RemoteResult<ProfileDto?> {
        upserts += row
        if (!pushStarted.isCompleted) pushStarted.complete(Unit)
        gate?.let { it.await(); gate = null }
        nextUpsertResult?.let { nextUpsertResult = null; return it }
        val current = server
        if (current != null && OffsetDateTime.parse(row.clientModifiedAt).toInstant() < OffsetDateTime.parse(current.clientModifiedAt).toInstant()) {
            return RemoteResult.Success(null) // stale write ignored, like the server trigger
        }
        val stored = row.toStored(version = (current?.version ?: 0) + 1)
        server = stored
        return RemoteResult.Success(stored)
    }

    override suspend fun fetchOwn(userId: String): RemoteResult<ProfileDto?> =
        fetchResult ?: RemoteResult.Success(server?.takeIf { it.id == userId })

    private fun ProfileWriteDto.toStored(version: Long) = ProfileDto(
        id, displayName, primaryRole, battingHand, bowlingStyles, weekStart, weightUnit, timeZone,
        adultConfirmedAt, clientModifiedAt.replace("Z", "+00:00"), version,
    )
}

class FakeClock(var now: Instant = Instant.parse("2026-10-12T10:00:00Z")) : TimeSource {
    override fun now(): Instant = now.also { now = now.plusMillis(1000) }
}

class FakeAuthGateway : AuthGateway {
    override val state = MutableStateFlow<AuthState>(AuthState.Initializing)
    var signOutCalls = 0
    override suspend fun sendCode(email: String) = AuthResult.Success
    override suspend fun verifyCode(email: String, code: String) = AuthResult.Success
    override suspend fun signOut() { signOutCalls++; state.value = AuthState.SignedOut(byUser = true) }
}

class FakeAccountStore(private val dao: FakeProfileDao) : AccountStore {
    var ownerId: String? = null
    var wipes = 0
    override suspend fun owner() = ownerId
    override suspend fun setOwner(userId: String) { ownerId = userId }
    override suspend fun unsyncedCount() = dao.unsyncedCount()
    override suspend fun wipeUserData() { wipes++; dao.clear(); ownerId = null }
}
