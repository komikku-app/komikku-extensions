package eu.kanade.tachiyomi.extension.all.comickfun

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.interceptor.rateLimit
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import rx.Observable
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale

abstract class ComickFun(override val lang: String, private val comickFunLang: String) : HttpSource() {

    override val name = "Comick"

    override val baseUrl = "https://comick.app"

    private val apiUrl = "https://api.comick.fun"

    private val cdnUrl = "https://meo3.comick.pictures"

    override val supportsLatest = true

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        explicitNulls = true
    }

    override fun headersBuilder() = Headers.Builder().apply {
        add("Referer", "$baseUrl/")
        add("User-Agent", "Tachiyomi ${System.getProperty("http.agent")}")
    }

    override val client: OkHttpClient = network.client.newBuilder().rateLimit(4, 1).build()

    /** Popular Manga **/
    override fun popularMangaRequest(page: Int): Request {
        return searchMangaRequest(
            page = page,
            query = "",
            filters = FilterList(
                SortFilter("", getSortsList, defaultPopularSort),
            ),
        )
    }

    override fun popularMangaParse(response: Response) = searchMangaParse(response)

    /** Latest Manga **/
    override fun latestUpdatesRequest(page: Int): Request {
        return searchMangaRequest(
            page = page,
            query = "",
            filters = FilterList(
                SortFilter("", getSortsList, defaultLatestSort),
            ),
        )
    }

    override fun latestUpdatesParse(response: Response) = searchMangaParse(response)

    /** Manga Search **/
    override fun fetchSearchManga(page: Int, query: String, filters: FilterList): Observable<MangasPage> {
        if (!query.startsWith(SLUG_SEARCH_PREFIX)) {
            return super.fetchSearchManga(page, query, filters)
        }

        val slugOrHid = query.substringAfter(SLUG_SEARCH_PREFIX)
        return fetchMangaDetails(SManga.create().apply { this.url = "/comic/$slugOrHid#" }).map {
            MangasPage(listOf(it), false)
        }
    }

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        val url = "$apiUrl/v1.0/search".toHttpUrl().newBuilder().apply {
            if (query.isEmpty()) {
                filters.forEach { it ->
                    when (it) {
                        is CompletedFilter -> {
                            if (it.state) {
                                addQueryParameter("completed", "true")
                            }
                        }
                        is GenreFilter -> {
                            it.state.filter { (it as TriState).isIncluded() }.forEach {
                                addQueryParameter(
                                    "genres",
                                    (it as TriState).value,
                                )
                            }

                            it.state.filter { (it as TriState).isExcluded() }.forEach {
                                addQueryParameter(
                                    "excludes",
                                    (it as TriState).value,
                                )
                            }
                        }
                        is DemographicFilter -> {
                            it.state.filter { (it as CheckBox).state }.forEach {
                                addQueryParameter(
                                    "demographic",
                                    (it as CheckBox).value,
                                )
                            }
                        }
                        is TypeFilter -> {
                            it.state.filter { (it as CheckBox).state }.forEach {
                                addQueryParameter(
                                    "country",
                                    (it as CheckBox).value,
                                )
                            }
                        }
                        is SortFilter -> {
                            addQueryParameter("sort", it.getValue())
                        }
                        is CreatedAtFilter -> {
                            if (it.state > 0) {
                                addQueryParameter("time", it.getValue())
                            }
                        }
                        is MinimumFilter -> {
                            if (it.state.isNotEmpty()) {
                                addQueryParameter("minimum", it.state)
                            }
                        }
                        is FromYearFilter -> {
                            if (it.state.isNotEmpty()) {
                                addQueryParameter("from", it.state)
                            }
                        }
                        is ToYearFilter -> {
                            if (it.state.isNotEmpty()) {
                                addQueryParameter("to", it.state)
                            }
                        }
                        is TagFilter -> {
                            if (it.state.isNotEmpty()) {
                                it.state.split(",").forEach {
                                    addQueryParameter("tags", it.trim())
                                }
                            }
                        }
                        else -> {}
                    }
                }
            } else {
                addQueryParameter("q", query)
            }
            addQueryParameter("tachiyomi", "true")
            addQueryParameter("limit", "50")
            addQueryParameter("page", "$page")
        }.build()
        return GET(url, headers)
    }

    override fun searchMangaParse(response: Response): MangasPage {
        val isQueryPresent = response.request.url.queryParameterNames.contains("q")
        val result = json.decodeFromString<List<Manga>>(response.body.string())
        return MangasPage(
            result.map { data ->
                SManga.create().apply {
                    // appennding # at end as part of migration from slug to hid
                    url = "/comic/${data.hid}#"
                    title = data.title
                    thumbnail_url = "$cdnUrl/${data.md_covers[0].b2key}"
                }
            },
            /*
                api always returns `limit` amount of results
                for text search and page>=2 is always empty
                so here we are checking if url has the text query parameter
                to avoid false 'No result found' toasts.
             */
            hasNextPage = !isQueryPresent && result.size >= 50,
        )
    }

    /** Manga Details **/
    override fun mangaDetailsRequest(manga: SManga): Request {
        // Migration from slug based urls to hid based ones
        if (!manga.url.endsWith("#")) {
            throw Exception("Migrate from Comick to Comick")
        }

        val mangaUrl = manga.url.removeSuffix("#")
        return GET("$apiUrl$mangaUrl?tachiyomi=true", headers)
    }

    override fun mangaDetailsParse(response: Response): SManga {
        val mangaData = json.decodeFromString<MangaDetails>(response.body.string())
        return SManga.create().apply {
            // appennding # at end as part of migration from slug to hid
            url = "/comic/${mangaData.comic.hid}#"
            title = mangaData.comic.title
            artist = mangaData.artists.joinToString { it.name.trim() }
            author = mangaData.authors.joinToString { it.name.trim() }
            description = beautifyDescription(mangaData.comic.desc)
            genre = mangaData.genres.joinToString { it.name.trim() }
            status = parseStatus(mangaData.comic.status)
            thumbnail_url = "$cdnUrl/${mangaData.comic.md_covers[0].b2key}"
        }
    }

    override fun getMangaUrl(manga: SManga): String {
        return "$baseUrl${manga.url.removeSuffix("#")}"
    }

    /** Manga Chapter List **/
    override fun chapterListRequest(manga: SManga): Request {
        // Migration from slug based urls to hid based ones
        if (!manga.url.endsWith("#")) {
            throw Exception("Migrate from Comick to Comick")
        }

        return paginatedChapterListRequest(manga.url.removeSuffix("#"), 1)
    }

    private fun paginatedChapterListRequest(mangaUrl: String, page: Int): Request {
        return GET(
            "$apiUrl$mangaUrl".toHttpUrl().newBuilder().apply {
                addPathSegment("chapters")
                if (comickFunLang != "all") addQueryParameter("lang", comickFunLang)
                addQueryParameter("tachiyomi", "true")
                addQueryParameter("page", "$page")
            }.build(),
            headers,
        )
    }

    override fun chapterListParse(response: Response): List<SChapter> {
        val chapterListResponse = json.decodeFromString<ChapterList>(response.body.string())

        val mangaUrl = response.request.url.toString()
            .substringBefore("/chapters")
            .substringAfter(apiUrl)

        var resultSize = chapterListResponse.chapters.size
        var page = 2

        while (chapterListResponse.total > resultSize) {
            val newRequest = paginatedChapterListRequest(mangaUrl, page)
            val newResponse = client.newCall(newRequest).execute()
            val newChapterListResponse = json.decodeFromString<ChapterList>(newResponse.body.string())

            chapterListResponse.chapters += newChapterListResponse.chapters

            resultSize += newChapterListResponse.chapters.size
            page += 1
        }

        return chapterListResponse.chapters.map { chapter ->
            SChapter.create().apply {
                url = "$mangaUrl/${chapter.hid}-chapter-${chapter.chap}-$comickFunLang"
                name = beautifyChapterName(chapter.vol, chapter.chap, chapter.title)
                date_upload = chapter.created_at.let {
                    try {
                        dateFormat.parse(it)?.time ?: 0L
                    } catch (e: ParseException) {
                        0L
                    }
                }
                scanlator = chapter.group_name.joinToString().takeUnless { it.isBlank() } ?: "Unknown"
            }
        }
    }

    private val dateFormat by lazy {
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH)
    }

    override fun getChapterUrl(chapter: SChapter): String {
        return "$baseUrl${chapter.url}"
    }

    /** Chapter Pages **/
    override fun pageListRequest(chapter: SChapter): Request {
        val chapterHid = chapter.url.substringAfterLast("/").substringBefore("-")
        return GET("$apiUrl/chapter/$chapterHid?tachiyomi=true", headers)
    }

    override fun pageListParse(response: Response): List<Page> {
        val result = json.decodeFromString<PageList>(response.body.string())
        return result.chapter.images.mapIndexedNotNull { index, data ->
            if (data.url == null) null else Page(index = index, imageUrl = data.url)
        }
    }

    companion object {
        const val SLUG_SEARCH_PREFIX = "id:"
    }

    override fun imageUrlParse(response: Response): String {
        throw UnsupportedOperationException("Not used")
    }

    protected open val defaultPopularSort: Int = 0
    protected open val defaultLatestSort: Int = 4

    override fun getFilterList() = FilterList(
        getFilters(),
    )
}
