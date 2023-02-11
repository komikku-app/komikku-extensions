package eu.kanade.tachiyomi.extension.pt.yugenmangas

import eu.kanade.tachiyomi.multisrc.madara.Madara
import eu.kanade.tachiyomi.network.interceptor.rateLimit
import eu.kanade.tachiyomi.source.model.SChapter
import okhttp3.Headers
import okhttp3.OkHttpClient
import org.jsoup.nodes.Element
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class YugenMangas : Madara(
    "YugenMangas",
    "https://yugenmangas.com.br",
    "pt-BR",
    SimpleDateFormat("MMMMM dd, yyyy", Locale("pt", "BR")),
) {

    override val client: OkHttpClient = super.client.newBuilder()
        .addInterceptor(uaIntercept)
        .rateLimit(1, 3, TimeUnit.SECONDS)
        .build()

    override fun headersBuilder(): Headers.Builder = Headers.Builder()
        .add("Origin", baseUrl)
        .add("Referer", "$baseUrl/")

    override val formHeaders: Headers = headersBuilder()
        .add("X-Requested-With", "XMLHttpRequest")
        .set("Referer", "$baseUrl/todas-las-series/")
        .build()

    override val useNewChapterEndpoint: Boolean = true

    override fun chapterFromElement(element: Element): SChapter = SChapter.create().apply {
        name = element.selectFirst("p.chapter-manhwa-title")!!.text()
        date_upload = parseChapterDate(element.selectFirst("span.chapter-release-date i")?.text())

        val chapterUrl = element.selectFirst("a")!!.attr("abs:href")
        setUrlWithoutDomain(
            chapterUrl.substringBefore("?style=paged") +
                if (!chapterUrl.endsWith(chapterUrlSuffix)) chapterUrlSuffix else "",
        )
    }

    override val useRandomUserAgentByDefault: Boolean = true
}
