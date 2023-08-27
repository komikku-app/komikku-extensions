package eu.kanade.tachiyomi.extension.es.templescanesp

import eu.kanade.tachiyomi.multisrc.madara.Madara
import eu.kanade.tachiyomi.source.model.SChapter
import org.jsoup.nodes.Element
import java.text.SimpleDateFormat
import java.util.Locale

class TempleScanEsp : Madara(
    "TempleScan",
    "https://templescanesp.com",
    "es",
    SimpleDateFormat("dd.MM.yyyy", Locale("es")),
) {
    override val mangaSubString = "series"

    override fun popularMangaSelector() = "div.tab-content-wrap div.loopcont > div"
    override val popularMangaUrlSelector = "div#series-card a.series-link"

    override val mangaDetailsSelectorAuthor = "div.post-content_item:contains(Autor) div.summary-content"
    override val mangaDetailsSelectorArtist = "div.post-content_item:contains(Artista) div.summary-content"
    override val mangaDetailsSelectorStatus = "div.post-content_item:contains(Estado) div.summary-content"

    override fun chapterFromElement(element: Element): SChapter {
        val chapter = SChapter.create()

        with(element) {
            select(chapterUrlSelector).first()?.let { urlElement ->
                chapter.url = urlElement.attr("abs:href").let {
                    it.substringBefore("?style=paged") + if (!it.endsWith(chapterUrlSuffix)) chapterUrlSuffix else ""
                }
                chapter.name = urlElement.select("p").text()
            }

            chapter.date_upload = select("img:not(.thumb)").firstOrNull()?.attr("alt")?.let { parseRelativeDate(it) }
                ?: select("span a").firstOrNull()?.attr("title")?.let { parseRelativeDate(it) }
                ?: parseChapterDate(select(chapterDateSelector()).firstOrNull()?.text())
        }

        return chapter
    }
}
