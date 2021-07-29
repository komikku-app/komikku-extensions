package eu.kanade.tachiyomi.extension.it.lupiteam

import eu.kanade.tachiyomi.multisrc.foolslide.FoolSlide
import eu.kanade.tachiyomi.source.model.SManga
import org.jsoup.nodes.Document

class LupiTeam : FoolSlide("LupiTeam", "https://lupiteam.net", "it", "/reader") {
    override fun mangaDetailsParse(document: Document) = SManga.create().apply {
        val infoElement = document.select(mangaDetailsInfoSelector).first().text()
        author = infoElement.substringAfter("Autore: ").substringBefore("Artista: ")
        artist = infoElement.substringAfter("Artista: ").substringBefore("Target: ")
        status = when (infoElement.substringAfter("Stato: ").substringBefore("Trama: ").take(8)) {
            "In corso" -> SManga.ONGOING
            "Completa" -> SManga.COMPLETED
            "Licenzia" -> SManga.LICENSED
            else -> SManga.UNKNOWN
        }
        description = infoElement.substringAfter("Trama: ")
        thumbnail_url = getDetailsThumbnail(document)
    }
}
