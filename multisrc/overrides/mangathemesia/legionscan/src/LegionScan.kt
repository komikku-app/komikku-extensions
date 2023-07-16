package eu.kanade.tachiyomi.extension.es.legionscan

import eu.kanade.tachiyomi.multisrc.mangathemesia.MangaThemesia
import java.text.SimpleDateFormat
import java.util.Locale

class LegionScan : MangaThemesia(
    "Legion Scan",
    "https://legionscans.com",
    "es",
    dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale("es")),
) {
    // Theme changed from Madara to MangaThemesia
    override val versionId = 2
    override val seriesAltNameSelector = ".infotable tr:contains(alt) td:last-child"
}
