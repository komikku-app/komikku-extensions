package eu.kanade.tachiyomi.extension.id.komiklab

import eu.kanade.tachiyomi.multisrc.wpmangareader.WPMangaReader
import eu.kanade.tachiyomi.source.model.SManga
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

class KomikLab : WPMangaReader("Komik Lab", "https://komiklab.com", "id") {
    override val hasProjectPage = true

    override fun mangaDetailsParse(document: Document) = SManga.create().apply {
        author = document.select(".listinfo li:contains(Author), .tsinfo .imptdt:nth-child(4) i, .infotable tr:contains(author) td:last-child")
            .firstOrNull()?.ownText()

        artist = document.select(".infotable tr:contains(artist) td:last-child, .tsinfo .imptdt:contains(artist) i")
            .firstOrNull()?.ownText()

        genre = document.select("div.gnr a, .mgen a, .seriestugenre a").joinToString { it.text() }
        status = parseStatus(
            document.select("div.listinfo li:contains(Status), .tsinfo .imptdt:contains(status), .tsinfo .imptdt:contains(الحالة), .infotable tr:contains(status) td")
                .text()
        )

        title = document.selectFirst("h1.entry-title").text()
        thumbnail_url = document.select(".infomanga > div[itemprop=image] img, .thumb img").attr("abs:data-src")
        description = document.select(".desc, .entry-content[itemprop=description]").joinToString("\n") { it.text() }

        // add series type(manga/manhwa/manhua/other) thinggy to genre
        document.select(seriesTypeSelector).firstOrNull()?.ownText()?.let {
            if (it.isEmpty().not() && genre!!.contains(it, true).not()) {
                genre += if (genre!!.isEmpty()) it else ", $it"
            }
        }

        // add alternative name to manga description
        document.select(altNameSelector).firstOrNull()?.ownText()?.let {
            if (it.isEmpty().not()) {
                description += when {
                    description!!.isEmpty() -> altName + it
                    else -> "\n\n$altName" + it
                }
            }
        }
    }

    override fun searchMangaFromElement(element: Element) = SManga.create().apply {
        thumbnail_url = element.select("img").attr("abs:data-src")
        title = element.select("a").attr("title")
        setUrlWithoutDomain(element.select("a").attr("href"))
    }
}
