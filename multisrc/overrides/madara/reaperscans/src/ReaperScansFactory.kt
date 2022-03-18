package eu.kanade.tachiyomi.extension.all.reaperscans

import eu.kanade.tachiyomi.lib.ratelimit.RateLimitInterceptor
import eu.kanade.tachiyomi.multisrc.madara.Madara
import eu.kanade.tachiyomi.source.SourceFactory
import eu.kanade.tachiyomi.source.model.SChapter
import okhttp3.OkHttpClient
import org.jsoup.nodes.Element
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class ReaperScansFactory : SourceFactory {
    override fun createSources() = listOf(
        ReaperScansEn(),
        ReaperScansBr()
    )
}

abstract class ReaperScans(
    override val baseUrl: String,
    lang: String,
    dateFormat: SimpleDateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.US)
) : Madara("Reaper Scans", baseUrl, lang, dateFormat)

class ReaperScansEn : ReaperScans("https://reaperscans.com", "en", SimpleDateFormat("MMM dd,yyyy", Locale.US)) {
    override val versionId = 2

    override fun chapterFromElement(element: Element): SChapter {
        val chapter = SChapter.create()
        with(element) {
            select(chapterUrlSelector).first()?.let { urlElement ->
                chapter.url = urlElement.attr("abs:href").let {
                    it.substringBefore("?style=paged") + if (!it.endsWith(chapterUrlSuffix)) chapterUrlSuffix else ""
                }
                chapter.name = urlElement.select("p.chapter-manhwa-title").firstOrNull()?.text().toString()
            }
            chapter.date_upload = select("span.chapter-release-date > i").firstOrNull()?.text().let { parseChapterDate(it) }
        }
        return chapter
    }
}

class ReaperScansBr : ReaperScans("https://reaperscans.com.br", "pt-BR", SimpleDateFormat("dd/MM/yyyy", Locale.US)) {
    override val id = 7767018058145795388

    override val client: OkHttpClient = super.client.newBuilder()
        .addInterceptor(RateLimitInterceptor(1, 2, TimeUnit.SECONDS))
        .build()

    override fun chapterFromElement(element: Element): SChapter {
        val chapter = SChapter.create()
        with(element) {
            select(chapterUrlSelector).first()?.let { urlElement ->
                chapter.url = urlElement.attr("abs:href").let {
                    it.substringBefore("?style=paged") + if (!it.endsWith(chapterUrlSuffix)) chapterUrlSuffix else ""
                }
                chapter.name = urlElement.ownText()
            }
            chapter.date_upload = select("span.chapter-release-date > i").firstOrNull()?.text().let { parseChapterDate(it) }
        }
        return chapter
    }
}
