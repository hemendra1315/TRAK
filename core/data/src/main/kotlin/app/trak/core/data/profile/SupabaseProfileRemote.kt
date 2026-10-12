package app.trak.core.data.profile

import app.trak.core.data.remote.SupabaseClientHolder
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.postgrest.from
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** PostgREST access to public.profiles. RLS on the server restricts every call to the caller's own row. */
@Singleton
class SupabaseProfileRemote @Inject constructor(
    private val holder: SupabaseClientHolder,
) : ProfileRemote {

    override suspend fun upsertOwn(row: ProfileWriteDto): RemoteResult<ProfileDto?> = call {
        from(TABLE).upsert(row) {
            onConflict = "id"
            select()
        }.decodeList<ProfileDto>().firstOrNull()
    }

    override suspend fun fetchOwn(userId: String): RemoteResult<ProfileDto?> = call {
        from(TABLE).select {
            filter { eq("id", userId) }
        }.decodeList<ProfileDto>().firstOrNull()
    }

    private suspend fun <T> call(block: suspend SupabaseClient.() -> T): RemoteResult<T> {
        val client = holder.client ?: return RemoteResult.Unauthorized
        return try {
            RemoteResult.Success(client.block())
        } catch (e: RestException) {
            when (e.statusCode) {
                401 -> RemoteResult.Unauthorized
                408, 429, in 500..599 -> RemoteResult.Network
                else -> RemoteResult.Rejected(e.error)
            }
        } catch (e: HttpRequestException) {
            RemoteResult.Network
        } catch (e: IOException) {
            RemoteResult.Network
        }
    }

    private companion object {
        const val TABLE = "profiles"
    }
}
