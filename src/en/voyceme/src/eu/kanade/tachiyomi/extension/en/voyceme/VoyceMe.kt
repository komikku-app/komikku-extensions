package eu.kanade.tachiyomi.extension.en.voyceme

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.asObservableSuccess
import eu.kanade.tachiyomi.network.interceptor.rateLimit
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.util.asJsoup
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import rx.Observable
import uy.kohesive.injekt.injectLazy
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class VoyceMe : HttpSource() {

    override val name = "Voyce.Me"

    override val baseUrl = "http://voyce.me"

    override val lang = "en"

    override val supportsLatest = true

    override val client: OkHttpClient = network.cloudflareClient.newBuilder()
        .rateLimit(2, 1, TimeUnit.SECONDS)
        .build()

    private val json: Json by injectLazy()

    override fun headersBuilder(): Headers.Builder = super.headersBuilder()
        .add("Accept", ACCEPT_ALL)
        .add("Origin", baseUrl)
        .add("Referer", "$baseUrl/")

    private fun genericComicBookFromObject(comic: VoyceMeComic): SManga =
        SManga.create().apply {
            title = comic.title
            url = "/series/${comic.slug}"
            thumbnail_url = STATIC_URL + comic.thumbnail
        }

    override fun popularMangaRequest(page: Int): Request {
        val payload = buildJsonObject {
            put("query", POPULAR_QUERY)
            putJsonObject("variables") {
                put("offset", (page - 1) * POPULAR_PER_PAGE)
                put("limit", POPULAR_PER_PAGE)
            }
        }

        val body = payload.toString().toRequestBody(JSON_MEDIA_TYPE)

        val newHeaders = headersBuilder()
            .add("Content-Length", body.contentLength().toString())
            .add("Content-Type", body.contentType().toString())
            .build()

        return POST(GRAPHQL_URL, newHeaders, body)
    }

    override fun popularMangaParse(response: Response): MangasPage {
        val result = json.parseToJsonElement(response.body.string()).jsonObject

        val comicList = result["data"]!!.jsonObject["voyce_series"]!!
            .let { json.decodeFromJsonElement<List<VoyceMeComic>>(it) }
            .map(::genericComicBookFromObject)
        val hasNextPage = comicList.size == POPULAR_PER_PAGE

        return MangasPage(comicList, hasNextPage)
    }

    override fun latestUpdatesRequest(page: Int): Request {
        val payload = buildJsonObject {
            put("query", LATEST_QUERY)
            putJsonObject("variables") {
                put("offset", (page - 1) * POPULAR_PER_PAGE)
                put("limit", POPULAR_PER_PAGE)
            }
        }

        val body = payload.toString().toRequestBody(JSON_MEDIA_TYPE)

        val newHeaders = headersBuilder()
            .add("Content-Length", body.contentLength().toString())
            .add("Content-Type", body.contentType().toString())
            .build()

        return POST(GRAPHQL_URL, newHeaders, body)
    }

    override fun latestUpdatesParse(response: Response): MangasPage {
        val result = json.parseToJsonElement(response.body.string()).jsonObject

        val comicList = result["data"]!!.jsonObject["voyce_series"]!!
            .let { json.decodeFromJsonElement<List<VoyceMeComic>>(it) }
            .map(::genericComicBookFromObject)
        val hasNextPage = comicList.size == POPULAR_PER_PAGE

        return MangasPage(comicList, hasNextPage)
    }

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        val payload = buildJsonObject {
            put("query", SEARCH_QUERY)
            putJsonObject("variables") {
                put("searchTerm", "%$query%")
                put("offset", (page - 1) * POPULAR_PER_PAGE)
                put("limit", POPULAR_PER_PAGE)
            }
        }

        val body = payload.toString().toRequestBody(JSON_MEDIA_TYPE)

        val newHeaders = headersBuilder()
            .add("Content-Length", body.contentLength().toString())
            .add("Content-Type", body.contentType().toString())
            .build()

        return POST(GRAPHQL_URL, newHeaders, body)
    }

    override fun searchMangaParse(response: Response): MangasPage {
        val result = json.parseToJsonElement(response.body.string()).jsonObject

        val comicList = result["data"]!!.jsonObject["voyce_series"]!!
            .let { json.decodeFromJsonElement<List<VoyceMeComic>>(it) }
            .map(::genericComicBookFromObject)
        val hasNextPage = comicList.size == POPULAR_PER_PAGE

        return MangasPage(comicList, hasNextPage)
    }

    // Workaround to allow "Open in browser" use the real URL.
    override fun fetchMangaDetails(manga: SManga): Observable<SManga> {
        return client.newCall(mangaDetailsApiRequest(manga))
            .asObservableSuccess()
            .map { response ->
                mangaDetailsParse(response).apply { initialized = true }
            }
    }

    private fun mangaDetailsApiRequest(manga: SManga): Request {
        val comicSlug = manga.url
            .substringAfter("/series/")
            .substringBefore("/")

        val payload = buildJsonObject {
            put("query", DETAILS_QUERY)
            putJsonObject("variables") {
                put("slug", comicSlug)
            }
        }

        val body = payload.toString().toRequestBody(JSON_MEDIA_TYPE)

        val newHeaders = headersBuilder()
            .add("Content-Length", body.contentLength().toString())
            .add("Content-Type", body.contentType().toString())
            .set("Referer", baseUrl + manga.url)
            .build()

        return POST(GRAPHQL_URL, newHeaders, body)
    }

    override fun mangaDetailsParse(response: Response): SManga = SManga.create().apply {
        val result = json.parseToJsonElement(response.body.string()).jsonObject
        val comic = result["data"]!!.jsonObject["voyce_series"]!!.jsonArray[0].jsonObject
            .let { json.decodeFromJsonElement<VoyceMeComic>(it) }

        title = comic.title
        author = comic.author?.username.orEmpty()
        description = Parser.unescapeEntities(comic.description.orEmpty(), true)
            .let { Jsoup.parse(it).text() }
        status = comic.status.orEmpty().toStatus()
        genre = comic.genres.mapNotNull { it.genre?.title }.joinToString(", ")
        thumbnail_url = STATIC_URL + comic.thumbnail
    }

    override fun chapterListRequest(manga: SManga): Request {
        val comicSlug = manga.url
            .substringAfter("/series/")
            .substringBefore("/")

        val payload = buildJsonObject {
            put("query", CHAPTERS_QUERY)
            putJsonObject("variables") {
                put("slug", comicSlug)
            }
        }

        val body = payload.toString().toRequestBody(JSON_MEDIA_TYPE)

        val newHeaders = headersBuilder()
            .add("Content-Length", body.contentLength().toString())
            .add("Content-Type", body.contentType().toString())
            .set("Referer", baseUrl + manga.url)
            .build()

        return POST(GRAPHQL_URL, newHeaders, body)
    }

    override fun chapterListParse(response: Response): List<SChapter> {
        val result = json.parseToJsonElement(response.body.string()).jsonObject
        val comicBook = result["data"]!!.jsonObject["voyce_series"]!!.jsonArray[0].jsonObject
            .let { json.decodeFromJsonElement<VoyceMeComic>(it) }

        return comicBook.chapters
            .map { chapter -> chapterFromObject(chapter, comicBook) }
            .distinctBy { chapter -> chapter.name }
    }

    private fun chapterFromObject(chapter: VoyceMeChapter, comic: VoyceMeComic): SChapter =
        SChapter.create().apply {
            name = chapter.title
            date_upload = chapter.createdAt.toDate()
            url = "/series/${comic.slug}/${chapter.id}#comic"
        }

    override fun pageListRequest(chapter: SChapter): Request {
        val newHeaders = headersBuilder()
            .set("Referer", baseUrl + chapter.url.substringBeforeLast("/"))
            .build()

        return GET(baseUrl + chapter.url, newHeaders)
    }

    private fun pageListApiRequest(buildId: String, chapterUrl: String): Request {
        val newHeaders = headersBuilder()
            .set("Referer", baseUrl + chapterUrl)
            .build()

        val comicSlug = chapterUrl
            .substringAfter("/series/")
            .substringBefore("/")
        val chapterId = chapterUrl
            .substringAfterLast("/")
            .substringBefore("#")

        return GET("$baseUrl/_next/data/$buildId/series/$comicSlug/$chapterId.json", newHeaders)
    }

    override fun pageListParse(response: Response): List<Page> {
        // GraphQL endpoints do not have the chapter images, so we need
        // to get the buildId to fetch the chapter from NextJS static data.
        val document = response.asJsoup()
        val nextData = document.selectFirst("script#__NEXT_DATA__")!!.data()
        val nextJson = json.parseToJsonElement(nextData).jsonObject

        val buildId = nextJson["buildId"]!!.jsonPrimitive.content
        val chapterUrl = response.request.url.toString().substringAfter(baseUrl)

        val dataRequest = pageListApiRequest(buildId, chapterUrl)
        val dataResponse = client.newCall(dataRequest).execute()
        val dataJson = json.parseToJsonElement(dataResponse.body.string()).jsonObject

        val comic = dataJson["pageProps"]!!.jsonObject["series"]!!
            .let { json.decodeFromJsonElement<VoyceMeComic>(it) }

        val chapterId = response.request.url.toString()
            .substringAfterLast("/")
            .substringBefore("#")
            .toInt()
        val chapter = comic.chapters.firstOrNull { it.id == chapterId }
            ?: throw Exception(CHAPTER_DATA_NOT_FOUND)

        return chapter.images.mapIndexed { i, page ->
            Page(i, baseUrl, STATIC_URL + page.image)
        }
    }

    override fun imageUrlParse(response: Response): String = ""

    override fun imageRequest(page: Page): Request {
        val newHeaders = headersBuilder()
            .add("Accept", ACCEPT_IMAGE)
            .set("Referer", page.url)
            .build()

        return GET(page.imageUrl!!, newHeaders)
    }

    private fun String.toDate(): Long {
        return try {
            DATE_FORMATTER.parse(this)?.time ?: 0L
        } catch (e: ParseException) {
            0L
        }
    }

    private fun String.toStatus(): Int = when (this) {
        "completed" -> SManga.COMPLETED
        "ongoing" -> SManga.ONGOING
        else -> SManga.UNKNOWN
    }

    companion object {
        private const val ACCEPT_ALL = "*/*"
        private const val ACCEPT_IMAGE = "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8"

        private const val STATIC_URL = "https://dlkfxmdtxtzpb.cloudfront.net/"
        private const val GRAPHQL_URL = "https://graphql.voyce.me/v1/graphql"

        private const val POPULAR_PER_PAGE = 10

        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaTypeOrNull()

        private val DATE_FORMATTER by lazy { SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH) }

        private const val CHAPTER_DATA_NOT_FOUND = "Chapter data not found in website."
    }
}
