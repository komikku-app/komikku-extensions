package eu.kanade.tachiyomi.multisrc.heancms

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.asObservableSuccess
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import rx.Observable
import uy.kohesive.injekt.injectLazy

abstract class HeanCms(
    override val name: String,
    override val baseUrl: String,
    override val lang: String,
    protected val apiUrl: String = baseUrl.replace("://", "://api.")
) : HttpSource() {

    override val supportsLatest = true

    override val client: OkHttpClient = network.cloudflareClient

    protected val json: Json by injectLazy()

    protected val intl by lazy { HeanCmsIntl(lang) }

    private var seriesSlugMap: Map<String, String>? = null

    override fun headersBuilder(): Headers.Builder = Headers.Builder()
        .add("Origin", baseUrl)
        .add("Referer", "$baseUrl/")

    override fun popularMangaRequest(page: Int): Request {
        val payloadObj = HeanCmsSearchDto(
            order = "desc",
            orderBy = "total_views",
            status = "Ongoing",
            type = "Comic"
        )

        val payload = json.encodeToString(payloadObj).toRequestBody(JSON_MEDIA_TYPE)

        val apiHeaders = headersBuilder()
            .add("Accept", ACCEPT_JSON)
            .add("Content-Type", payload.contentType().toString())
            .build()

        return POST("$apiUrl/series/querysearch", apiHeaders, payload)
    }

    override fun popularMangaParse(response: Response): MangasPage {
        val mangaList = response.parseAs<List<HeanCmsSeriesDto>>()
            .map { it.toSManga(apiUrl) }

        fetchAllTitles()

        return MangasPage(mangaList, hasNextPage = false)
    }

    override fun latestUpdatesRequest(page: Int): Request {
        val payloadObj = HeanCmsSearchDto(
            order = "desc",
            orderBy = "latest",
            status = "Ongoing",
            type = "Comic"
        )

        val payload = json.encodeToString(payloadObj).toRequestBody(JSON_MEDIA_TYPE)

        val apiHeaders = headersBuilder()
            .add("Accept", ACCEPT_JSON)
            .add("Content-Type", payload.contentType().toString())
            .build()

        return POST("$apiUrl/series/querysearch", apiHeaders, payload)
    }

    override fun latestUpdatesParse(response: Response): MangasPage = popularMangaParse(response)

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        val sortByFilter = filters.firstInstanceOrNull<SortByFilter>()

        val payloadObj = HeanCmsSearchDto(
            order = if (sortByFilter?.state?.ascending == true) "asc" else "desc",
            orderBy = sortByFilter?.selected ?: "total_views",
            status = filters.firstInstanceOrNull<StatusFilter>()?.selected?.value ?: "Ongoing",
            type = "Comic",
            tagIds = filters.firstInstanceOrNull<GenreFilter>()?.state
                ?.filter(Genre::state)
                ?.map(Genre::id)
                .orEmpty()
        )

        val payload = json.encodeToString(payloadObj).toRequestBody(JSON_MEDIA_TYPE)

        val apiHeaders = headersBuilder()
            .add("Accept", ACCEPT_JSON)
            .add("Content-Type", payload.contentType().toString())
            .build()

        val apiUrl = "$apiUrl/series/querysearch".toHttpUrl().newBuilder()
            .addQueryParameter("q", query)
            .toString()

        return POST(apiUrl, apiHeaders, payload)
    }

    override fun searchMangaParse(response: Response): MangasPage {
        val query = response.request.url.queryParameter("q").orEmpty()

        var mangaList = response.parseAs<List<HeanCmsSeriesDto>>()
            .map { it.toSManga(apiUrl) }

        if (query.isNotBlank()) {
            mangaList = mangaList.filter { it.title.contains(query, ignoreCase = true) }
        }

        fetchAllTitles()

        return MangasPage(mangaList, hasNextPage = false)
    }

    // Workaround to allow "Open in browser" use the real URL.
    override fun fetchMangaDetails(manga: SManga): Observable<SManga> {
        return client.newCall(seriesDetailsRequest(manga))
            .asObservableSuccess()
            .map { response ->
                mangaDetailsParse(response).apply { initialized = true }
            }
    }

    private fun seriesDetailsRequest(manga: SManga): Request {
        val seriesSlug = manga.url
            .substringAfterLast("/")
            .replace(TIMESTAMP_REGEX, "")

        val currentSlug = seriesSlugMap?.get(seriesSlug) ?: seriesSlug

        val apiHeaders = headersBuilder()
            .add("Accept", ACCEPT_JSON)
            .build()

        return GET("$apiUrl/series/$currentSlug#${manga.status}", apiHeaders)
    }

    override fun mangaDetailsParse(response: Response): SManga {
        val result = runCatching { response.parseAs<HeanCmsSeriesDto>() }
        val seriesDetails = result.getOrNull()?.toSManga(apiUrl)
            ?: throw Exception(intl.urlChangedError(name))

        return seriesDetails.apply {
            status = response.request.url.fragment?.toIntOrNull() ?: SManga.UNKNOWN
        }
    }

    override fun chapterListRequest(manga: SManga): Request = seriesDetailsRequest(manga)

    override fun chapterListParse(response: Response): List<SChapter> {
        val result = response.parseAs<HeanCmsSeriesDto>()
        val seriesSlug = response.request.url.pathSegments.last()

        return result.chapters.orEmpty()
            .map { it.toSChapter(seriesSlug) }
            .reversed()
    }

    override fun pageListRequest(chapter: SChapter): Request {
        val chapterId = chapter.url.substringAfterLast("#")

        val apiHeaders = headersBuilder()
            .add("Accept", ACCEPT_JSON)
            .build()

        return GET("$apiUrl/series/chapter/$chapterId", apiHeaders)
    }

    override fun pageListParse(response: Response): List<Page> {
        return response.parseAs<HeanCmsReaderDto>().content?.images.orEmpty()
            .mapIndexed { i, url -> Page(i, "", "$apiUrl/$url") }
    }

    override fun fetchImageUrl(page: Page): Observable<String> = Observable.just(page.imageUrl!!)

    override fun imageUrlParse(response: Response): String = ""

    override fun imageRequest(page: Page): Request {
        val imageHeaders = headersBuilder()
            .add("Accept", ACCEPT_IMAGE)
            .build()

        return GET(page.imageUrl!!, imageHeaders)
    }

    protected open fun getStatusList(): List<Status> = listOf(
        Status(intl.statusOngoing, "Ongoing"),
        Status(intl.statusOnHiatus, "Hiatus"),
        Status(intl.statusDropped, "Dropped"),
    )

    protected open fun getSortProperties(): List<SortProperty> = listOf(
        SortProperty(intl.sortByTitle, "title"),
        SortProperty(intl.sortByViews, "total_views"),
        SortProperty(intl.sortByLatest, "latest"),
        SortProperty(intl.sortByRecentlyAdded, "recently_added"),
    )

    protected open fun getGenreList(): List<Genre> = emptyList()

    protected open fun fetchAllTitles() {
        if (!seriesSlugMap.isNullOrEmpty()) {
            return
        }

        val result = runCatching {
            client.newCall(allTitlesRequest()).execute()
                .let { parseAllTitles(it) }
        }

        seriesSlugMap = result.getOrNull()
    }

    protected open fun allTitlesRequest(): Request {
        val payloadObj = HeanCmsSearchDto(
            order = "desc",
            orderBy = "total_views",
            status = "",
            type = "Comic"
        )

        val payload = json.encodeToString(payloadObj).toRequestBody(JSON_MEDIA_TYPE)

        val apiHeaders = headersBuilder()
            .add("Accept", ACCEPT_JSON)
            .add("Content-Type", payload.contentType().toString())
            .build()

        return POST("$apiUrl/series/querysearch", apiHeaders, payload)
    }

    protected open fun parseAllTitles(response: Response): Map<String, String> {
        return response.parseAs<List<HeanCmsSeriesDto>>()
            .filter { it.type == "Comic" }
            .associateBy(
                keySelector = { it.slug.replace(TIMESTAMP_REGEX, "") },
                valueTransform = HeanCmsSeriesDto::slug
            )
    }

    override fun getFilterList(): FilterList {
        val genres = getGenreList()

        val filters = listOfNotNull(
            StatusFilter(intl.statusFilterTitle, getStatusList()),
            SortByFilter(intl.sortByFilterTitle, getSortProperties()),
            GenreFilter(intl.genreFilterTitle, genres).takeIf { genres.isNotEmpty() }
        )

        return FilterList(filters)
    }

    private inline fun <reified T> Response.parseAs(): T = use {
        json.decodeFromString(it.body?.string().orEmpty())
    }

    private inline fun <reified R> List<*>.firstInstanceOrNull(): R? =
        filterIsInstance<R>().firstOrNull()

    companion object {
        private const val ACCEPT_IMAGE = "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8"
        private const val ACCEPT_JSON = "application/json, text/plain, */*"

        private val JSON_MEDIA_TYPE = "application/json".toMediaType()

        val TIMESTAMP_REGEX = "-\\d+$".toRegex()
    }
}
