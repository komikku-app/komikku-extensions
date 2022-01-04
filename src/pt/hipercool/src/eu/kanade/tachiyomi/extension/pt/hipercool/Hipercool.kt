package eu.kanade.tachiyomi.extension.pt.hipercool

import eu.kanade.tachiyomi.lib.ratelimit.SpecificHostRateLimitInterceptor
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
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import rx.Observable
import uy.kohesive.injekt.injectLazy
import java.text.SimpleDateFormat
import java.util.Locale

class Hipercool : HttpSource() {

    // Hardcode the id because the language wasn't specific.
    override val id: Long = 5898568703656160

    override val name = "HipercooL"

    override val baseUrl = "https://hiper.cool"

    override val lang = "pt-BR"

    override val supportsLatest = true

    override val client: OkHttpClient = network.cloudflareClient.newBuilder()
        .addInterceptor(SpecificHostRateLimitInterceptor(baseUrl.toHttpUrl(), 1, 2))
        .addInterceptor(SpecificHostRateLimitInterceptor(STATIC_URL.toHttpUrl(), 1, 1))
        .build()

    override fun headersBuilder(): Headers.Builder = Headers.Builder()
        .add("User-Agent", USER_AGENT)
        .add("Referer", baseUrl)
        .add("X-Requested-With", "XMLHttpRequest")

    private val json: Json by injectLazy()

    private fun genericMangaListParse(response: Response): MangasPage {
        val chapters = response.parseAs<List<HipercoolChapterDto>>()

        if (chapters.isEmpty())
            return MangasPage(emptyList(), false)

        val mangaList = chapters
            .distinctBy { it.book!!.title }
            .map(::genericMangaFromObject)

        val hasNextPage = chapters.size == DEFAULT_COUNT

        return MangasPage(mangaList, hasNextPage)
    }

    private fun genericMangaFromObject(chapter: HipercoolChapterDto): SManga = SManga.create().apply {
        title = chapter.book!!.title
        thumbnail_url = chapter.book.slug.toThumbnailUrl(chapter.book.revision)
        url = "/books/" + chapter.book.slug
    }

    // The source does not have popular mangas, so use latest instead.
    override fun popularMangaRequest(page: Int): Request = latestUpdatesRequest(page)

    override fun popularMangaParse(response: Response): MangasPage = genericMangaListParse(response)

    override fun latestUpdatesRequest(page: Int): Request {
        val start = (page - 1) * DEFAULT_COUNT
        return GET("$baseUrl/api/books/chapters?start=$start&count=$DEFAULT_COUNT", headers)
    }

    override fun latestUpdatesParse(response: Response): MangasPage = genericMangaListParse(response)

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        val searchPayload = HipercoolSearchDto(
            start = (page - 1) * DEFAULT_COUNT,
            count = DEFAULT_COUNT,
            text = query,
            type = "text"
        )

        val body = json.encodeToString(searchPayload).toRequestBody(JSON_MEDIA_TYPE)

        return POST("$baseUrl/api/books/chapters/search", headers, body)
    }

    override fun searchMangaParse(response: Response): MangasPage = genericMangaListParse(response)

    // Workaround to allow "Open in browser" use the real URL.
    override fun fetchMangaDetails(manga: SManga): Observable<SManga> {
        return client.newCall(mangaDetailsApiRequest(manga))
            .asObservableSuccess()
            .map { response ->
                mangaDetailsParse(response).apply { initialized = true }
            }
    }

    private fun mangaDetailsApiRequest(manga: SManga): Request {
        val slug = manga.url.substringAfterLast("/")

        return GET("$baseUrl/api/books/$slug", headers)
    }

    override fun mangaDetailsParse(response: Response): SManga = SManga.create().apply {
        val book = response.parseAs<HipercoolBookDto>()

        title = book.title
        thumbnail_url = book.slug.toThumbnailUrl(book.revision)
        description = book.synopsis.orEmpty()
        artist = book.fixedTags["artista"].orEmpty().joinToString("; ")
        author = book.fixedTags["autor"].orEmpty().joinToString("; ")
        genre = book.fixedTags["tags"].orEmpty().joinToString()
    }

    // Chapters are available in the same url of the manga details.
    override fun chapterListRequest(manga: SManga): Request = mangaDetailsApiRequest(manga)

    override fun chapterListParse(response: Response): List<SChapter> {
        val book = response.parseAs<HipercoolBookDto>()

        if (book.chapters is JsonPrimitive)
            return emptyList()

        return json.decodeFromString<List<HipercoolChapterDto>>(book.chapters.toString())
            .map { chapterListItemParse(book, it) }
            .reversed()
    }

    private fun chapterListItemParse(book: HipercoolBookDto, chapter: HipercoolChapterDto): SChapter =
        SChapter.create().apply {
            name = "Cap. " + chapter.title
            chapter_number = chapter.title.toFloatOrNull() ?: -1f
            date_upload = chapter.publishedAt.toDate()
            scanlator = book.fixedTags["tradutor"]?.joinToString(" & ")

            val fullUrl = "$baseUrl/books".toHttpUrl().newBuilder()
                .addPathSegment(book.slug)
                .addPathSegment(chapter.slug)
                .addQueryParameter("images", chapter.images.toString())
                .addQueryParameter("revision", book.revision.toString())
                .toString()

            setUrlWithoutDomain(fullUrl)
        }

    override fun fetchPageList(chapter: SChapter): Observable<List<Page>> {
        val chapterUrl = (baseUrl + chapter.url).toHttpUrlOrNull()!!

        val bookSlug = chapterUrl.pathSegments[1]
        val chapterSlug = chapterUrl.pathSegments[2]
        val images = chapterUrl.queryParameter("images")!!.toInt()
        val revision = chapterUrl.queryParameter("revision")!!

        val pages = List(images) { i ->
            val imageUrl = "$STATIC_URL/books".toHttpUrl().newBuilder()
                .addPathSegment(bookSlug)
                .addPathSegment(chapterSlug)
                .addPathSegment("$bookSlug-chapter-$chapterSlug-page-${i + 1}.jpg")
                .addQueryParameter("revision", revision)
                .toString()

            Page(i, chapter.url, imageUrl)
        }

        return Observable.just(pages)
    }

    override fun pageListParse(response: Response): List<Page> =
        throw Exception("This method should not be called!")

    override fun fetchImageUrl(page: Page): Observable<String> = Observable.just(page.imageUrl!!)

    override fun imageUrlParse(response: Response): String = ""

    override fun imageRequest(page: Page): Request {
        val newHeaders = headersBuilder()
            .set("Referer", baseUrl + page.url)
            .build()

        return GET(page.imageUrl!!, newHeaders)
    }

    private inline fun <reified T> Response.parseAs(): T = use {
        json.decodeFromString(it.body?.string().orEmpty())
    }

    private fun String.toDate(): Long {
        return runCatching { DATE_FORMATTER.parse(this)?.time }
            .getOrNull() ?: 0L
    }

    private fun String.toThumbnailUrl(revision: Int): String =
        "$STATIC_URL/books".toHttpUrlOrNull()!!.newBuilder()
            .addPathSegment(this)
            .addPathSegment("$this-cover.jpg")
            .addQueryParameter("revision", revision.toString())
            .toString()

    companion object {
        private const val STATIC_URL = "https://static.hiper.cool"

        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/96.0.4664.110 Safari/537.36"

        private const val DEFAULT_COUNT = 40

        private val DATE_FORMATTER by lazy {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ENGLISH)
        }
    }
}
