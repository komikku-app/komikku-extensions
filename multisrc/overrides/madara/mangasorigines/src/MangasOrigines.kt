package eu.kanade.tachiyomi.extension.fr.mangasorigines

import eu.kanade.tachiyomi.multisrc.madara.Madara
import eu.kanade.tachiyomi.source.model.SManga
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.text.SimpleDateFormat
import java.util.Locale

class MangasOrigines : Madara("Mangas Origines", "https://mangas-origines.fr", "fr", SimpleDateFormat("dd MMM yyyy", Locale("fr"))) {
    override val useNewChapterEndpoint = true

    private fun String.removeFireEmoji() = this.substringAfter("\uD83D\uDD25 ")

    override fun popularMangaFromElement(element: Element): SManga {
        return super.popularMangaFromElement(element).apply {
            title = title.removeFireEmoji()
        }
    }

    override fun mangaDetailsParse(document: Document): SManga {
        val manga = SManga.create()
        with(document) {
            select("div.post-title h1").first()?.let {
                manga.title = it.ownText().removeFireEmoji()
            }
            select("div.author-content > a").eachText().filter {
                it.notUpdating()
            }.joinToString().takeIf { it.isNotBlank() }?.let {
                manga.author = it
            }
            select("div.artist-content > a").eachText().filter {
                it.notUpdating()
            }.joinToString().takeIf { it.isNotBlank() }?.let {
                manga.artist = it
            }
            select("div.description-summary div.summary__content").let {
                if (it.select("p").text().isNotEmpty()) {
                    manga.description = it.select("p").joinToString(separator = "\n\n") { p ->
                        p.text().replace("<br>", "\n")
                    }
                } else {
                    manga.description = it.text()
                }
            }
            select("div.summary_image img").first()?.let {
                manga.thumbnail_url = imageFromElement(it)
            }
            select("div.summary-content").last()?.let {
                manga.status = when (it.text()) {
                    // I don't know what's the corresponding for COMPLETED and LICENSED
                    // There's no support for "Canceled" or "On Hold"
                    "Terminé ⚫" -> SManga.COMPLETED
                    "En cours \uD83D\uDFE2" -> SManga.ONGOING
                    else -> SManga.UNKNOWN
                }
            }
            val genres = select("div.genres-content a")
                    .map { element -> element.text().toLowerCase(Locale.ROOT) }
                    .toMutableSet()

            // add tag(s) to genre
            select("div.tags-content a").forEach { element ->
                if (genres.contains(element.text()).not()) {
                    genres.add(element.text().toLowerCase(Locale.ROOT))
                }
            }

            // add manga/manhwa/manhua thinggy to genre
            document.select(seriesTypeSelector).firstOrNull()?.ownText()?.let {
                if (it.isEmpty().not() && it.notUpdating() && it != "-" && genres.contains(it).not()) {
                    genres.add(it.toLowerCase(Locale.ROOT))
                }
            }

            manga.genre = genres.toList().joinToString(", ") { it.capitalize(Locale.ROOT) }

            // add alternative name to manga description
            document.select(altNameSelector).firstOrNull()?.ownText()?.let {
                if (it.isBlank().not() && it.notUpdating()) {
                    manga.description = when {
                        manga.description.isNullOrBlank() -> altName + it
                        else -> manga.description + "\n\n$altName" + it
                    }
                }
            }
        }

        return manga
    }
}