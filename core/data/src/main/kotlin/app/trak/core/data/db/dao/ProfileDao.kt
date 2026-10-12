package app.trak.core.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import app.trak.core.data.db.entity.ProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profile WHERE id = :userId")
    fun observe(userId: String): Flow<ProfileEntity?>

    @Query("SELECT * FROM profile WHERE id = :userId")
    suspend fun get(userId: String): ProfileEntity?

    @Upsert
    suspend fun upsert(profile: ProfileEntity)

    /**
     * Compare-and-set after a successful push (review fix B1): marks the row SYNCED only if nobody
     * edited it while the push was in flight. Returns rows updated (0 or 1).
     */
    @Query(
        "UPDATE profile SET sync_state = 'SYNCED', sync_error = NULL, server_version = :serverVersion " +
            "WHERE id = :userId AND client_modified_at = :sentClientModifiedAt"
    )
    suspend fun markSyncedIfUnchanged(userId: String, sentClientModifiedAt: Long, serverVersion: Long): Int

    @Query(
        "UPDATE profile SET sync_state = 'FAILED', sync_error = :error " +
            "WHERE id = :userId AND client_modified_at = :sentClientModifiedAt"
    )
    suspend fun markFailedIfUnchanged(userId: String, sentClientModifiedAt: Long, error: String): Int

    /** Replaces the local row with the server copy only if the local row is unchanged since [sentClientModifiedAt]. */
    @Transaction
    suspend fun adoptIfUnchanged(sentClientModifiedAt: Long, server: ProfileEntity): Boolean {
        val current = get(server.id) ?: return false
        if (current.clientModifiedAt != sentClientModifiedAt) return false
        upsert(server)
        return true
    }

    /** Rows with changes the server hasn't accepted yet (PENDING or FAILED). */
    @Query("SELECT COUNT(*) FROM profile WHERE sync_state != 'SYNCED'")
    suspend fun unsyncedCount(): Int

    @Query("SELECT COUNT(*) FROM profile WHERE sync_state != 'SYNCED'")
    fun observeUnsyncedCount(): Flow<Int>
}
