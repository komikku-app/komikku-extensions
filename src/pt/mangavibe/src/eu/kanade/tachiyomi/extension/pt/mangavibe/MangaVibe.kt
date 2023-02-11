package eu.kanade.tachiyomi.extension.pt.mangavibe

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
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import rx.Observable
import uy.kohesive.injekt.injectLazy
import java.text.Normalizer
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.ceil

class MangaVibe : HttpSource() {

    override val name = "MangaVibe"

    override val baseUrl = "https://mangavibe.top"

    override val lang = "pt-BR"

    override val supportsLatest = true

    override val client: OkHttpClient = network.cloudflareClient.newBuilder()
        .rateLimit(1, 2, TimeUnit.SECONDS)
        .addInterceptor(::directoryCacheIntercept)
        .build()

    override fun headersBuilder(): Headers.Builder = Headers.Builder()
        .add("Referer", "$baseUrl/")

    private val json: Json by injectLazy()

    private val directoryCache: MutableMap<String, String> = mutableMapOf()

    override fun popularMangaRequest(page: Int): Request {
        val newHeaders = headersBuilder()
            .add("Accept", ACCEPT_JSON)
            .set("Referer", "$baseUrl/mangas?Ordem=Populares")
            .add("X-Page", page.toString())
            .build()

        return GET("$baseUrl/$API_PATH/data?page=medias", newHeaders)
    }

    override fun popularMangaParse(response: Response): MangasPage {
        val result = json.decodeFromString<MangaVibePopularDto>(response.body.string())

        if (result.data.isNullOrEmpty()) {
            return MangasPage(emptyList(), hasNextPage = false)
        }

        val totalPages = ceil(result.data.size.toDouble() / ITEMS_PER_PAGE)
        val currentPage = response.request.header("X-Page")!!.toInt()

        val mangaList = result.data
            .sortedByDescending { it.views }
            .drop(ITEMS_PER_PAGE * (currentPage - 1))
            .take(ITEMS_PER_PAGE)
            .map(::popularMangaFromObject)

        return MangasPage(mangaList, hasNextPage = currentPage < totalPages)
    }

    private fun popularMangaFromObject(comic: MangaVibeComicDto): SManga = SManga.create().apply {
        title = comic.title["romaji"] ?: comic.title["english"] ?: comic.title["native"]!!
        thumbnail_url = comic.id.toThumbnailUrl()
        url = "/manga/${comic.id}/${title.toSlug()}"
    }

    override fun latestUpdatesRequest(page: Int): Request {
        val newHeaders = headersBuilder()
            .add("Accept", ACCEPT_JSON)
            .set("Referer", "$baseUrl/mangas?Ordem=Atualizados")
            .add("X-Page", page.toString())
            .build()

        return GET("$baseUrl/$API_PATH/data?page=medias&Ordem=Atualizados", newHeaders)
    }

    override fun latestUpdatesParse(response: Response): MangasPage {
        val result = json.decodeFromString<MangaVibeLatestDto>(response.body.string())

        if (result.data.isNullOrEmpty()) {
            return MangasPage(emptyList(), hasNextPage = false)
        }

        val totalPages = ceil(result.data.size.toDouble() / ITEMS_PER_PAGE)
        val currentPage = response.request.header("X-Page")!!.toInt()

        val mangaList = result.data
            .asSequence()
            .distinctBy { it.title }
            .filter { it.mediaID.isNullOrBlank().not() }
            .drop(ITEMS_PER_PAGE * (currentPage - 1))
            .take(ITEMS_PER_PAGE)
            .map(::latestMangaFromObject)
            .toList()

        return MangasPage(mangaList, hasNextPage = currentPage < totalPages)
    }

    private fun latestMangaFromObject(chapter: MangaVibeLatestChapterDto): SManga = SManga.create().apply {
        title = chapter.title!!
        thumbnail_url = chapter.mediaID!!.toInt().toThumbnailUrl()
        url = "/manga/${chapter.mediaID}/${chapter.title.toSlug()}"
    }

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        val newHeaders = headersBuilder()
            .add("Accept", ACCEPT_JSON)
            .add("X-Page", page.toString())
            .build()

        val apiUrl = "$baseUrl/$API_PATH/data".toHttpUrl().newBuilder()
            .addQueryParameter("page", "medias")
            .addQueryParameter("st", query)
            .toString()

        return GET(apiUrl, newHeaders)
    }

    override fun searchMangaParse(response: Response): MangasPage {
        val result = json.decodeFromString<MangaVibePopularDto>(response.body.string())

        if (result.data.isNullOrEmpty()) {
            return MangasPage(emptyList(), hasNextPage = false)
        }

        val searchTerm = response.request.url.queryParameter("st")!!

        val mangaList = result.data
            .filter {
                it.title.values.any { title ->
                    title?.contains(searchTerm, ignoreCase = true) ?: false
                }
            }
            .sortedByDescending { it.views }
            .map(::searchMangaFromObject)

        return MangasPage(mangaList, hasNextPage = false)
    }

    private fun searchMangaFromObject(comic: MangaVibeComicDto): SManga = popularMangaFromObject(comic)

    override fun getMangaUrl(manga: SManga): String = baseUrl + manga.url

    override fun mangaDetailsRequest(manga: SManga): Request {
        val comicId = manga.url.substringAfter("/manga/")
            .substringBefore("/")

        val newHeaders = headersBuilder()
            .add("Accept", ACCEPT_JSON)
            .set("Referer", "$baseUrl/mangas?Ordem=Populares")
            .add("X-Id", comicId)
            .build()

        return GET("$baseUrl/$API_PATH/data?page=medias", newHeaders)
    }

    override fun mangaDetailsParse(response: Response): SManga {
        val result = json.decodeFromString<MangaVibePopularDto>(response.body.string())

        if (result.data.isNullOrEmpty()) {
            throw Exception(COULD_NOT_PARSE_THE_MANGA)
        }

        val comicId = response.request.header("X-Id")!!.toInt()
        val comic = result.data.find { it.id == comicId }
            ?: throw Exception(COULD_NOT_PARSE_THE_MANGA)

        return SManga.create().apply {
            title = comic.title["romaji"] ?: comic.title["english"] ?: comic.title["native"]!!
            description = comic.description.orEmpty()
            genre = comic.genres?.joinToString(", ")
            status = comic.status?.toStatus() ?: SManga.UNKNOWN
            thumbnail_url = comic.id.toThumbnailUrl()
        }
    }

    // Chapters are available in the same url of the manga details.
    override fun chapterListRequest(manga: SManga): Request {
        val comicId = manga.url.substringAfter("/manga/")
            .substringBefore("/")

        val newHeaders = headersBuilder()
            .add("Accept", ACCEPT_JSON)
            .set("Referer", baseUrl + manga.url)
            .build()

        return GET("$baseUrl/$API_PATH/data?page=chapter&mediaID=$comicId", newHeaders)
    }

    override fun chapterListParse(response: Response): List<SChapter> {
        val result = json.decodeFromString<MangaVibeChapterListDto>(response.body.string())

        if (result.data.isNullOrEmpty()) {
            return emptyList()
        }

        return result.data
            .map(::chapterFromObject)
            .reversed()
    }

    private fun chapterFromObject(chapter: MangaVibeChapterDto): SChapter = SChapter.create().apply {
        name = "Capítulo #" + chapter.number.toString().replace(".0", "")
        chapter_number = chapter.number
        date_upload = chapter.datePublished?.toDate() ?: 0L

        val chapterUrl = "$baseUrl/chapter".toHttpUrl().newBuilder()
            .addPathSegment(chapter.mediaID.toString())
            .addPathSegment(chapter.title?.toSlug() ?: "null")
            .addPathSegment(chapter.number.toString().replace(".0", ""))
            .addQueryParameter("pgn", chapter.pages.toString())
            .toString()
        setUrlWithoutDomain(chapterUrl)
    }

    override fun fetchPageList(chapter: SChapter): Observable<List<Page>> {
        val chapterUrlPaths = chapter.url
            .removePrefix("/")
            .split("/")

        val comicId = chapterUrlPaths[1]
        val chapterNumber = chapterUrlPaths[3].substringBefore("?")
        val pageCount = chapter.url.substringAfterLast("?pgn=").toInt()

        val pages = List(pageCount) { i ->
            val pageUrl = "$CDN_URL/img/media/$comicId/chapter/$chapterNumber/${i + 1}.jpg"
            Page(i, baseUrl, pageUrl)
        }

        return Observable.just(pages)
    }

    override fun pageListParse(response: Response): List<Page> =
        throw Exception("This method should not be called!")

    override fun fetchImageUrl(page: Page): Observable<String> = Observable.just(page.imageUrl!!)

    override fun imageUrlParse(response: Response): String = ""

    override fun imageRequest(page: Page): Request {
        val newHeaders = headersBuilder()
            .add("Accept", ACCEPT_IMAGE)
            .set("Referer", page.url)
            .build()

        return GET(page.imageUrl!!, newHeaders)
    }

    private fun directoryCacheIntercept(chain: Interceptor.Chain): Response {
        if (!chain.request().url.toString().contains("data?page=medias")) {
            return chain.proceed(chain.request())
        }

        val directoryType = if (chain.request().url.queryParameter("Ordem") == null) {
            POPULAR_KEY
        } else {
            LATEST_KEY
        }
        val page = chain.request().header("X-Page")?.toInt()

        if (directoryCache.containsKey(directoryType) && page != null && page > 1) {
            val jsonContentType = "application/json; charset=UTF-8".toMediaTypeOrNull()
            val responseBody = directoryCache[directoryType]!!.toResponseBody(jsonContentType)

            return Response.Builder()
                .code(200)
                .protocol(Protocol.HTTP_1_1)
                .request(chain.request())
                .message("OK")
                .body(responseBody)
                .build()
        }

        val response = chain.proceed(chain.request())
        val responseContentType = response.body.contentType()
        val responseString = response.body.string()

        directoryCache[directoryType] = responseString

        return response.newBuilder()
            .body(responseString.toResponseBody(responseContentType))
            .build()
    }

    private fun Int.toThumbnailUrl(): String = "$CDN_URL/img/media/$this/cover/l.jpg"

    private fun String.toDate(): Long {
        return runCatching { DATE_FORMATTER.parse(substringBefore("T"))?.time }
            .getOrNull() ?: 0L
    }

    private fun String.toStatus(): Int = when (this) {
        "Em lançamento" -> SManga.ONGOING
        "Completo" -> SManga.COMPLETED
        else -> SManga.UNKNOWN
    }

    private fun String.toSlug(): String {
        return Normalizer
            .normalize(this, Normalizer.Form.NFD)
            .replace("[^\\p{ASCII}]".toRegex(), "")
            .replace("[^a-zA-Z0-9\\s]+".toRegex(), "").trim()
            .replace("\\s+".toRegex(), "-")
            .lowercase(Locale("pt", "BR"))
    }

    companion object {
        private const val API_PATH = "mangavibe/api/v1"
        private const val CDN_URL = "https://cdn.mangavibe.top"

        private const val ACCEPT_JSON = "application/json, text/plain, */*"
        private const val ACCEPT_IMAGE = "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8"

        private const val ITEMS_PER_PAGE = 24

        private const val COULD_NOT_PARSE_THE_MANGA = "Ocorreu um erro ao obter as informações."

        private const val POPULAR_KEY = "popular"
        private const val LATEST_KEY = "latest"

        private val DATE_FORMATTER by lazy { SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH) }
    }
}
