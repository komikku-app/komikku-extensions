package eu.kanade.tachiyomi.extension.ar.arflamescans

import eu.kanade.tachiyomi.multisrc.wpmangareader.WPMangaReader
import eu.kanade.tachiyomi.source.model.SManga
import java.text.SimpleDateFormat
import java.util.Locale

class ARFlameScans : WPMangaReader(
    "AR FlameScans", "https://ar.flamescans.org", "ar",
    mangaUrlDirectory = "/series",
    dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.US)
) {
    override fun parseStatus(status: String) = when {
        status.contains("مستمر") -> SManga.ONGOING
        status.contains("مكتمل") -> SManga.COMPLETED
        else -> SManga.UNKNOWN
    }
}
