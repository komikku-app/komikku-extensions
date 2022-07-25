package eu.kanade.tachiyomi.extension.fr.sushiscan

import eu.kanade.tachiyomi.multisrc.wpmangareader.WPMangaReader
import eu.kanade.tachiyomi.source.model.SManga
import java.text.SimpleDateFormat
import java.util.Locale

class SushiScan : WPMangaReader("Sushi-Scan", "https://sushiscan.su", "fr", dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.FRENCH)) {
    override val altName = "Nom alternatif : "
    override val seriesStatusSelector = ".tsinfo .imptdt:contains(Statut)"
    override val seriesArtistSelector = ".tsinfo .imptdt:contains(Dessinateur) i"

    override fun parseStatus(status: String) = when {
        status.contains("En Cours") -> SManga.ONGOING
        status.contains("Terminé") -> SManga.COMPLETED
        else -> SManga.UNKNOWN
    }
}
