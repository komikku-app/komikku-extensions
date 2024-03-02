package eu.kanade.tachiyomi.multisrc.masonry

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.UpdateStrategy
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import eu.kanade.tachiyomi.util.asJsoup
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import rx.Observable
import java.lang.UnsupportedOperationException

abstract class Masonry(
    override val name: String,
    override val baseUrl: String,
    override val lang: String,
) : ParsedHttpSource() {
    protected open val useAlternativeLatestRequest = false

    override val supportsLatest = true

    override val client = network.cloudflareClient

    override fun headersBuilder() = super.headersBuilder()
        .set("Referer", "$baseUrl/")

    /**
     * /updates/sort/popular/ doesn't support pages on all sites so we use filter instead
     * Some time, it has a bit different content comparing to filter so we still query it
     */
    override fun popularMangaRequest(page: Int): Request {
        val url = when (page) {
            1 -> baseUrl
            2 -> "$baseUrl/updates/sort/popular/"
            else -> "$baseUrl/updates/sort/filter/ord/popular/content/0/quality/0/tags/0/mpage/${page - 2}/"
        }

        return GET(url, headers)
    }

    override fun popularMangaParse(response: Response): MangasPage {
        return super.popularMangaParse(response)
    }

    override fun popularMangaSelector() = ".list-gallery:not(.static) figure:not(:has(a[href*=/video/]))"
    override fun popularMangaNextPageSelector() = ".pagination-a li.next"

    override fun popularMangaFromElement(element: Element) = SManga.create().apply {
        element.selectFirst("a")!!.also {
            setUrlWithoutDomain(it.absUrl("href"))
            title = it.attr("title")
        }
        thumbnail_url = element.selectFirst("img")?.imgAttr()
    }

    /**
     * Archive is sorted as post's ID
     *
     * Newest is sorted as post's date
     *
     * /updates/sort/newest/ is similar to
     *   => /updates/sort/newest/mpage/1/ (a bit out of sync)
     *   => /archive/
     *   => /archive/page/1/
     *   => /updates/sort/filter/ord/newest/content/0/quality/0/tags/0/
     *   => /updates/sort/filter/ord/newest/content/0/quality/0/tags/0/mpage/1/
     *
     * /updates/sort/newest/mpage/2/ is similar to
     *   => /archive/page/2/
     *   => /updates/sort/filter/ord/newest/content/0/quality/0/tags/0/mpage/2/
     */
    override fun latestUpdatesRequest(page: Int) =
        if (useAlternativeLatestRequest) {
            alternativeLatestRequest(page)
        } else {
            defaultLatestRequest(page)
        }

    private fun defaultLatestRequest(page: Int) =
        GET("$baseUrl/archive/page/$page/", headers)

    /**
     * Some sites doesn't support page for /updates/sort/newest/
     *  - JoyMii
     *  - XArt (doesn't work at all)
     * This URL is often not consistent
     */
    private fun alternativeLatestRequest(page: Int) =
        GET("$baseUrl/updates/sort/newest/mpage/$page/", headers)

    override fun latestUpdatesParse(response: Response) = popularMangaParse(response)
    override fun latestUpdatesSelector() = popularMangaSelector()
    override fun latestUpdatesNextPageSelector() = popularMangaNextPageSelector()
    override fun latestUpdatesFromElement(element: Element) = popularMangaFromElement(element)

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        return if (query.isNotEmpty()) {
            val url = "$baseUrl/search/post/".toHttpUrl().newBuilder()
                .addPathSegment(query.trim())
                .addEncodedPathSegments("mpage/$page/")
                .build()

            GET(url, headers)
        } else {
            val tagsFilter = filters.filterIsInstance<TagsFilter>().first()
            val sortFilter = filters.filterIsInstance<SortFilter>().first()

            val url = baseUrl.toHttpUrl().newBuilder().apply {
                if (tagsFilter.state.none { it.state }) {
                    when (sortFilter.state) {
                        0 -> {
                            // Trending: use /updates/sort/ since it won't be available with Filter
                            addPathSegment("updates")
                            sortFilter.getUriPartIfNeeded("search").also {
                                // Only EliteBabes & MetArt supports Pages for updates/sort/trending
                                if (it.isBlank()) {
                                    addEncodedPathSegments("page/$page/")
                                } else {
                                    addEncodedPathSegments(it)
                                    addEncodedPathSegments("mpage/$page/")
                                }
                            }
                        }
                        // Using a more effective request comparing to the /updates/sort/newest/
                        1 -> latestUpdatesRequest(page)
                        // Using a more effective request comparing to the /updates/sort/popular/
                        2 -> popularMangaRequest(page)
                    }
                } else {
                    // tag/ will support pages for both newest & popular on all sites, so no need to change
                    addPathSegment("tag")
                    addPathSegment(
                        tagsFilter.state
                            .filter { it.state }
                            .joinToString("+") { it.uriPart },
                    )
                    sortFilter.getUriPartIfNeeded("tag").also {
                        // Only EliteBabes supports Pages for tag/sort/trending
                        if (it.isBlank()) {
                            addEncodedPathSegments("page/$page/")
                        } else {
                            addEncodedPathSegments(it)
                            addEncodedPathSegments("mpage/$page/")
                        }
                    }
                }
            }.build()

            GET(url, headers)
        }
    }

    private var tags = emptyList<Tag>()
    private var tagsFetchAttempt = 0

    private fun getTags() {
        if (tags.isEmpty() && tagsFetchAttempt < 3) {
            runCatching {
                tags = client.newCall(GET("$baseUrl/updates/sort/newest/", headers))
                    .execute().asJsoup()
                    .select("#filter-a span:has(> input)")
                    .mapNotNull {
                        Tag(
                            it.select("label").text(),
                            it.select("input").attr("value"),
                        )
                    }
            }
            tagsFetchAttempt++
        }
    }

    override fun getFilterList(): FilterList {
        getTags()
        val filters = mutableListOf(
            Filter.Header("Filters ignored with text search"),
            Filter.Separator(),
            SortFilter(),
        )

        if (tags.isEmpty()) {
            filters.add(
                Filter.Header("Press 'reset' to attempt to load tags"),
            )
        } else {
            filters.add(
                TagsFilter(tags),
            )
        }

        return FilterList(filters)
    }

    override fun searchMangaParse(response: Response) = popularMangaParse(response)
    override fun searchMangaSelector() = popularMangaSelector()
    override fun searchMangaNextPageSelector() = popularMangaNextPageSelector()
    override fun searchMangaFromElement(element: Element) = popularMangaFromElement(element)

    override fun mangaDetailsParse(document: Document) = SManga.create().apply {
        document.selectFirst("p.link-btn")?.run {
            artist = select("a[href*=/model/]").eachText().joinToString()
            genre = select("a[href*=/tag/]").eachText().joinToString()
            author = selectFirst("a")?.text()
        }
        description = document.selectFirst("#content > p")?.text()
        status = SManga.COMPLETED
        update_strategy = UpdateStrategy.ONLY_FETCH_ONCE
    }

    override fun fetchChapterList(manga: SManga): Observable<List<SChapter>> {
        return Observable.just(
            listOf(
                SChapter.create().apply {
                    name = "Gallery"
                    url = manga.url
                },
            ),
        )
    }

    override fun chapterListSelector() = throw UnsupportedOperationException()
    override fun chapterFromElement(element: Element) = throw UnsupportedOperationException()

    override fun pageListParse(document: Document): List<Page> {
        return document.select(".list-gallery a[href^=https://cdn.]").mapIndexed { idx, img ->
            Page(idx, imageUrl = img.absUrl("href"))
        }
    }

    override fun imageUrlParse(document: Document) = throw UnsupportedOperationException()

    protected fun Element.imgAttr(): String? {
        return when {
            hasAttr("srcset") -> attr("abs:srcset").substringBefore(" ")
            hasAttr("data-cfsrc") -> attr("abs:data-cfsrc")
            hasAttr("data-src") -> attr("abs:data-src")
            hasAttr("data-lazy-src") -> attr("abs:data-lazy-src")
            else -> attr("abs:src")
        }
    }
}
