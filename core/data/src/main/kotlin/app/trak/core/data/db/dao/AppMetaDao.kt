package app.trak.core.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import app.trak.core.data.db.entity.AppMetaEntity

@Dao
interface AppMetaDao {
    @Query("SELECT value FROM app_meta WHERE `key` = :key")
    suspend fun get(key: String): String?

    @Upsert
    suspend fun put(entry: AppMetaEntity)

    @Query("DELETE FROM app_meta WHERE `key` = :key")
    suspend fun remove(key: String)
}
