package eu.kanade.tachiyomi.extension.en.manhwanelo

import eu.kanade.tachiyomi.multisrc.madara.Madara
import eu.kanade.tachiyomi.source.model.SManga
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.util.Locale

class ManhwaNelo : Madara("ManhwaNelo", "https://manhwanelo.com", "en") {

    override fun popularMangaFromElement(element: Element): SManga {
        val manga = SManga.create()

        with(element) {
            select("div.item-thumb a").first()?.let {
                manga.setUrlWithoutDomain(it.attr("abs:href"))
                manga.title = it.attr("title")
            }

            select("img").first()?.let {
                manga.thumbnail_url = imageFromElement(it)
            }
        }

        return manga
    }

    override fun mangaDetailsParse(document: Document): SManga {
        val manga = SManga.create()
        with(document) {
            select("div.detailsingle h1").first()?.let {
                manga.title = it.ownText()
            }
            select("div.author a").eachText().filter {
                it.notUpdating()
            }.joinToString().takeIf { it.isNotBlank() }?.let {
                manga.author = it
                // Site does not separate artist and author
                manga.artist = manga.author
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
            select("div.status p").let {
                manga.status = when (it.text().substringAfter("| ")) {
                    "end" -> SManga.COMPLETED
                    "on-going" -> SManga.ONGOING
                    else -> SManga.UNKNOWN
                }
            }
            manga.genre = select("div.genres a")
                .joinToString(", ") { element -> element.text().toLowerCase(Locale.ROOT) }

            return manga
        }
    }
}
