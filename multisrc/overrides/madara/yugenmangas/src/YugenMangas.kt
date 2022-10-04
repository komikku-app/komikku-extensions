package eu.kanade.tachiyomi.extension.pt.yugenmangas

import eu.kanade.tachiyomi.multisrc.madara.Madara
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.interceptor.rateLimit
import eu.kanade.tachiyomi.source.model.SChapter
import kotlinx.serialization.decodeFromString
import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import org.jsoup.nodes.Element
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class YugenMangas : Madara(
    "YugenMangas",
    "https://yugenmangas.com.br",
    "pt-BR",
    SimpleDateFormat("MMMMM dd, yyyy", Locale("pt", "BR"))
) {

    override val client: OkHttpClient = super.client.newBuilder()
        .addInterceptor(::uaIntercept)
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
                if (!chapterUrl.endsWith(chapterUrlSuffix)) chapterUrlSuffix else ""
        )
    }

    private var userAgent: String? = null
    private var checkedUa = false

    private fun uaIntercept(chain: Interceptor.Chain): Response {
        if (userAgent == null && !checkedUa) {
            val uaResponse = chain.proceed(GET(UA_DB_URL))

            if (uaResponse.isSuccessful) {
                userAgent = json.decodeFromString<List<String>>(uaResponse.body!!.string()).random()
                checkedUa = true
            }

            uaResponse.close()
        }

        if (userAgent != null) {
            val newRequest = chain.request().newBuilder()
                .header("User-Agent", userAgent!!)
                .build()

            return chain.proceed(newRequest)
        }

        return chain.proceed(chain.request())
    }

    companion object {
        private const val UA_DB_URL = "https://tachiyomiorg.github.io/user-agents/user-agents.json"
    }
}
