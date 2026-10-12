package app.trak

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

/**
 * Acceptance test A21 (review fix B4): the database, DataStore files (settings, session) and shared
 * preferences are excluded from cloud backup and device transfer, and the manifest uses the rules.
 */
class BackupRulesTest {

    private val required = setOf("database", "file", "sharedpref", "root")

    private fun excludedDomains(file: String, section: String?): Set<String> {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(File(file))
        val scope = if (section == null) doc.documentElement
        else doc.getElementsByTagName(section).item(0) as Element
        val nodes = scope.getElementsByTagName("exclude")
        return (0 until nodes.length).map { (nodes.item(it) as Element) }
            .filter { it.getAttribute("path") == "." }
            .map { it.getAttribute("domain") }
            .toSet()
    }

    @Test
    fun `cloud backup excludes all local data`() {
        assertTrue(excludedDomains("src/main/res/xml/data_extraction_rules.xml", "cloud-backup").containsAll(required))
    }

    @Test
    fun `device transfer excludes all local data`() {
        assertTrue(excludedDomains("src/main/res/xml/data_extraction_rules.xml", "device-transfer").containsAll(required))
    }

    @Test
    fun `legacy full backup excludes all local data`() {
        assertTrue(excludedDomains("src/main/res/xml/backup_rules.xml", null).containsAll(required))
    }

    @Test
    fun `manifest wires the rules and disables backup`() {
        val doc = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
            .newDocumentBuilder().parse(File("src/main/AndroidManifest.xml"))
        val app = doc.getElementsByTagName("application").item(0) as Element
        val ns = "http://schemas.android.com/apk/res/android"
        assertEquals("false", app.getAttributeNS(ns, "allowBackup"))
        assertEquals("@xml/data_extraction_rules", app.getAttributeNS(ns, "dataExtractionRules"))
        assertEquals("@xml/backup_rules", app.getAttributeNS(ns, "fullBackupContent"))
    }
}
