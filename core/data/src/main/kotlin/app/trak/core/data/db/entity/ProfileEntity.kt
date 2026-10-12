package app.trak.core.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Local copy of the signed-in player's profile (one row).
 * Follows the TRAK table conventions: server-mirrored fields plus sync bookkeeping.
 * Timestamps are epoch milliseconds (UTC).
 */
@Entity(tableName = "profile")
data class ProfileEntity(
    @PrimaryKey
    @ColumnInfo(name = "id") val id: String, // = auth user id (UUID)
    @ColumnInfo(name = "display_name") val displayName: String,
    @ColumnInfo(name = "primary_role") val primaryRole: String,
    @ColumnInfo(name = "batting_hand") val battingHand: String?,
    @ColumnInfo(name = "bowling_styles") val bowlingStyles: String, // comma-separated codes; empty = none recorded
    @ColumnInfo(name = "week_start") val weekStart: Int, // ISO day, 1 = Monday
    @ColumnInfo(name = "weight_unit") val weightUnit: String,
    @ColumnInfo(name = "time_zone") val timeZone: String,
    @ColumnInfo(name = "adult_confirmed_at") val adultConfirmedAt: Long,
    @ColumnInfo(name = "client_modified_at") val clientModifiedAt: Long,
    @ColumnInfo(name = "sync_state") val syncState: String, // PENDING | SYNCED | FAILED
    @ColumnInfo(name = "sync_error") val syncError: String?,
    @ColumnInfo(name = "server_version") val serverVersion: Long?,
)
