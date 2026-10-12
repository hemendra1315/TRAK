package app.trak.core.model

import java.util.UUID

/** The authenticated user's id (Supabase auth.users.id). Never generated on the device. */
@JvmInline
value class UserId(val value: String) {
    init {
        require(isUuid(value)) { "UserId must be a UUID" }
    }
}

/**
 * Id of a client-created record. Generated on the device so offline inserts and retries are
 * idempotent: the same row always carries the same id to the server.
 */
@JvmInline
value class EntityId(val value: String) {
    init {
        require(isUuid(value)) { "EntityId must be a UUID" }
    }

    companion object {
        fun random(): EntityId = EntityId(UUID.randomUUID().toString())
    }
}

internal fun isUuid(value: String): Boolean =
    runCatching { UUID.fromString(value).toString() == value.lowercase() }.getOrDefault(false)
