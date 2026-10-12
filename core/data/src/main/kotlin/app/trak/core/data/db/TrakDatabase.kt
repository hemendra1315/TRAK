package app.trak.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import app.trak.core.data.db.dao.ProfileDao
import app.trak.core.data.db.entity.ProfileEntity

/**
 * Version 1. Schema JSON is exported to core/data/schemas and committed.
 * Migrations are always explicit; destructive fallback is never enabled.
 */
@Database(entities = [ProfileEntity::class], version = 1, exportSchema = true)
abstract class TrakDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
}
