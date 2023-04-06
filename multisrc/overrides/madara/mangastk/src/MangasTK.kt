package eu.kanade.tachiyomi.extension.es.mangastk

import eu.kanade.tachiyomi.multisrc.madara.Madara
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import org.jsoup.nodes.Element
import java.text.SimpleDateFormat
import java.util.Locale

class MangasTK : Madara(
    "MangasTK",
    "https://mangastk.net",
    "es",
    SimpleDateFormat("dd.MM.yyyy", Locale("es")),
) {
    override fun popularMangaSelector() = "div#series-card:not(:has(a[href*='bilibilicomics.com']))"
    override val popularMangaUrlSelector = "a.series-link"

    override val mangaDetailsSelectorTag = "div.tags-content a.notUsed" // Source use this for the scanlator
    override val mangaDetailsSelectorStatus = "div.post-status div.summary-content"

    override fun popularMangaFromElement(element: Element): SManga {
        val manga = SManga.create()

        with(element) {
            select(popularMangaUrlSelector).first()?.let {
                manga.setUrlWithoutDomain(it.attr("abs:href"))
                manga.title = it.attr("title")
            }

            select("img").first()?.let {
                manga.thumbnail_url = imageFromElement(it)
            }
        }

        return manga
    }

    override fun chapterFromElement(element: Element): SChapter {
        val chapter = SChapter.create()

        with(element) {
            select(chapterUrlSelector).first()?.let { urlElement ->
                chapter.url = urlElement.attr("abs:href").let {
                    it.substringBefore("?style=paged") + if (!it.endsWith(chapterUrlSuffix)) chapterUrlSuffix else ""
                }
                chapter.name = urlElement.select("p.chapter-manhwa-title").text()
                chapter.date_upload = parseChapterDate(select("span.chapter-release-date").text())
            }
        }

        return chapter
    }
}
