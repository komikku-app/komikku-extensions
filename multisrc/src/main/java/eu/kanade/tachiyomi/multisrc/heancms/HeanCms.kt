package eu.kanade.tachiyomi.multisrc.heancms

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.source.model.Filter
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
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import rx.Observable
import java.text.SimpleDateFormat
import java.util.Locale

abstract class HeanCms(
    override val name: String,
    override val baseUrl: String,
    override val lang: String,
    protected val apiUrl: String = baseUrl.replace("://", "://api."),
) : HttpSource() {

    override val supportsLatest = true

    override val client: OkHttpClient = network.cloudflareClient

    /**
     * Custom Json instance to make usage of `encodeDefaults`,
     * which is not enabled on the injected instance of the app.
     */
    protected val json: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }

    protected val intl by lazy { HeanCmsIntl(lang) }

    protected open val coverPath: String = "cover/"

    protected open val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZZZZZ", Locale.US)

    override fun headersBuilder(): Headers.Builder = Headers.Builder()
        .add("Origin", baseUrl)
        .add("Referer", "$baseUrl/")

    override fun popularMangaRequest(page: Int): Request {
        val payloadObj = HeanCmsQuerySearchPayloadDto(
            page = page,
            order = "desc",
            orderBy = "total_views",
            status = "All",
            type = "Comic",
        )

        val payload = json.encodeToString(payloadObj).toRequestBody(JSON_MEDIA_TYPE)

        val apiHeaders = headersBuilder()
            .add("Accept", ACCEPT_JSON)
            .add("Content-Type", payload.contentType().toString())
            .build()

        return POST("$apiUrl/series/querysearch", apiHeaders, payload)
    }

    override fun popularMangaParse(response: Response): MangasPage {
        val json = response.body.string()

        if (json.startsWith("{")) {
            val result = json.parseAs<HeanCmsQuerySearchDto>()
            val mangaList = result.data.map { it.toSManga(apiUrl, coverPath) }

            return MangasPage(mangaList, result.meta?.hasNextPage ?: false)
        }

        val mangaList = json.parseAs<List<HeanCmsSeriesDto>>()
            .map { it.toSManga(apiUrl, coverPath) }

        return MangasPage(mangaList, hasNextPage = false)
    }

    override fun latestUpdatesRequest(page: Int): Request {
        val payloadObj = HeanCmsQuerySearchPayloadDto(
            page = page,
            order = "desc",
            orderBy = "latest",
            status = "All",
            type = "Comic",
        )

        val payload = json.encodeToString(payloadObj).toRequestBody(JSON_MEDIA_TYPE)

        val apiHeaders = headersBuilder()
            .add("Accept", ACCEPT_JSON)
            .add("Content-Type", payload.contentType().toString())
            .build()

        return POST("$apiUrl/series/querysearch", apiHeaders, payload)
    }

    override fun latestUpdatesParse(response: Response): MangasPage = popularMangaParse(response)

    override fun fetchSearchManga(page: Int, query: String, filters: FilterList): Observable<MangasPage> {
        if (!query.startsWith(SEARCH_PREFIX)) {
            return super.fetchSearchManga(page, query, filters)
        }

        val slug = query.substringAfter(SEARCH_PREFIX)
        val manga = SManga.create().apply { url = "/series/$slug" }

        return fetchMangaDetails(manga).map { MangasPage(listOf(it), false) }
    }

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        if (query.isNotBlank()) {
            val searchPayloadObj = HeanCmsSearchPayloadDto(query)
            val searchPayload = json.encodeToString(searchPayloadObj)
                .toRequestBody(JSON_MEDIA_TYPE)

            val apiHeaders = headersBuilder()
                .add("Accept", ACCEPT_JSON)
                .add("Content-Type", searchPayload.contentType().toString())
                .build()

            return POST("$apiUrl/series/search", apiHeaders, searchPayload)
        }

        val sortByFilter = filters.firstInstanceOrNull<SortByFilter>()

        val payloadObj = HeanCmsQuerySearchPayloadDto(
            page = page,
            order = if (sortByFilter?.state?.ascending == true) "asc" else "desc",
            orderBy = sortByFilter?.selected ?: "total_views",
            status = filters.firstInstanceOrNull<StatusFilter>()?.selected?.value ?: "Ongoing",
            type = "Comic",
            tagIds = filters.firstInstanceOrNull<GenreFilter>()?.state
                ?.filter(Genre::state)
                ?.map(Genre::id)
                .orEmpty(),
        )

        val payload = json.encodeToString(payloadObj).toRequestBody(JSON_MEDIA_TYPE)

        val apiHeaders = headersBuilder()
            .add("Accept", ACCEPT_JSON)
            .add("Content-Type", payload.contentType().toString())
            .build()

        return POST("$apiUrl/series/querysearch", apiHeaders, payload)
    }

    override fun searchMangaParse(response: Response): MangasPage {
        val json = response.body.string()

        if (response.request.url.pathSegments.last() == "search") {
            val result = json.parseAs<List<HeanCmsSearchDto>>()
            val mangaList = result
                .filter { it.type == "Comic" }
                .map {
                    it.slug = it.slug.replace(TIMESTAMP_REGEX, "")
                    it.toSManga(apiUrl, coverPath)
                }

            return MangasPage(mangaList, false)
        }

        if (json.startsWith("{")) {
            val result = json.parseAs<HeanCmsQuerySearchDto>()
            val mangaList = result.data.map { it.toSManga(apiUrl, coverPath) }

            return MangasPage(mangaList, result.meta?.hasNextPage ?: false)
        }

        val mangaList = json.parseAs<List<HeanCmsSeriesDto>>()
            .map { it.toSManga(apiUrl, coverPath) }

        return MangasPage(mangaList, hasNextPage = false)
    }

    override fun getMangaUrl(manga: SManga) = baseUrl + manga.url

    override fun mangaDetailsRequest(manga: SManga): Request {
        val seriesSlug = manga.url
            .substringAfterLast("/")

        val apiHeaders = headersBuilder()
            .add("Accept", ACCEPT_JSON)
            .build()

        return GET("$apiUrl/series/$seriesSlug#${manga.status}", apiHeaders)
    }

    override fun mangaDetailsParse(response: Response): SManga {
        val mangaStatus = response.request.url.fragment?.toIntOrNull() ?: SManga.UNKNOWN

        val result = runCatching { response.parseAs<HeanCmsSeriesDto>() }

        val seriesDetails = result.getOrNull()?.toSManga(apiUrl, coverPath)
            ?: throw Exception(intl.urlChangedError(name))

        return seriesDetails.apply {
            status = status.takeUnless { it == SManga.UNKNOWN }
                ?: mangaStatus
        }
    }

    override fun chapterListRequest(manga: SManga): Request = mangaDetailsRequest(manga)

    override fun chapterListParse(response: Response): List<SChapter> {
        val result = response.parseAs<HeanCmsSeriesDto>()

        val currentTimestamp = System.currentTimeMillis()

        return result.chapters.orEmpty()
            .filterNot { it.price == 1 }
            .map { it.toSChapter(result.slug, dateFormat) }
            .filter { it.date_upload <= currentTimestamp }
            .reversed()
    }

    override fun getChapterUrl(chapter: SChapter): String = baseUrl + chapter.url

    override fun pageListRequest(chapter: SChapter): Request {
        val chapterId = chapter.url.substringAfterLast("#")

        val apiHeaders = headersBuilder()
            .add("Accept", ACCEPT_JSON)
            .build()

        return GET("$apiUrl/series/chapter/$chapterId", apiHeaders)
    }

    override fun pageListParse(response: Response): List<Page> {
        return response.parseAs<HeanCmsReaderDto>().content?.images.orEmpty()
            .filterNot { imageUrl ->
                // Their image server returns HTTP 403 for hidden files that starts
                // with a dot in the file name. To avoid download errors, these are removed.
                imageUrl
                    .removeSuffix("/")
                    .substringAfterLast("/")
                    .startsWith(".")
            }
            .mapIndexed { i, url ->
                Page(i, imageUrl = if (url.startsWith("http")) url else "$apiUrl/$url")
            }
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
        Status(intl.statusAll, "All"),
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

    override fun getFilterList(): FilterList {
        val genres = getGenreList()

        val filters = listOfNotNull(
            Filter.Header(intl.filterWarning),
            StatusFilter(intl.statusFilterTitle, getStatusList()),
            SortByFilter(intl.sortByFilterTitle, getSortProperties()),
            GenreFilter(intl.genreFilterTitle, genres).takeIf { genres.isNotEmpty() },
        )

        return FilterList(filters)
    }

    protected inline fun <reified T> Response.parseAs(): T = use {
        it.body.string().parseAs()
    }

    protected inline fun <reified T> String.parseAs(): T = json.decodeFromString(this)

    protected inline fun <reified R> List<*>.firstInstanceOrNull(): R? =
        filterIsInstance<R>().firstOrNull()

    companion object {
        private const val ACCEPT_IMAGE = "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8"
        private const val ACCEPT_JSON = "application/json, text/plain, */*"

        private val JSON_MEDIA_TYPE = "application/json".toMediaType()

        val TIMESTAMP_REGEX = """-\d{13}$""".toRegex()

        const val SEARCH_PREFIX = "slug:"
    }
}
