package eu.kanade.tachiyomi.extension.es.legionscan

import eu.kanade.tachiyomi.multisrc.wpmangareader.WPMangaReader
import java.text.SimpleDateFormat
import java.util.Locale

class LegionScan : WPMangaReader("Legion Scan", "https://legionscans.com", "es", dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale("es"))) {

    override val seriesTypeSelector = ".imptdt:contains(Tipo) :last-child"
    override val seriesStatusSelector = ".tsinfo .imptdt:contains(Estado) i"
}
