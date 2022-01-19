package eu.kanade.tachiyomi.extension.all.yugenmangas

import eu.kanade.tachiyomi.lib.ratelimit.RateLimitInterceptor
import eu.kanade.tachiyomi.multisrc.madara.Madara
import eu.kanade.tachiyomi.source.SourceFactory
import eu.kanade.tachiyomi.source.model.SChapter
import okhttp3.OkHttpClient
import org.jsoup.nodes.Element
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class YugenMangasFactory : SourceFactory {
    override fun createSources() = listOf(
        YugenMangasEs(),
        YugenMangasBr()
    )
}

abstract class YugenMangas(
    override val baseUrl: String,
    override val lang: String,
    dateFormat: SimpleDateFormat = SimpleDateFormat("MMMMM dd, yyyy", Locale.US)
) : Madara("YugenMangas", baseUrl, lang, dateFormat) {

    override fun popularMangaSelector() = "div.page-item-detail.manga"

    override fun chapterFromElement(element: Element): SChapter = SChapter.create().apply {
        name = element.selectFirst("a")!!.ownText()
        date_upload = parseChapterDate(element.selectFirst("span.chapter-release-date i")?.text())

        val chapterUrl = element.selectFirst("a")!!.attr("abs:href")
        setUrlWithoutDomain(
            chapterUrl.substringBefore("?style=paged") +
                if (!chapterUrl.endsWith(chapterUrlSuffix)) chapterUrlSuffix else ""
        )
    }
}

class YugenMangasEs : YugenMangas("https://yugenmangas.com", "es")

class YugenMangasBr : YugenMangas(
    "https://yugenmangas.com.br",
    "pt-BR",
    SimpleDateFormat("MMMMM dd, yyyy", Locale("pt", "BR"))
) {

    override val client: OkHttpClient = super.client.newBuilder()
        .addInterceptor(RateLimitInterceptor(1, 2, TimeUnit.SECONDS))
        .build()

    override val altName: String = "Nome alternativo: "

    override val useNewChapterEndpoint: Boolean = true
}
