package eu.kanade.tachiyomi.extension.pt.yugenmangas

import androidx.preference.PreferenceScreen
import eu.kanade.tachiyomi.lib.randomua.UserAgentType
import eu.kanade.tachiyomi.lib.randomua.setRandomUserAgent
import eu.kanade.tachiyomi.multisrc.madara.Madara
import eu.kanade.tachiyomi.network.interceptor.rateLimit
import eu.kanade.tachiyomi.source.model.SChapter
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

    override val client: OkHttpClient = network.cloudflareClient.newBuilder()
        .setRandomUserAgent(
            UserAgentType.DESKTOP,
        )
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .rateLimit(1, 3, TimeUnit.SECONDS)
        .build()

    override fun headersBuilder() = super.headersBuilder()
        .add("Origin", baseUrl)

    override val useNewChapterEndpoint: Boolean = true

    override val mangaSubString = "series"

    override fun chapterFromElement(element: Element): SChapter = SChapter.create().apply {
        name = element.selectFirst("p.chapter-manhwa-title")!!.text()
        date_upload = parseChapterDate(element.selectFirst("span.chapter-release-date i")?.text())

        val chapterUrl = element.selectFirst("a")!!.attr("abs:href")
        setUrlWithoutDomain(
            chapterUrl.substringBefore("?style=paged") +
                if (!chapterUrl.endsWith(chapterUrlSuffix)) chapterUrlSuffix else "",
        )
    }

    override fun setupPreferenceScreen(screen: PreferenceScreen) { }
}
