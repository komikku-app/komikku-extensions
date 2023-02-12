package eu.kanade.tachiyomi.multisrc.zeistmanga

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import eu.kanade.tachiyomi.util.asJsoup
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import uy.kohesive.injekt.injectLazy

abstract class ZeistManga(
    override val name: String,
    override val baseUrl: String,
    override val lang: String,
) : ParsedHttpSource() {

    override val supportsLatest = false
    val json: Json by injectLazy()
    open val chapterFeedRegex = """clwd\.run\('([^']+)'""".toRegex()
    open val scriptSelector = "#clwd > script"
    open val imgSelector = "img[src]"
    open val imgSelectorAttr = "src"

    open fun getChaptersUrl(doc: Document): String {
        val script = doc.selectFirst(scriptSelector)!!
        val feed = chapterFeedRegex
            .find(script.html())
            ?.groupValues?.get(1)
            ?: throw Exception("Failed to find chapter feed")

        val url = apiUrl(feed)
            .addQueryParameter("start-index", "2") // Only get chapters
            .addQueryParameter("max-results", "999999") // Get all chapters
            .build()

        return url.toString()
    }

    override fun chapterListParse(response: Response): List<SChapter> {
        val document = response.asJsoup()

        val url = getChaptersUrl(document)

        // Call JSON API
        val req = GET(url, headers)
        val res = client.newCall(req).execute()

        // Parse JSON API response
        val jsonString = res.body.string()
        val result = json.decodeFromString<ZeistMangaDto>(jsonString)

        // Transform JSON response into List<SChapter>
        return result.feed?.entry?.map { it.toSChapter(baseUrl) }
            ?: throw Exception("Failed to parse from chapter API")
    }

    override fun imageUrlParse(document: Document): String {
        throw UnsupportedOperationException("Not used.")
    }

    override fun latestUpdatesParse(response: Response): MangasPage {
        throw UnsupportedOperationException("Not used.")
    }

    override fun latestUpdatesRequest(page: Int): Request {
        throw UnsupportedOperationException("Not used.")
    }

    override fun chapterFromElement(element: Element): SChapter {
        throw UnsupportedOperationException("Not used.")
    }

    override fun chapterListSelector(): String {
        throw UnsupportedOperationException("Not used.")
    }

    override fun latestUpdatesFromElement(element: Element): SManga {
        throw UnsupportedOperationException("Not used.")
    }

    override fun latestUpdatesNextPageSelector(): String? {
        throw UnsupportedOperationException("Not used.")
    }

    override fun latestUpdatesSelector(): String {
        throw UnsupportedOperationException("Not used.")
    }

    override fun popularMangaFromElement(element: Element): SManga {
        throw UnsupportedOperationException("Not used.")
    }

    override fun popularMangaNextPageSelector(): String? {
        throw UnsupportedOperationException("Not used.")
    }

    override fun popularMangaSelector(): String {
        throw UnsupportedOperationException("Not used.")
    }

    override fun mangaDetailsParse(document: Document): SManga {
        val profileManga = document.selectFirst(".grid.gtc-235fr")!!
        return SManga.create().apply {
            title = profileManga.selectFirst("h1.mt-0.mb-6.fs-20")!!.text()
            thumbnail_url = profileManga.selectFirst("img")!!.attr("src")
            description = profileManga.select("#synopsis").text()
            status = SManga.UNKNOWN
        }
    }

    override fun pageListParse(document: Document): List<Page> {
        val images = document.selectFirst("div.check-box")!!
        return images.select(imgSelector).mapIndexed { i, img ->
            Page(i, "", img.attr(imgSelectorAttr))
        }
    }

    override fun popularMangaParse(response: Response): MangasPage {
        val jsonString = response.body.string()
        val result = json.decodeFromString<ZeistMangaDto>(jsonString)
        // Transform JSON response into List<SManga>
        val mangas = result.feed!!.entry?.map { it.toSManga(baseUrl) }
        val mangalist = mangas!!.toMutableList()
        if (mangas.size == maxResults + 1) {
            mangalist.removeLast()
            return MangasPage(mangalist, true)
        }
        return MangasPage(mangalist, false)
    }

    override fun popularMangaRequest(page: Int): Request {
        val startIndex = maxResults * (page - 1) + 1
        val url = apiUrl()
            .addQueryParameter("orderby", "published")
            .addQueryParameter("max-results", (maxResults + 1).toString())
            .addQueryParameter("start-index", startIndex.toString())
            .build()

        return GET(url.toString(), headers)
    }

    override fun searchMangaSelector(): String = ".grid.gtc-f141a > div"
    override fun searchMangaFromElement(element: Element): SManga {
        return SManga.create().apply {
            setUrlWithoutDomain(element.select(".block").attr("href"))
            title = element.selectFirst(".clamp.toe.oh.block")!!.text().trim()
            thumbnail_url = element.selectFirst("img")!!.attr("src")
        }
    }

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        val url = "$baseUrl/search".toHttpUrl().newBuilder()
            .addQueryParameter("q", query)
            .build()

        return GET(url.toString(), headers)
    }

    override fun searchMangaNextPageSelector(): String? = null

    open fun apiUrl(feed: String = "Series"): HttpUrl.Builder {
        return "$baseUrl/feeds/posts/default/-/".toHttpUrl().newBuilder()
            .addPathSegment(feed)
            .addQueryParameter("alt", "json")
    }

    companion object {
        private const val maxResults = 20
    }
}
