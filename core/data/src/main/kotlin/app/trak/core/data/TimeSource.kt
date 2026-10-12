package app.trak.core.data

import java.time.Instant
import javax.inject.Qualifier

/** Injectable clock so time-dependent rules are testable. */
fun interface TimeSource {
    fun now(): Instant

    companion object {
        val System = TimeSource { Instant.now() }
    }
}

/** Qualifier for the process-wide CoroutineScope used for work that must outlive a screen (sync). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope
