package app.trak.profile

import app.trak.core.data.profile.SyncOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Sync now reports honestly: an attempt that didn't reach the server is never shown as done. */
class SyncNoticeTest {
    @Test
    fun `an attempt that did not reach the server is reported`() {
        assertEquals(SyncNotice.NOT_REACHED, syncNoticeFor(SyncOutcome.StillPending))
    }

    @Test
    fun `outcomes the status line already shows add no extra message`() {
        assertNull(syncNoticeFor(SyncOutcome.Synced))
        assertNull(syncNoticeFor(SyncOutcome.AdoptedServerCopy))
        assertNull(syncNoticeFor(SyncOutcome.NothingToSync))
        assertNull(syncNoticeFor(SyncOutcome.Failed("refused")))
    }
}
