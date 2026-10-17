package app.trak

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Plain HTTP is allowed only in debug builds (for the local Supabase stack); release stays HTTPS-only. */
class CleartextPolicyTest {
    @Test
    fun `release manifest and resources never allow cleartext`() {
        assertFalse(File("src/main/AndroidManifest.xml").readText().contains("usesCleartextTraffic"))
        assertFalse(File("src/main/res/xml").listFiles().orEmpty().any { it.readText().contains("cleartextTrafficPermitted=\"true\"") })
        assertFalse(File("src/release").exists())
    }

    @Test
    fun `cleartext is enabled only through the debug source set`() {
        assertTrue(File("src/debug/AndroidManifest.xml").readText().contains("android:usesCleartextTraffic=\"true\""))
    }
}
