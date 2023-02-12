package eu.kanade.tachiyomi.extension.ar.asgardteam

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.util.concurrent.TimeUnit

class AsgardTeam : ParsedHttpSource() {

    override val name = "AsgardTeam"

    override val baseUrl = "https://asgard1team.com"

    override val lang = "ar"

    override val supportsLatest = true

    override val client: OkHttpClient = network.cloudflareClient.newBuilder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    override fun headersBuilder(): Headers.Builder = super.headersBuilder()
        .add("Referer", baseUrl)

    // Popular

    override fun popularMangaSelector() = "div.manga-card"

    override fun popularMangaRequest(page: Int): Request {
        return GET("$baseUrl/manga-list/?page=$page", headers)
    }

    override fun popularMangaFromElement(element: Element): SManga = SManga.create().apply {
        element.select("div.manga-details__container").let {
            thumbnail_url = element.select("img").attr("abs:src")
            // title = it.text()
        }
        element.select("div.manga-details__container").let {
            title = element.select("img").attr("alt")
        }
        element.select("div a.manga-card__title").let {
            setUrlWithoutDomain(it.attr("abs:href"))
            // title = it.text()
        }
    }

    override fun popularMangaNextPageSelector() = "ul.pagination a.page-link"

    // Latest

    override fun latestUpdatesRequest(page: Int): Request {
        return GET(baseUrl)
    }

    override fun latestUpdatesSelector() = popularMangaSelector()

    override fun latestUpdatesFromElement(element: Element): SManga = popularMangaFromElement(element)

    override fun latestUpdatesNextPageSelector(): String? = null

    // Search

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        return if (query.isNotBlank()) {
            GET("$baseUrl/search/?s=$query&page=$page", headers)
        } else {
            val url = "$baseUrl/manga-list/?page=$page".toHttpUrlOrNull()!!.newBuilder()
            filters.forEach { filter ->
                when (filter) {
                    is TypeFilter -> url.addQueryParameter("type", filter.toUriPart())
                    else -> {}
                }
            }
            GET(url.build().toString(), headers)
        }
    }

    override fun searchMangaSelector() = popularMangaSelector()

    override fun searchMangaFromElement(element: Element): SManga = popularMangaFromElement(element)

    override fun searchMangaNextPageSelector() = popularMangaNextPageSelector()

    // Details

    override fun mangaDetailsParse(document: Document): SManga = SManga.create().apply {
        return SManga.create().apply {
            document.select("div.author-info-title").first()!!.let { info ->
                title = info.select("h6").text()
            }
            document.select("div.review-author-info").let { info ->
                genre = info.select("a").joinToString { it.text() }
            }
            document.select("div.full-list-info:contains(المؤلف)").let { info ->
                author = info.select("small").joinToString { it.text() }
            }
            document.select("div.full-list-info:contains(الرسام)").let { info ->
                artist = info.select("small").joinToString { it.text() }
            }
            document.select("div.review-content").let { info ->
                description = info.select("p").text()
            }
        }
    }

    // Chapters

    override fun chapterListSelector() = "tbody > tr > td"

    override fun chapterFromElement(element: Element): SChapter {
        val chapter = SChapter.create()
        element.select("a").let {
            chapter.setUrlWithoutDomain(it.attr("abs:href"))
            chapter.name = it.text()
        }
        chapter.date_upload = 0
        return chapter
    }

    // Pages

    override fun pageListParse(document: Document): List<Page> {
        return document.select("section div.container div.container img").mapIndexed { i, img ->
            Page(i, "", img.attr("abs:src"))
        }
    }

    override fun imageRequest(page: Page): Request {
        return GET(page.imageUrl!!, headersBuilder().set("Referer", page.url).build())
    }

    override fun imageUrlParse(document: Document): String = throw UnsupportedOperationException("Not used")

    // Filters (TODO: Add Genre Filters Later)

    override fun getFilterList() = FilterList(
        Filter.Header("NOTE: Ignored if using text search!"),
        TypeFilter(getTypeFilter()),
    )

    private class TypeFilter(vals: Array<Pair<String?, String>>) : UriPartFilter("Type", vals)

    private fun getTypeFilter(): Array<Pair<String?, String>> = arrayOf(
        Pair("", "<select>"),
        Pair("3", "صينية (مانها)"),
        Pair("2", "مانجا (يابانية)"),
        Pair("1", "كورية (مانهوا)"),
    )

    open class UriPartFilter(displayName: String, private val vals: Array<Pair<String?, String>>) :
        Filter.Select<String>(displayName, vals.map { it.second }.toTypedArray()) {
        fun toUriPart() = vals[state].first
    }
}
