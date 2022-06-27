package eu.kanade.tachiyomi.extension.en.acescans

import eu.kanade.tachiyomi.multisrc.wpmangareader.WPMangaReader
import eu.kanade.tachiyomi.source.model.Page
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

class AceScans : WPMangaReader("Ace Scans", "https://acescans.xyz", "en") {
    override fun searchMangaFromElement(element: Element) =
        super.searchMangaFromElement(element).apply {
            thumbnail_url = element.select("img").attr("abs:data-src")
        }

    override fun mangaDetailsParse(document: Document) = super.mangaDetailsParse(document).apply {
        thumbnail_url = document.select(seriesThumbnailSelector).attr("abs:data-src")
    }

    override fun pageListParse(document: Document): List<Page> =
        document.select(pageSelector).filterNot {
            it.attr("abs:data-src").isNullOrEmpty()
        }.mapIndexed { i, img -> Page(i, "", img.attr("abs:data-src")) }
}
