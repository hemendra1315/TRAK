package app.trak.core.data.integration

import app.trak.core.data.auth.AuthResult
import app.trak.core.data.auth.SupabaseAuthGateway
import app.trak.core.data.fakes.FakeClock
import app.trak.core.data.fakes.FakeProfileDao
import app.trak.core.data.profile.ProfileRepository
import app.trak.core.data.profile.ProfileWriteDto
import app.trak.core.data.profile.RemoteResult
import app.trak.core.data.profile.SaveResult
import app.trak.core.data.profile.SupabaseProfileRemote
import app.trak.core.data.profile.SyncOutcome
import app.trak.core.data.remote.SupabaseClientHolder
import app.trak.core.data.remote.SupabaseConfig
import app.trak.core.model.PrimaryRole
import app.trak.core.model.ProfileInput
import app.trak.core.model.UserId
import io.github.jan.supabase.auth.MemorySessionManager
import io.github.jan.supabase.auth.auth
import java.net.URL
import java.net.URLEncoder
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestScope
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

/**
 * Runs the real supabase-kt client against the LOCAL Supabase stack in CI (never a hosted project).
 * Skipped unless TRAK_IT_API_URL, TRAK_IT_KEY and TRAK_IT_MAIL_URL are set (the CI database job).
 *
 * Covers A22 through the app's own code (email code sign-in), B2 (stale write returns no row),
 * server-side validation surfacing as Rejected, and the full repository save + sync path.
 */
class SupabaseIntegrationTest {

    private val apiUrl = System.getenv("TRAK_IT_API_URL")
    private val key = System.getenv("TRAK_IT_KEY")
    private val mailUrl = System.getenv("TRAK_IT_MAIL_URL")

    private fun dto(id: String, name: String, modified: String) = ProfileWriteDto(
        id = id, displayName = name, primaryRole = "all_rounder", battingHand = null, bowlingStyles = emptyList(),
        weekStart = 1, weightUnit = "kg", timeZone = "Asia/Kolkata",
        adultConfirmedAt = "2026-10-12T09:00:00Z", clientModifiedAt = modified,
    )

    @Test
    fun `email code sign-in, profile upsert, stale guard and RLS through the app's own code`() = runBlocking {
        assumeTrue("Integration env not set; skipped outside the CI database job", apiUrl != null && key != null && mailUrl != null)

        val holder = SupabaseClientHolder(SupabaseConfig(apiUrl!!, key!!), MemorySessionManager(), lifecycleCallbacks = false)
        val auth = SupabaseAuthGateway(holder, MemorySessionManager())
        val remote = SupabaseProfileRemote(holder)
        val email = "it-${UUID.randomUUID().toString().take(8)}@trak.test"

        assertEquals(AuthResult.Success, auth.sendCode(email))
        // A wrong code is rejected and mapped for the UI (chance of colliding with the real code: 1 in 10^6).
        assertEquals(AuthResult.InvalidOrExpiredCode, auth.verifyCode(email, "000000"))
        val code = readCode(email)
        assertEquals(AuthResult.Success, auth.verifyCode(email, code))
        val userId = holder.client!!.auth.currentUserOrNull()!!.id

        val first = remote.upsertOwn(dto(userId, "Arjun", "2026-10-12T10:00:00Z"))
        assertTrue("first upsert: $first", first is RemoteResult.Success && first.value != null)
        assertEquals(1L, (first as RemoteResult.Success).value!!.version)

        val second = remote.upsertOwn(dto(userId, "Arjun K", "2026-10-12T10:05:00Z"))
        assertEquals(2L, (second as RemoteResult.Success).value!!.version)

        val stale = remote.upsertOwn(dto(userId, "Old", "2026-10-11T10:00:00Z"))
        assertEquals("stale write returns no row", RemoteResult.Success(null), stale)

        val fetched = remote.fetchOwn(userId) as RemoteResult.Success
        assertEquals("Arjun K", fetched.value!!.displayName)
        assertEquals(Instant.parse("2026-10-12T10:05:00Z"), java.time.OffsetDateTime.parse(fetched.value!!.clientModifiedAt).toInstant())

        val otherUsersId = UUID.randomUUID().toString()
        val forged = remote.upsertOwn(dto(otherUsersId, "Impostor", "2026-10-12T10:00:00Z"))
        assertTrue("writing another id must be refused: $forged", forged is RemoteResult.Rejected || forged is RemoteResult.Unauthorized)

        val blank = remote.upsertOwn(dto(userId, "   ", "2026-10-12T10:10:00Z"))
        assertTrue("server check constraint surfaces as Rejected: $blank", blank is RemoteResult.Rejected)

        // Full repository path on top of the real remote.
        val repo = ProfileRepository(FakeProfileDao(), remote, FakeClock(Instant.parse("2026-10-12T11:00:00Z")), TestScope())
        val input = ProfileInput("Arjun Repo", PrimaryRole.BOWLER, timeZoneId = "Asia/Kolkata",
            adultConfirmedAt = Instant.parse("2026-10-12T09:00:00Z"))
        assertEquals(SaveResult.Saved, repo.save(UserId(userId), input))
        assertEquals(SyncOutcome.Synced, repo.sync(UserId(userId)))

        auth.signOut()
        assertNull(holder.client!!.auth.currentUserOrNull())
        val afterSignOut = remote.fetchOwn(userId)
        assertTrue("signed-out reads see nothing: $afterSignOut",
            afterSignOut is RemoteResult.Unauthorized || afterSignOut == RemoteResult.Success(null))
    }

    /** Reads the newest 6-digit code for [email] from the local mail catcher (Mailpit, else Inbucket). */
    private suspend fun readCode(email: String): String {
        val json = Json { ignoreUnknownKeys = true }
        repeat(30) {
            val text = runCatching {
                val q = URLEncoder.encode("to:$email", "UTF-8")
                val search = json.parseToJsonElement(URL("$mailUrl/api/v1/search?query=$q").readText()).jsonObject
                val id = search["messages"]!!.jsonArray.first().jsonObject["ID"]!!.jsonPrimitive.content
                val msg = json.parseToJsonElement(URL("$mailUrl/api/v1/message/$id").readText()).jsonObject
                (msg["Text"]?.jsonPrimitive?.content ?: "") + (msg["HTML"]?.jsonPrimitive?.content ?: "")
            }.getOrNull() ?: runCatching {
                val local = email.substringBefore("@")
                val box = json.parseToJsonElement(URL("$mailUrl/api/v1/mailbox/$local").readText()).jsonArray
                val id = box.last().jsonObject["id"]!!.jsonPrimitive.content
                val msg = json.parseToJsonElement(URL("$mailUrl/api/v1/mailbox/$local/$id").readText()).jsonObject
                msg["body"]!!.jsonObject["text"]!!.jsonPrimitive.content
            }.getOrNull()
            val code = text?.let { Regex("\\b(\\d{6})\\b").find(it)?.groupValues?.get(1) }
            if (code != null) return code
            delay(1000)
        }
        error("No code found for $email")
    }
}
