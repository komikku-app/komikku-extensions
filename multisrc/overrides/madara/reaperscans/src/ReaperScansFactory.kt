package eu.kanade.tachiyomi.extension.all.reaperscans

import eu.kanade.tachiyomi.lib.ratelimit.RateLimitInterceptor
import eu.kanade.tachiyomi.multisrc.madara.Madara
import eu.kanade.tachiyomi.source.SourceFactory
import eu.kanade.tachiyomi.source.model.SChapter
import okhttp3.OkHttpClient
import org.jsoup.nodes.Element
import java.util.concurrent.TimeUnit

class ReaperScansFactory : SourceFactory {
    override fun createSources() = listOf(
        ReaperScansEn(),
        ReaperScansBr()
    )
}

abstract class ReaperScans(
    override val baseUrl: String,
    override val lang: String
) : Madara("Reaper Scans", baseUrl, lang) {

    override fun popularMangaSelector() = "div.page-item-detail.manga"

    override fun chapterFromElement(element: Element): SChapter {
        val chapter = SChapter.create()

        with(element) {
            select(chapterUrlSelector).first()?.let { urlElement ->
                chapter.url = urlElement.attr("abs:href").let {
                    it.substringBefore("?style=paged") + if (!it.endsWith(chapterUrlSuffix)) chapterUrlSuffix else ""
                }
                chapter.name = urlElement.ownText()
            }
            // Dates can be part of a "new" graphic or plain text
            // Added "title" alternative
            chapter.date_upload = select("img").firstOrNull()?.attr("alt")?.let { parseRelativeDate(it) }
                ?: select("span a").firstOrNull()?.attr("title")?.let { parseRelativeDate(it) }
                    ?: parseChapterDate(select("span.chapter-release-date i").firstOrNull()?.text())
        }

        return chapter
    }
}

class ReaperScansEn : ReaperScans("https://reaperscans.com", "en") {
    override val versionId = 2
}

class ReaperScansBr : ReaperScans("https://reaperscans.com.br", "pt-BR") {
    override val id = 7767018058145795388

    override val client: OkHttpClient = super.client.newBuilder()
        .addInterceptor(RateLimitInterceptor(1, 2, TimeUnit.SECONDS))
        .build()

    override val altName: String = "Nome alternativo: "
}
