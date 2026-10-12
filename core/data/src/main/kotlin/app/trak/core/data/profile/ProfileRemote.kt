package app.trak.core.data.profile

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Outcome of a remote call, classified so the repository can decide what to keep and retry. */
sealed interface RemoteResult<out T> {
    data class Success<T>(val value: T) : RemoteResult<T>
    /** No connection or a transient server problem: keep data, retry later. */
    data object Network : RemoteResult<Nothing>
    /** Session missing or rejected: keep data, retry after sign-in. */
    data object Unauthorized : RemoteResult<Nothing>
    /** The server refused the data (validation/permission): keep data, mark FAILED, show reason. */
    data class Rejected(val message: String) : RemoteResult<Nothing>
}

/** Remote access to the signed-in player's own profile row. Implemented over Supabase PostgREST. */
interface ProfileRemote {
    /**
     * Idempotent upsert keyed on id, returning the stored row. Returns `Success(null)` when the
     * server ignored the write because it already holds a newer copy (stale-write guard).
     */
    suspend fun upsertOwn(row: ProfileWriteDto): RemoteResult<ProfileDto?>

    suspend fun fetchOwn(userId: String): RemoteResult<ProfileDto?>
}

/** Columns the client may write. Server-owned columns (created_at, updated_at, version) are absent. */
@Serializable
data class ProfileWriteDto(
    @SerialName("id") val id: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("primary_role") val primaryRole: String,
    @SerialName("batting_hand") val battingHand: String?,
    @SerialName("bowling_styles") val bowlingStyles: List<String>,
    @SerialName("week_start") val weekStart: Int,
    @SerialName("weight_unit") val weightUnit: String,
    @SerialName("time_zone") val timeZone: String,
    @SerialName("adult_confirmed_at") val adultConfirmedAt: String,
    @SerialName("client_modified_at") val clientModifiedAt: String,
)

/** A stored profile row as returned by the server. */
@Serializable
data class ProfileDto(
    @SerialName("id") val id: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("primary_role") val primaryRole: String,
    @SerialName("batting_hand") val battingHand: String? = null,
    @SerialName("bowling_styles") val bowlingStyles: List<String> = emptyList(),
    @SerialName("week_start") val weekStart: Int,
    @SerialName("weight_unit") val weightUnit: String,
    @SerialName("time_zone") val timeZone: String,
    @SerialName("adult_confirmed_at") val adultConfirmedAt: String,
    @SerialName("client_modified_at") val clientModifiedAt: String,
    @SerialName("version") val version: Long,
)
