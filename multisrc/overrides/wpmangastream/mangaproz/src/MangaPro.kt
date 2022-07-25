package eu.kanade.tachiyomi.extension.ar.mangaproz

import eu.kanade.tachiyomi.multisrc.wpmangastream.WPMangaStream
import eu.kanade.tachiyomi.network.interceptor.rateLimit
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import okhttp3.OkHttpClient
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.util.concurrent.TimeUnit

class MangaPro : WPMangaStream("Manga Pro", "https://mangaprotm.com", "ar") {
    override val id: Long = 964048798769065340

    override val client: OkHttpClient = network.cloudflareClient.newBuilder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .rateLimit(4)
        .build()

    override fun chapterListParse(response: Response): List<SChapter> =
        super.chapterListParse(response).filter { !it.url.isNullOrEmpty() }

    override fun chapterFromElement(element: Element): SChapter {
        val urlElement = element.select(".lchx > a, span.leftoff a, div.eph-num > a").first()
        val chapter = SChapter.create()

        // ignore chapters that are protected behind ads shortners.
        if (urlElement.attr("href").startsWith(baseUrl))
            chapter.setUrlWithoutDomain(urlElement.attr("href"))
        else
            chapter.setUrlWithoutDomain("")

        chapter.name = if (urlElement.select("span.chapternum")
            .isNotEmpty()
        ) urlElement.select("span.chapternum").text() else urlElement.text()
        chapter.name = chapter.name.replace("-*free".toRegex(RegexOption.IGNORE_CASE), "")
        chapter.date_upload =
            element.select("span.rightoff, time, span.chapterdate").firstOrNull()?.text()
                ?.let { parseChapterDate(it) }
                ?: 0
        return chapter
    }

    override fun mangaDetailsParse(document: Document): SManga =
        super.mangaDetailsParse(document).apply {
            document.select("div.bigcontent, div.animefull, div.main-info").firstOrNull()
                ?.let { infoElement ->
                    thumbnail_url =
                        infoElement.select(mangaDetailsSelectorThumbnail).attr("abs:data-lazy-src")
                }
        }
}
