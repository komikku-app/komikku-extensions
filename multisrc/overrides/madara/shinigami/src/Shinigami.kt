package eu.kanade.tachiyomi.extension.id.shinigami

import eu.kanade.tachiyomi.multisrc.madara.Madara
import eu.kanade.tachiyomi.network.interceptor.rateLimit
import eu.kanade.tachiyomi.source.model.SChapter
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import org.jsoup.nodes.Element
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class Shinigami : Madara("Shinigami", "https://shinigami.sh", "id") {
    // moved from Reaper Scans (id) to Shinigami (id)
    override val id = 3411809758861089969

    override val client: OkHttpClient = super.client.newBuilder()
        .rateLimit(4, 1, TimeUnit.SECONDS)
        .build()

    override fun headersBuilder(): Headers.Builder = super.headersBuilder()
        .add("Referer", "$baseUrl/")
        .add("Sec-Fetch-Dest", "document")
        .add("Sec-Fetch-Mode", "navigate")
        .add("Sec-Fetch-Site", "same-origin")
        .add("Upgrade-Insecure-Requests", "1")
        .add("User-Agent", "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/$randomVersionNumber.0.0.0 Mobile Safari/537.3")
        .add("X-Requested-With", randomString)

    private val randomVersionNumber = Random.Default.nextInt(112, 118)

    private fun generateRandomString(length: Int): String {
        val charset = "ABCDEFGHIJKLMNOPQRSTUVWXYZ.abcdefghijklmnopqrstuvwxyz.0123456789"
        return (1..length)
            .map { charset.random() }
            .joinToString("")
    }

    private val randomLength = Random.Default.nextInt(13, 21)

    private val randomString = generateRandomString(randomLength)

    override val mangaSubString = "semua-series"

    // Tags are useless as they are just SEO keywords.
    override val mangaDetailsSelectorTag = ""

    override fun chapterFromElement(element: Element): SChapter = SChapter.create().apply {
        val urlElement = element.selectFirst(chapterUrlSelector)!!

        name = urlElement.selectFirst("p.chapter-manhwa-title")?.text()
            ?: urlElement.ownText()
        date_upload = urlElement.selectFirst("span.chapter-release-date > i")?.text()
            .let { parseChapterDate(it) }

        val fixedUrl = urlElement.attr("abs:href").toHttpUrl().newBuilder()
            .removeAllQueryParameters("style")
            .addQueryParameter("style", "list")
            .toString()

        setUrlWithoutDomain(fixedUrl)
    }
}
