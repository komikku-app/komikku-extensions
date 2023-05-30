package eu.kanade.tachiyomi.extension.all.comickfun

import android.app.Application
import android.content.SharedPreferences
import androidx.preference.ListPreference
import androidx.preference.PreferenceScreen
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.interceptor.rateLimit
import eu.kanade.tachiyomi.source.ConfigurableSource
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
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.text.SimpleDateFormat
import java.util.Locale

abstract class ComickFun(
    override val lang: String,
    private val comickFunLang: String,
) : HttpSource(), ConfigurableSource {

    override val name = "Comick"

    override val baseUrl = "https://comick.app"

    private val apiUrl = "https://api.comick.fun"

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

    override val client: OkHttpClient = network.client.newBuilder()
        .addNetworkInterceptor(::thumbnailIntercept)
        .rateLimit(4, 1)
        .build()

    private val preferences: SharedPreferences by lazy {
        Injekt.get<Application>().getSharedPreferences("source_$id", 0x0000)
    }

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
        val manga = SManga.create().apply { this.url = "/comic/$slugOrHid#" }
        return fetchMangaDetails(manga).map {
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
        val result = response.parseAs<List<SearchManga>>()
        return MangasPage(
            result.map { it.toSManga(useScaledCover) },
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
        val mangaData = response.parseAs<Manga>()
        return mangaData.toSManga(useScaledCover)
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
        val chapterListResponse = response.parseAs<ChapterList>()

        val mangaUrl = response.request.url.toString()
            .substringBefore("/chapters")
            .substringAfter(apiUrl)

        var resultSize = chapterListResponse.chapters.size
        var page = 2

        while (chapterListResponse.total > resultSize) {
            val newRequest = paginatedChapterListRequest(mangaUrl, page)
            val newResponse = client.newCall(newRequest).execute()
            val newChapterListResponse = newResponse.parseAs<ChapterList>()

            chapterListResponse.chapters += newChapterListResponse.chapters

            resultSize += newChapterListResponse.chapters.size
            page += 1
        }

        return chapterListResponse.chapters.map { it.toSChapter(mangaUrl) }
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
        val result = response.parseAs<PageList>()
        return result.chapter.images.mapIndexedNotNull { index, data ->
            if (data.url == null) null else Page(index = index, imageUrl = data.url)
        }
    }

    private inline fun <reified T> Response.parseAs(): T {
        return json.decodeFromString(body.string())
    }

    override fun imageUrlParse(response: Response): String {
        throw UnsupportedOperationException("Not used")
    }

    protected open val defaultPopularSort: Int = 0
    protected open val defaultLatestSort: Int = 4

    override fun getFilterList() = FilterList(
        getFilters(),
    )

    override fun setupPreferenceScreen(screen: PreferenceScreen) {
        ListPreference(screen.context).apply {
            key = coverQualityPref
            title = "Cover Quality"
            entries = arrayOf("Original", "Scaled")
            entryValues = arrayOf("orig", "scaled")
            setDefaultValue("orig")
            summary = "%s"
        }.let { screen.addPreference(it) }
    }

    private val useScaledCover: Boolean by lazy {
        preferences.getString(coverQualityPref, "orig") != "orig"
    }

    companion object {
        const val SLUG_SEARCH_PREFIX = "id:"
        val dateFormat by lazy {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH)
        }
        val markdownLinksRegex = "\\[([^]]+)\\]\\(([^)]+)\\)".toRegex()
        val markdownItalicBoldRegex = "\\*+\\s*([^\\*]*)\\s*\\*+".toRegex()
        val markdownItalicRegex = "_+\\s*([^_]*)\\s*_+".toRegex()
        private const val coverQualityPref = "pref_cover_quality"
    }
}
