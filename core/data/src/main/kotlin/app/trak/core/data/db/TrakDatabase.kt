package app.trak.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import app.trak.core.data.db.dao.AppMetaDao
import app.trak.core.data.db.dao.ProfileDao
import app.trak.core.data.db.entity.AppMetaEntity
import app.trak.core.data.db.entity.ProfileEntity

/**
 * Version 1 (unreleased; no installs exist yet). Schema JSON is exported to core/data/schemas and
 * committed; CI fails if it drifts. Migrations are always explicit; destructive fallback is never
 * enabled.
 */
@Database(entities = [ProfileEntity::class, AppMetaEntity::class], version = 1, exportSchema = true)
abstract class TrakDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun appMetaDao(): AppMetaDao

    companion object {
        const val NAME = "trak.db"
    }
}
