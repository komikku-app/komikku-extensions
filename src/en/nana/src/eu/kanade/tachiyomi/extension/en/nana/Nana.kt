package eu.kanade.tachiyomi.extension.en.nana

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.asObservable
import eu.kanade.tachiyomi.network.interceptor.rateLimit
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.UpdateStrategy
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import okhttp3.Call
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import rx.Observable

class Nana : ParsedHttpSource() {
    override val name = "Nana ナナ"

    override val baseUrl = "https://nana.my.id"

    override val lang = "en"

    override val supportsLatest = false

    override val client = super.client.newBuilder()
        .rateLimit(1)
        .build()

    // ~~Popular~~ Latest
    override fun popularMangaRequest(page: Int): Request =
        searchMangaRequest(page, "", FilterList())

    override fun popularMangaSelector(): String =
        searchMangaSelector()

    override fun popularMangaFromElement(element: Element): SManga =
        searchMangaFromElement(element)

    override fun popularMangaNextPageSelector(): String? =
        searchMangaNextPageSelector()

    // Search
    // The search returns 404 when there's no results.
    override fun fetchSearchManga(page: Int, query: String, filters: FilterList): Observable<MangasPage> {
        return client.newCall(searchMangaRequest(page, query, filters))
            .asObservableIgnoreCode(404)
            .map(::searchMangaParse)
    }

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        val filterList = if (filters.isEmpty()) getFilterList() else filters
        val tagsFilter = filterList.find { it is TagsFilter } as TagsFilter
        val sortFilter = filterList.find { it is SortFilter } as SortFilter

        val url = "$baseUrl/".toHttpUrl().newBuilder()
            .addQueryParameter("q", "${tagsFilter.toUriPart()} $query".trim())
            .addQueryParameter("sort", sortFilter.toUriPart())

        if (page != 1) {
            url.addQueryParameter("p", page.toString())
        }

        return GET(url.toString(), headers)
    }

    override fun searchMangaSelector(): String =
        "#thumbs_container > .id1"

    override fun searchMangaFromElement(element: Element): SManga = SManga.create().apply {
        val a = element.selectFirst(".id3 > a")!!
        setUrlWithoutDomain(a.absUrl("href"))
        title = a.attr("title")

        val img = a.selectFirst("> img")!!
        thumbnail_url = img.absUrl("src")
        author = img.attr("alt")
            .replace("$title by ", "")
            .ifBlank { null }

        genre = element.select(".id4 > .tags > span")
            .joinToString { it.text() }

        status = SManga.COMPLETED
        update_strategy = UpdateStrategy.ONLY_FETCH_ONCE
        initialized = true
    }

    override fun searchMangaNextPageSelector(): String =
        "a.paginate_button.current + a.paginate_button"

    // Latest
    override fun latestUpdatesRequest(page: Int): Request =
        throw UnsupportedOperationException("Not used.")

    override fun latestUpdatesSelector(): String =
        throw UnsupportedOperationException("Not used.")

    override fun latestUpdatesFromElement(element: Element): SManga =
        throw UnsupportedOperationException("Not used.")

    override fun latestUpdatesNextPageSelector(): String =
        throw UnsupportedOperationException("Not used.")

    // Details
    override fun fetchMangaDetails(manga: SManga): Observable<SManga> =
        Observable.just(manga)

    override fun mangaDetailsParse(document: Document): SManga =
        throw UnsupportedOperationException("Not used.")

    // Chapters
    override fun fetchChapterList(manga: SManga): Observable<List<SChapter>> {
        return Observable.just(
            listOf(
                SChapter.create().apply {
                    setUrlWithoutDomain(manga.url)
                    name = "Chapter"
                    date_upload = 0L
                    chapter_number = 1F
                },
            ),
        )
    }

    override fun chapterListSelector(): String =
        throw UnsupportedOperationException("Not used.")

    override fun chapterFromElement(element: Element): SChapter =
        throw UnsupportedOperationException("Not used.")

    // Pages
    override fun pageListParse(document: Document): List<Page> {
        val body = document.body().toString()

        return PATTERN_PAGES.find(body)
            ?.groupValues?.get(1)
            ?.split(',')
            ?.map(String::trim)
            ?.mapIndexed { i, imgStr ->
                val imgUrl = baseUrl + imgStr.substring(1, imgStr.lastIndex)
                Page(i, "", imgUrl)
            }
            ?: emptyList()
    }

    override fun imageUrlParse(document: Document): String =
        throw UnsupportedOperationException("Not used.")

    // Filters
    override fun getFilterList(): FilterList = FilterList(
        Filter.Header("Use comma (,) to separate tags"),
        Filter.Header("Prefix plus (+) to require tag"),
        Filter.Header("Prefix minus (-) to exclude tag"),
        TagsFilter(),

        Filter.Separator(),
        SortFilter(),
    )

    open class TagsFilter :
        Filter.Text("Tags", "") {
        fun toUriPart(): String {
            return state.split(',')
                .map(String::trim)
                .map { tag ->
                    if (tag.isEmpty() || tag.contains('"')) { return@map tag }

                    val prefix = tag.substring(0, 1)

                    if (listOf("+", "-").any { prefix.contains(it) }) {
                        "$prefix\"${tag.substring(1)}\""
                    } else {
                        "\"$tag\""
                    }
                }
                .joinToString(" ")
        }
    }

    open class SortFilter :
        Filter.Sort("Sort", arrayOf("Date Added"), Selection(0, false)) {
        fun toUriPart(): String = when (state?.ascending) {
            true -> "asc"
            else -> "desc"
        }
    }

    // Other
    private fun Call.asObservableIgnoreCode(code: Int): Observable<Response> {
        return asObservable().doOnNext { response ->
            if (!response.isSuccessful && response.code != code) {
                response.close()
                throw Exception("HTTP error ${response.code}")
            }
        }
    }

    companion object {
        private val PATTERN_PAGES = Regex("Reader\\.pages\\s*=\\s*\\{\\\"pages\\\":\\[([^];\\n]+)]\\}\\.pages;")
    }
}
