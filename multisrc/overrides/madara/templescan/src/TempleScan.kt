package eu.kanade.tachiyomi.extension.en.templescan

import eu.kanade.tachiyomi.multisrc.madara.Madara
import eu.kanade.tachiyomi.network.interceptor.rateLimit
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import okhttp3.OkHttpClient
import org.jsoup.nodes.Element
import java.text.SimpleDateFormat
import java.util.Locale

class TempleScan : Madara(
    "Temple Scan",
    "https://templescan.net",
    "en",
    SimpleDateFormat("dd.MM.yyyy", Locale.ENGLISH),
) {
    override val mangaSubString = "comic"
    override val useNewChapterEndpoint = true
    override fun popularMangaSelector() = "div.c-tabs-item > div > div"
    override val popularMangaUrlSelector = "div.series-box a"
    override val mangaDetailsSelectorStatus = ".post-content_item:contains(Status) .summary-content"

    override val client: OkHttpClient = super.client.newBuilder()
        .rateLimit(1)
        .build()

    override fun popularMangaFromElement(element: Element): SManga {
        return super.popularMangaFromElement(element).apply {
            title = element.select(popularMangaUrlSelector).text()
        }
    }

    override fun searchPage(page: Int): String {
        return if (page > 1) {
            "page/$page/"
        } else {
            ""
        }
    }

    override fun chapterFromElement(element: Element): SChapter {
        return super.chapterFromElement(element).apply {
            name = element.select(".chapter-manhwa-title").text()
        }
    }
}
