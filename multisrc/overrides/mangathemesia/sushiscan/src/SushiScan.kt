package eu.kanade.tachiyomi.extension.fr.sushiscan

import eu.kanade.tachiyomi.multisrc.mangathemesia.MangaThemesia
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SManga
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import org.jsoup.nodes.Document
import java.text.SimpleDateFormat
import java.util.Locale

class SushiScan : MangaThemesia("Sushi-Scan", "https://sushiscan.net", "fr", mangaUrlDirectory = "/catalogue", dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.FRENCH)) {
    override val altNamePrefix = "Nom alternatif : "
    override val seriesAuthorSelector = ".imptdt:contains(Auteur) i, .fmed b:contains(Auteur)+span"
    override val seriesStatusSelector = ".imptdt:contains(Statut) i"
    override fun String?.parseStatus(): Int = when {
        this == null -> SManga.UNKNOWN
        this.contains("En Cours", ignoreCase = true) -> SManga.ONGOING
        this.contains("Terminé", ignoreCase = true) -> SManga.COMPLETED
        else -> SManga.UNKNOWN
    }

    override fun mangaDetailsParse(document: Document): SManga =
        super.mangaDetailsParse(document).apply {
            status = document.select(seriesStatusSelector).text().parseStatus()
        }

    override fun pageListParse(document: Document): List<Page> {
        val scriptContent = document.selectFirst("script:containsData(ts_reader)")?.data()
            ?: return super.pageListParse(document)
        val jsonString = scriptContent.substringAfter("ts_reader.run(").substringBefore(");")
        val tsReader = json.decodeFromString<TSReader>(jsonString)
        val imageUrls = tsReader.sources.firstOrNull()?.images ?: return emptyList()
        return imageUrls.mapIndexed { index, imageUrl -> Page(index, document.location(), imageUrl) }
    }

    @Serializable
    data class TSReader(
        val sources: List<ReaderImageSource>,
    )

    @Serializable
    data class ReaderImageSource(
        val source: String,
        val images: List<String>,
    )
}
