package app.trak.core.data.session

import app.trak.core.data.db.TrakDatabase
import app.trak.core.data.db.entity.AppMetaEntity
import app.trak.core.data.db.entity.AppMetaKeys
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Which account owns the local data, how much of it is unsynced, and wiping it. */
interface AccountStore {
    suspend fun owner(): String?
    suspend fun setOwner(userId: String)

    /**
     * Rows the server hasn't accepted yet (PENDING or FAILED), across all user tables.
     * Phase 2 adds the outbox count here.
     */
    suspend fun unsyncedCount(): Int

    /** Deletes every user row on this device, including the owner marker. */
    suspend fun wipeUserData()
}

@Singleton
class RoomAccountStore @Inject constructor(private val db: TrakDatabase) : AccountStore {
    override suspend fun owner(): String? = db.appMetaDao().get(AppMetaKeys.OWNER_USER_ID)

    override suspend fun setOwner(userId: String) =
        db.appMetaDao().put(AppMetaEntity(AppMetaKeys.OWNER_USER_ID, userId))

    override suspend fun unsyncedCount(): Int = db.profileDao().unsyncedCount()

    override suspend fun wipeUserData() = withContext(Dispatchers.IO) { db.clearAllTables() }
}
