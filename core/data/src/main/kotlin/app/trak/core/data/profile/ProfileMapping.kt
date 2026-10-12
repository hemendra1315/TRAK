package app.trak.core.data.profile

import app.trak.core.data.db.entity.ProfileEntity
import app.trak.core.model.BattingHand
import app.trak.core.model.BowlingStyle
import app.trak.core.model.PrimaryRole
import app.trak.core.model.Profile
import app.trak.core.model.SyncState
import app.trak.core.model.UserId
import app.trak.core.model.ValidatedProfile
import app.trak.core.model.WeightUnit
import java.time.DayOfWeek
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId

internal fun parseServerInstant(value: String): Instant = OffsetDateTime.parse(value).toInstant()

internal fun ValidatedProfile.toEntity(
    userId: UserId,
    clientModifiedAtMillis: Long,
    syncState: SyncState,
    serverVersion: Long?,
) = ProfileEntity(
    id = userId.value,
    displayName = displayName,
    primaryRole = primaryRole.code,
    battingHand = battingHand?.code,
    bowlingStyles = bowlingStyles.sortedBy { it.ordinal }.joinToString(",") { it.code },
    weekStart = weekStart.value,
    weightUnit = weightUnit.code,
    timeZone = timeZone.id,
    adultConfirmedAt = adultConfirmedAt.toEpochMilli(),
    clientModifiedAt = clientModifiedAtMillis,
    syncState = syncState.name,
    syncError = null,
    serverVersion = serverVersion,
)

internal fun ProfileEntity.toWriteDto() = ProfileWriteDto(
    id = id,
    displayName = displayName,
    primaryRole = primaryRole,
    battingHand = battingHand,
    bowlingStyles = bowlingStyleCodes(),
    weekStart = weekStart,
    weightUnit = weightUnit,
    timeZone = timeZone,
    adultConfirmedAt = Instant.ofEpochMilli(adultConfirmedAt).toString(),
    clientModifiedAt = Instant.ofEpochMilli(clientModifiedAt).toString(),
)

internal fun ProfileDto.toEntity(syncState: SyncState) = ProfileEntity(
    id = id,
    displayName = displayName,
    primaryRole = primaryRole,
    battingHand = battingHand,
    bowlingStyles = bowlingStyles.joinToString(","),
    weekStart = weekStart,
    weightUnit = weightUnit,
    timeZone = timeZone,
    adultConfirmedAt = parseServerInstant(adultConfirmedAt).toEpochMilli(),
    clientModifiedAt = parseServerInstant(clientModifiedAt).toEpochMilli(),
    syncState = syncState.name,
    syncError = null,
    serverVersion = version,
)

private fun ProfileEntity.bowlingStyleCodes(): List<String> =
    if (bowlingStyles.isBlank()) emptyList() else bowlingStyles.split(",")

/** Domain view of a stored row. Returns null if the row holds values this app version can't read. */
internal fun ProfileEntity.toDomain(): Profile? {
    val role = PrimaryRole.fromCode(primaryRole) ?: return null
    val unit = WeightUnit.fromCode(weightUnit) ?: return null
    val zone = runCatching { ZoneId.of(timeZone) }.getOrNull() ?: return null
    return Profile(
        userId = UserId(id),
        displayName = displayName,
        primaryRole = role,
        battingHand = BattingHand.fromCode(battingHand),
        bowlingStyles = bowlingStyleCodes().mapNotNull { BowlingStyle.fromCode(it) }.toSet(),
        weekStart = DayOfWeek.of(weekStart),
        weightUnit = unit,
        timeZone = zone,
        adultConfirmedAt = Instant.ofEpochMilli(adultConfirmedAt),
        clientModifiedAt = Instant.ofEpochMilli(clientModifiedAt),
    )
}
