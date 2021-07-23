package eu.kanade.tachiyomi.extension.ar.mangaswat

import eu.kanade.tachiyomi.multisrc.wpmangastream.WPMangaStream
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SManga
import org.jsoup.nodes.Document

class MangaSwat : WPMangaStream("MangaSwat", "https://mangaswat.com", "ar") {

    override val projectPageString = "/project-list"

    override val hasProjectPage = true

    override fun mangaDetailsParse(document: Document): SManga {
        return SManga.create().apply {
            document.select("div.bigcontent").firstOrNull()?.let { infoElement ->
                genre = infoElement.select("span:contains(التصنيف) a").joinToString { it.text() }
                status = parseStatus(infoElement.select("span:contains(الحالة)").firstOrNull()?.ownText())
                author = infoElement.select("span:contains(الناشر) i").firstOrNull()?.ownText()
                artist = author
                description = infoElement.select("div.desc").text()
                thumbnail_url = infoElement.select("img").imgAttr()
            }
        }
    }
    override val seriesTypeSelector = "span:contains(النوع) a"

    override val pageSelector = "div#readerarea img"

    override fun pageListParse(document: Document): List<Page> {
        return document.select(pageSelector)
            .filterNot { it.attr("src").isNullOrEmpty() }
            .mapIndexed { i, img -> Page(i, "", img.attr("src")) }
    }
}
