package eu.kanade.tachiyomi.extension.pt.reaperscans

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.asObservableSuccess
import eu.kanade.tachiyomi.network.interceptor.rateLimitHost
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import rx.Observable
import uy.kohesive.injekt.injectLazy

class ReaperScans : HttpSource() {

    override val name = "Reaper Scans"

    override val baseUrl = "https://reaperscans.com.br"

    override val lang = "pt-BR"

    override val supportsLatest = true

    // Migrated from Madara to a custom CMS.
    override val versionId = 2

    override val client: OkHttpClient = network.cloudflareClient.newBuilder()
        .rateLimitHost(API_URL.toHttpUrl(), 1, 2)
        .build()

    private val json: Json by injectLazy()

    override fun headersBuilder(): Headers.Builder = Headers.Builder()
        .add("Origin", baseUrl)
        .add("Referer", "$baseUrl/")

    override fun popularMangaRequest(page: Int): Request {
        val payloadObj = buildJsonObject {
            put("order", "desc")
            put("order_by", "total_views")
            put("series_status", "Ongoing")
            put("series_type", "Comic")
            put("tag_ids", JsonArray(emptyList()))
        }

        val payload = payloadObj.toString().toRequestBody(JSON_MEDIA_TYPE)

        val apiHeaders = headersBuilder()
            .add("Accept", ACCEPT_JSON)
            .add("Content-Type", payload.contentType().toString())
            .build()

        return POST("$API_URL/series/querysearch", apiHeaders, payload)
    }

    override fun popularMangaParse(response: Response): MangasPage {
        val mangaList = response.parseAs<List<ReaperSeriesDto>>()
            .map(ReaperSeriesDto::toSManga)

        return MangasPage(mangaList, hasNextPage = false)
    }

    override fun latestUpdatesRequest(page: Int): Request {
        val payloadObj = buildJsonObject {
            put("order", "desc")
            put("order_by", "latest")
            put("series_status", "Ongoing")
            put("series_type", "Comic")
            put("tag_ids", JsonArray(emptyList()))
        }

        val payload = payloadObj.toString().toRequestBody(JSON_MEDIA_TYPE)

        val apiHeaders = headersBuilder()
            .add("Accept", ACCEPT_JSON)
            .add("Content-Type", payload.contentType().toString())
            .build()

        return POST("$API_URL/series/querysearch", apiHeaders, payload)
    }

    override fun latestUpdatesParse(response: Response): MangasPage = popularMangaParse(response)

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        val sortByFilter = filters.filterIsInstance<SortByFilter>().firstOrNull()
        val sortAscending = sortByFilter?.state?.ascending ?: false
        val sortProperty = sortByFilter?.selected ?: "total_views"

        val status = filters.filterIsInstance<StatusFilter>()
            .firstOrNull()?.selected?.value ?: "Ongoing"

        val payloadObj = buildJsonObject {
            put("order", if (sortAscending) "asc" else "desc")
            put("order_by", sortProperty)
            put("series_status", status)
            put("series_type", "Comic")
            put("tag_ids", JsonArray(emptyList()))
        }

        val payload = payloadObj.toString().toRequestBody(JSON_MEDIA_TYPE)

        val apiHeaders = headersBuilder()
            .add("Accept", ACCEPT_JSON)
            .add("Content-Type", payload.contentType().toString())
            .build()

        val apiUrl = "$API_URL/series/querysearch".toHttpUrl().newBuilder()
            .addQueryParameter("q", query)
            .toString()

        return POST(apiUrl, apiHeaders, payload)
    }

    override fun searchMangaParse(response: Response): MangasPage {
        val query = response.request.url.queryParameter("q").orEmpty()

        var mangaList = response.parseAs<List<ReaperSeriesDto>>()
            .map(ReaperSeriesDto::toSManga)

        if (query.isNotBlank()) {
            mangaList = mangaList.filter { it.title.contains(query, ignoreCase = true) }
        }

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
        val seriesSlug = manga.url.substringAfterLast("/")

        val apiHeaders = headersBuilder()
            .add("Accept", ACCEPT_JSON)
            .build()

        return GET("$API_URL/series/$seriesSlug#${manga.status}", apiHeaders)
    }

    override fun mangaDetailsParse(response: Response): SManga {
        return response.parseAs<ReaperSeriesDto>().toSManga().apply {
            status = response.request.url.fragment?.toIntOrNull() ?: SManga.UNKNOWN
        }
    }

    override fun chapterListRequest(manga: SManga): Request = seriesDetailsRequest(manga)

    override fun chapterListParse(response: Response): List<SChapter> {
        val result = response.parseAs<ReaperSeriesDto>()
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

        return GET("$API_URL/series/chapter/$chapterId", apiHeaders)
    }

    override fun pageListParse(response: Response): List<Page> {
        return response.parseAs<ReaperReaderDto>().content?.images.orEmpty()
            .mapIndexed { i, url -> Page(i, "", "$API_URL/$url") }
    }

    override fun fetchImageUrl(page: Page): Observable<String> = Observable.just(page.imageUrl!!)

    override fun imageUrlParse(response: Response): String = ""

    override fun imageRequest(page: Page): Request {
        val imageHeaders = headersBuilder()
            .add("Accept", ACCEPT_IMAGE)
            .build()

        return GET(page.imageUrl!!, imageHeaders)
    }

    private fun getStatusList(): List<Status> = listOf(
        Status("Em andamento", "Ongoing"),
        Status("Em hiato", "Hiatus"),
        Status("Cancelado", "Dropped"),
    )

    private fun getSortProperties(): List<SortProperty> = listOf(
        SortProperty("Título", "title"),
        SortProperty("Visualizações", "total_views"),
        SortProperty("Data de criação", "latest")
    )

    override fun getFilterList(): FilterList = FilterList(
        StatusFilter(getStatusList()),
        SortByFilter(getSortProperties()),
    )

    private inline fun <reified T> Response.parseAs(): T = use {
        json.decodeFromString(it.body?.string().orEmpty())
    }

    companion object {
        private const val ACCEPT_IMAGE = "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8"
        private const val ACCEPT_JSON = "application/json, text/plain, */*"

        const val API_URL = "https://api.reaperscans.com.br"

        private val JSON_MEDIA_TYPE = "application/json".toMediaType()
    }
}
