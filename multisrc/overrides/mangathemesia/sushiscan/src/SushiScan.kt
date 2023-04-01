package eu.kanade.tachiyomi.extension.fr.sushiscan

import eu.kanade.tachiyomi.multisrc.mangathemesia.MangaThemesia
import eu.kanade.tachiyomi.source.model.SManga
import java.text.SimpleDateFormat
import java.util.Locale

class SushiScan : MangaThemesia("Sushi-Scan", "https://sushiscan.net", "fr", dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.FRENCH)) {
    override val altNamePrefix = "Nom alternatif : "
    override val seriesAuthorSelector = ".infotable tr:contains(Auteur) td:last-child"
    override val seriesStatusSelector = ".infotable tr:contains(Statut) td:last-child"
    override fun String?.parseStatus(): Int = when {
        this == null -> SManga.UNKNOWN
        this.contains("En Cours", ignoreCase = true) -> SManga.ONGOING
        this.contains("Terminé", ignoreCase = true) -> SManga.COMPLETED
        else -> SManga.UNKNOWN
    }
}
