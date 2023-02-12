package eu.kanade.tachiyomi.extension.en.ksgroupscans

import eu.kanade.tachiyomi.multisrc.fmreader.FMReader
import eu.kanade.tachiyomi.source.model.SChapter
import org.jsoup.nodes.Element

class KSGroupScans : FMReader("KSGroupScans", "https://ksgroupscans.com", "en") {
    override fun popularMangaNextPageSelector() = ".pagination > li:last-child > a:not(.active)"

    override fun chapterFromElement(element: Element, mangaTitle: String): SChapter {
        return SChapter.create().apply {
            element.select(chapterUrlSelector).first()!!.let {
                setUrlWithoutDomain(it.attr("abs:href"))
                name = element.select(".chapter-name").text()
            }
        }
    }
}
