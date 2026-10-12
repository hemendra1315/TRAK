package app.trak.core.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import app.trak.core.data.db.entity.ProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profile WHERE id = :userId")
    fun observe(userId: String): Flow<ProfileEntity?>

    @Upsert
    suspend fun upsert(profile: ProfileEntity)

    /**
     * Compare-and-set used after a successful push (review fix B1): marks the row SYNCED only if
     * nobody edited it while the push was in flight. Returns the number of rows updated (0 or 1).
     */
    @Query(
        "UPDATE profile SET sync_state = 'SYNCED', sync_error = NULL, server_version = :serverVersion " +
            "WHERE id = :userId AND client_modified_at = :sentClientModifiedAt"
    )
    suspend fun markSyncedIfUnchanged(userId: String, sentClientModifiedAt: Long, serverVersion: Long): Int
}
