package app.trak.core.data.profile

import app.trak.core.data.ApplicationScope
import app.trak.core.data.TimeSource
import app.trak.core.data.db.dao.ProfileDao
import app.trak.core.model.Profile
import app.trak.core.model.ProfileInput
import app.trak.core.model.ProfileValidation
import app.trak.core.model.SyncState
import app.trak.core.model.UserId
import app.trak.core.model.validation.ValidationError
import app.trak.core.model.validation.ValidationResult
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** The local profile plus its sync bookkeeping, as the UI shows it. */
data class LocalProfile(val profile: Profile, val syncState: SyncState, val syncError: String?)

sealed interface SaveResult {
    data class Invalid(val errors: List<ValidationError>) : SaveResult
    /** Written to the phone. Sync runs separately; the change is visible immediately. */
    data object Saved : SaveResult
}

sealed interface SyncOutcome {
    data object NothingToSync : SyncOutcome
    data object Synced : SyncOutcome
    /** The server held a newer copy, which replaced the local row (review fix B2). */
    data object AdoptedServerCopy : SyncOutcome
    /** Offline or not signed in: the change stays PENDING and is retried later. */
    data object StillPending : SyncOutcome
    data class Failed(val reason: String?) : SyncOutcome
}

sealed interface RestoreResult {
    data object HasLocalProfile : RestoreResult
    data object Restored : RestoreResult
    data object NotFoundOnServer : RestoreResult
    data object Offline : RestoreResult
}

/**
 * Phase 1 profile persistence and sync (plan section 5). Room is the source of truth; the server is
 * written with an idempotent upsert. Replaced by the outbox in Phase 2.
 *
 * Rules:
 * - save(): validate, write the row as PENDING with a strictly increasing client_modified_at.
 * - sync(): one at a time (mutex). After a push, mark SYNCED only if the row is unchanged since it
 *   was sent (B1). If the server skipped the write because it holds a newer copy, adopt that copy (B2).
 *   Network/auth problems leave the row PENDING; a server refusal marks it FAILED with the reason.
 *   Data is never deleted by sync.
 */
@Singleton
class ProfileRepository @Inject constructor(
    private val dao: ProfileDao,
    private val remote: ProfileRemote,
    private val clock: TimeSource,
    @ApplicationScope private val appScope: CoroutineScope,
) {
    private val syncMutex = Mutex()

    fun observe(userId: UserId): Flow<LocalProfile?> = dao.observe(userId.value).map { row ->
        val profile = row?.toDomain() ?: return@map null
        LocalProfile(profile, SyncState.valueOf(row.syncState), row.syncError)
    }

    suspend fun save(userId: UserId, input: ProfileInput): SaveResult {
        val validated = when (val v = ProfileValidation.validate(input)) {
            is ValidationResult.Invalid -> return SaveResult.Invalid(v.errors)
            is ValidationResult.Valid -> v.value
        }
        val previous = dao.get(userId.value)
        val now = clock.now().truncatedTo(ChronoUnit.MILLIS).toEpochMilli()
        // Strictly increasing per row, so two edits within one millisecond are still ordered.
        val modifiedAt = if (previous != null && now <= previous.clientModifiedAt) previous.clientModifiedAt + 1 else now
        dao.upsert(validated.toEntity(userId, modifiedAt, SyncState.PENDING, previous?.serverVersion))
        return SaveResult.Saved
    }

    /** Fire-and-forget sync on the application scope, so it outlives the screen that saved. */
    fun requestSync(userId: UserId, includeFailed: Boolean = false) {
        appScope.launch { sync(userId, includeFailed) }
    }

    /**
     * Retries a PENDING profile each time the phone comes (back) online, until cancelled. Nothing is
     * attempted while offline. FAILED rows are not retried here: the server refused them, so retrying
     * unchanged data can't help; the player sees the reason and can edit or tap Sync now.
     */
    suspend fun syncWhenOnline(userId: UserId, online: Flow<Boolean>) {
        online.distinctUntilChanged().filter { it }.collect { sync(userId) }
    }

    suspend fun sync(userId: UserId, includeFailed: Boolean = false): SyncOutcome {
        syncMutex.withLock { return syncLocked(userId.value, includeFailed) }
    }

    private suspend fun syncLocked(id: String, includeFailed: Boolean): SyncOutcome {
        repeat(MAX_ROUNDS) {
            val row = dao.get(id) ?: return SyncOutcome.NothingToSync
            when (SyncState.valueOf(row.syncState)) {
                SyncState.SYNCED -> return SyncOutcome.Synced
                SyncState.FAILED -> if (!includeFailed) return SyncOutcome.Failed(row.syncError)
                SyncState.PENDING -> Unit
            }
            val sent = row.clientModifiedAt
            when (val pushed = remote.upsertOwn(row.toWriteDto())) {
                is RemoteResult.Success -> {
                    val stored = pushed.value
                    if (stored != null) {
                        if (dao.markSyncedIfUnchanged(id, sent, stored.version) > 0) return SyncOutcome.Synced
                        // Edited while the push was in flight: loop and push the newer version.
                    } else {
                        // The server ignored a stale write; fetch and adopt its newer copy.
                        when (val fetched = remote.fetchOwn(id)) {
                            is RemoteResult.Success -> {
                                val server = fetched.value
                                    ?: return SyncOutcome.Failed("Server returned no profile after a skipped write")
                                if (dao.adoptIfUnchanged(sent, server.toEntity(SyncState.SYNCED))) {
                                    return SyncOutcome.AdoptedServerCopy
                                }
                            }
                            is RemoteResult.Rejected -> return SyncOutcome.Failed(fetched.message)
                            RemoteResult.Network, RemoteResult.Unauthorized -> return SyncOutcome.StillPending
                        }
                    }
                }
                RemoteResult.Network, RemoteResult.Unauthorized -> return SyncOutcome.StillPending
                is RemoteResult.Rejected -> {
                    if (dao.markFailedIfUnchanged(id, sent, pushed.message) > 0) return SyncOutcome.Failed(pushed.message)
                    // Edited meanwhile: the new version gets its own attempt.
                }
            }
        }
        return SyncOutcome.StillPending
    }

    /** After sign-in on a device with no local profile: bring the server copy down if there is one. */
    suspend fun restoreFromServer(userId: UserId): RestoreResult {
        if (dao.get(userId.value) != null) return RestoreResult.HasLocalProfile
        return when (val fetched = remote.fetchOwn(userId.value)) {
            is RemoteResult.Success -> {
                val server = fetched.value ?: return RestoreResult.NotFoundOnServer
                dao.upsert(server.toEntity(SyncState.SYNCED))
                RestoreResult.Restored
            }
            else -> RestoreResult.Offline
        }
    }

    private companion object {
        const val MAX_ROUNDS = 3
    }
}
