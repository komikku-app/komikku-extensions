package eu.kanade.tachiyomi.extension.en.inkr

import eu.kanade.tachiyomi.lib.ratelimit.RateLimitInterceptor
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.asObservableSuccess
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.util.asJsoup
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import rx.Observable
import uy.kohesive.injekt.injectLazy
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class Inkr : HttpSource() {

    override val name = "INKR"

    override val baseUrl = "https://inkr.com"

    override val lang = "en"

    override val supportsLatest = true

    override val client: OkHttpClient = network.cloudflareClient.newBuilder()
        .addInterceptor(::buildIdIntercept)
        .addInterceptor(RateLimitInterceptor(2, 1, TimeUnit.SECONDS))
        .build()

    private val json: Json by injectLazy()

    private var buildId: String? = null

    override fun headersBuilder(): Headers.Builder = Headers.Builder()
        .add("Origin", baseUrl)
        .add("Referer", "$baseUrl/")

    override fun popularMangaRequest(page: Int): Request {
        val newHeaders = headersBuilder()
            .add("Accept", ACCEPT_JSON)
            .build()

        return GET("$baseUrl/_next/data/buildId/index.json", newHeaders)
    }

    override fun popularMangaParse(response: Response): MangasPage {
        val result = json.decodeFromString<NextJsWrapper<InkrHome>>(response.body!!.string())

        if (result.pageProps == null) {
            return MangasPage(emptyList(), hasNextPage = false)
        }

        val comicList = result.pageProps.topCharts!!.topTrending.map(::popularMangaFromObject)

        return MangasPage(comicList, hasNextPage = false)
    }

    private fun popularMangaFromObject(comic: InkrComic) = SManga.create().apply {
        title = comic.name
        thumbnail_url = comic.thumbnailURL
        url = "/${comic.oid}"
    }

    override fun latestUpdatesRequest(page: Int): Request = popularMangaRequest(page)

    override fun latestUpdatesParse(response: Response): MangasPage {
        val result = json.decodeFromString<NextJsWrapper<InkrHome>>(response.body!!.string())

        if (result.pageProps == null) {
            return MangasPage(emptyList(), hasNextPage = false)
        }

        val comicList = result.pageProps.latestUpdateDetails.map(::latestMangaFromObject)

        return MangasPage(comicList, hasNextPage = false)
    }

    private fun latestMangaFromObject(comic: InkrComic) = popularMangaFromObject(comic)

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        val payload = buildJsonObject { put("query", query) }

        val requestBody = payload.toString().toRequestBody(JSON_MEDIA_TYPE)

        val newHeaders = headersBuilder()
            .add("Accept", ACCEPT_JSON)
            .add("Cf-Ipcountry", "VN")
            .add("Content-Length", requestBody.contentLength().toString())
            .add("Content-Type", requestBody.contentType().toString())
            .add("Ikc-Platform", "android")
            .build()

        return POST("$ICQ_API_URL/title/search", newHeaders, requestBody)
    }

    private fun searchDetailsRequest(oids: List<String>): Request {
        val payload = buildJsonObject {
            putJsonArray("fields") {
                add("oid")
                add("name")
                add("thumbnailURL")
            }
            put("oids", json.encodeToJsonElement(oids))
            put("url", "$ICD_API_URL/content_json")
        }

        val requestBody = payload.toString().toRequestBody(JSON_MEDIA_TYPE)

        val newHeaders = headersBuilder()
            .add("Accept", ACCEPT_JSON)
            .add("Cf-Ipcountry", "VN")
            .add("Content-Length", requestBody.contentLength().toString())
            .add("Content-Type", requestBody.contentType().toString())
            .add("Ikc-Platform", "android")
            .build()

        return POST("$ICD_API_URL/content_json", newHeaders, requestBody)
    }

    override fun searchMangaParse(response: Response): MangasPage {
        val result = json.decodeFromString<InkrResult<InkrSearch>>(response.body!!.string())

        if (result.data == null) {
            return MangasPage(emptyList(), hasNextPage = false)
        }

        val searchResults = result.data.title.take(SEARCH_LIMIT)

        val detailsRequest = searchDetailsRequest(searchResults)
        val detailsResponse = client.newCall(detailsRequest).execute()
        val detailsJson = detailsResponse.body!!.string()
        val detailsResult = json.decodeFromString<InkrResult<Map<String, InkrComic>>>(detailsJson)

        if (detailsResult.data == null) {
            return MangasPage(emptyList(), hasNextPage = false)
        }

        // Use the searchResults to iterate to keep the result order.
        val comicList = searchResults.map { oid ->
            searchMangaFromObject(detailsResult.data[oid]!!)
        }

        return MangasPage(comicList, hasNextPage = false)
    }

    private fun searchMangaFromObject(comic: InkrComic) = popularMangaFromObject(comic)

    // Workaround to allow "Open in browser" use the real URL.
    override fun fetchMangaDetails(manga: SManga): Observable<SManga> {
        return client.newCall(mangaDetailsApiRequest(manga.url))
            .asObservableSuccess()
            .map { response ->
                mangaDetailsParse(response).apply { initialized = true }
            }
    }

    private fun mangaDetailsApiRequest(mangaUrl: String): Request {
        val newHeaders = headersBuilder()
            .add("Accept", ACCEPT_JSON)
            .build()

        val comicId = mangaUrl.substringAfterLast("/")

        return GET("$baseUrl/_next/data/buildId/$comicId.json", newHeaders)
    }

    override fun mangaDetailsRequest(manga: SManga): Request {
        val newHeaders = headersBuilder()
            .removeAll("Accept")
            .build()

        return GET(baseUrl + manga.url, newHeaders)
    }

    override fun mangaDetailsParse(response: Response): SManga = SManga.create().apply {
        val result = json.decodeFromString<NextJsWrapper<InkrTitleInfo>>(response.body!!.string())

        if (result.pageProps == null) {
            throw Exception(COULD_NOT_PARSE_RESPONSE)
        }

        val comic = result.pageProps.titleInfo!!

        title = comic.name
        author = comic.creators
            .filter { it.role == "story" }
            .joinToString(", ") { it.name }
        artist = comic.creators
            .filter { it.role == "art" }
            .joinToString(", ") { it.name }
        description = comic.summary.joinToString("\n\n")
            .plus(if (comic.extras?.containsKey("Copyright") == true) "\n\n${comic.extras["Copyright"]}" else "")
        genre = comic.listGenre?.jsonArray?.joinToString(", ") { it.jsonPrimitive.content }
        status = comic.releaseStatus.toStatus()
        thumbnail_url = comic.thumbnailURL
    }

    override fun chapterListRequest(manga: SManga): Request = mangaDetailsApiRequest(manga.url)

    override fun chapterListParse(response: Response): List<SChapter> {
        val result = json.decodeFromString<NextJsWrapper<InkrTitleInfo>>(response.body!!.string())

        if (result.pageProps == null) {
            throw Exception(COULD_NOT_PARSE_RESPONSE)
        }

        val comic = result.pageProps.titleInfo!!

        if (comic.webPreviewingPages.isEmpty()) {
            return emptyList()
        }

        val previewChapter = SChapter.create().apply {
            name = "Preview"
            scanlator = comic.creators.firstOrNull { it.role == "publisher" }?.name
            date_upload = comic.firstChapterFirstPublishedDate.toDate()
            url = "/${comic.oid}"
        }

        return listOf(previewChapter)
    }

    override fun pageListRequest(chapter: SChapter): Request = mangaDetailsApiRequest(chapter.url)

    override fun pageListParse(response: Response): List<Page> {
        val result = json.decodeFromString<NextJsWrapper<InkrTitleInfo>>(response.body!!.string())

        if (result.pageProps == null) {
            throw Exception(COULD_NOT_PARSE_RESPONSE)
        }

        val comic = result.pageProps.titleInfo!!
        val referer = "$baseUrl/"

        return comic.webPreviewingPages
            .mapIndexed { i, page -> Page(i, referer, page.url) }
    }

    override fun fetchImageUrl(page: Page): Observable<String> = Observable.just(page.imageUrl!!)

    override fun imageUrlParse(response: Response): String = ""

    override fun imageRequest(page: Page): Request {
        val newHeaders = headersBuilder()
            .set("Accept", ACCEPT_IMAGE)
            .set("Referer", page.url)
            .build()

        return GET(page.imageUrl!!, newHeaders)
    }

    private fun buildIdIntercept(chain: Interceptor.Chain): Response {
        if (chain.request().url.toString().contains("/buildId/").not()) {
            return chain.proceed(chain.request())
        }

        if (buildId == null) {
            val buildIdRequest = GET(baseUrl, headers)
            val buildIdResponse = chain.proceed(buildIdRequest)
            val document = buildIdResponse.asJsoup()

            val nextData = document.select("script#__NEXT_DATA__")
                .firstOrNull()?.data() ?: throw IOException(COULD_NOT_FIND_BUILD_ID)
            val nextJson = json.parseToJsonElement(nextData).jsonObject

            buildId = nextJson["buildId"]!!.jsonPrimitive.content

            buildIdResponse.close()
        }

        val newRequestUrl = chain.request().url.toString()
            .replace("buildId", buildId!!)
            .toHttpUrl()

        val newRequest = chain.request().newBuilder()
            .url(newRequestUrl)
            .build()

        return chain.proceed(newRequest)
    }

    private fun String.toDate(): Long {
        return runCatching { DATE_FORMATTER.parse(substringBefore("T"))?.time }
            .getOrNull() ?: 0L
    }

    private fun String.toStatus(): Int = when (this) {
        "ongoing", "hold" -> SManga.ONGOING
        "completed" -> SManga.COMPLETED
        else -> SManga.UNKNOWN
    }

    companion object {
        private const val ICQ_API_URL = "https://icq-api.inkr.com/v1"
        private const val ICD_API_URL = "https://icd-api.inkr.com/v1"

        private const val ACCEPT_JSON = "application/json, text/plain, */*"
        private const val ACCEPT_IMAGE = "image/avif,image/webp,image/apng,image/*,*/*;q=0.8"

        private val JSON_MEDIA_TYPE = "application/json; charset=UTF-8".toMediaType()

        private const val COULD_NOT_FIND_BUILD_ID = "Could not find the API token."
        private const val COULD_NOT_PARSE_RESPONSE = "Could not parse the API response."

        private const val SEARCH_LIMIT = 30

        private val DATE_FORMATTER by lazy { SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH) }
    }
}
