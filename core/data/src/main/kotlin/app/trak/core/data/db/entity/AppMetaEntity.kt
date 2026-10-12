package app.trak.core.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** Small key/value table for device-local bookkeeping, e.g. which account owns the local data. */
@Entity(tableName = "app_meta")
data class AppMetaEntity(
    @PrimaryKey @ColumnInfo(name = "key") val key: String,
    @ColumnInfo(name = "value") val value: String,
)

object AppMetaKeys {
    /** The auth user id that owns every user row on this device (review fix B3). */
    const val OWNER_USER_ID = "owner_user_id"
}
