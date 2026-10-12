package app.trak.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/** Five simple tab icons drawn in-house (no icon library dependency). Tinted by the navigation bar. */
object TrakIcons {
    private fun icon(name: String, vararg paths: String): ImageVector =
        ImageVector.Builder(name = name, defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
            .apply { paths.forEach { addPath(pathData = addPathNodes(it), fill = SolidColor(Color.Black)) } }
            .build()

    val Home = icon("Home", "M12 3L2 11h3v9h5v-6h4v6h5v-9h3z")
    val Log = icon("Log", "M11 4h2v7h7v2h-7v7h-2v-7H4v-2h7z")
    val Progress = icon("Progress", "M4 13h4v7H4z", "M10 8h4v12h-4z", "M16 4h4v16h-4z")
    val Recovery = icon("Recovery", "M20 14.5A8 8 0 0 1 9.5 4a8 8 0 1 0 10.5 10.5z")
    val Profile = icon("Profile", "M12 12a4 4 0 1 0 0-8a4 4 0 0 0 0 8z", "M4 20c0-3.3 3.6-6 8-6s8 2.7 8 6v1H4z")
}
