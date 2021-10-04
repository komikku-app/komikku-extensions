package eu.kanade.tachiyomi.extension.en.bilibilicomics

import eu.kanade.tachiyomi.lib.ratelimit.RateLimitInterceptor
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.asObservableSuccess
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.jsoup.Jsoup
import rx.Observable
import uy.kohesive.injekt.injectLazy
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class BilibiliComics : HttpSource() {

    override val name = "BILIBILI COMICS"

    override val baseUrl = "https://www.bilibilicomics.com"

    override val lang = "en"

    override val supportsLatest = true

    override val client: OkHttpClient = network.cloudflareClient.newBuilder()
        .addInterceptor(RateLimitInterceptor(1, 1, TimeUnit.SECONDS))
        .build()

    override fun headersBuilder(): Headers.Builder = Headers.Builder()
        .add("Accept", ACCEPT_JSON)
        .add("Origin", baseUrl)
        .add("Referer", "$baseUrl/")

    private val json: Json by injectLazy()

    override fun popularMangaRequest(page: Int): Request {
        val requestPayload = buildJsonObject {
            put("area_id", -1)
            put("is_finish", -1)
            put("is_free", 1)
            put("order", 0)
            put("page_num", page)
            put("page_size", POPULAR_PER_PAGE)
            put("style_id", -1)
            put("style_prefer", "[]")
        }
        val requestBody = requestPayload.toString().toRequestBody(JSON_MEDIA_TYPE)

        val newHeaders = headersBuilder()
            .add("Content-Length", requestBody.contentLength().toString())
            .add("Content-Type", requestBody.contentType().toString())
            .build()

        return POST(
            "$baseUrl/$BASE_API_ENDPOINT/ClassPage?device=pc&platform=web",
            headers = newHeaders,
            body = requestBody
        )
    }

    override fun popularMangaParse(response: Response): MangasPage {
        val result = json.decodeFromString<BilibiliResultDto<List<BilibiliComicDto>>>(response.body!!.string())

        if (result.code != 0) {
            return MangasPage(emptyList(), hasNextPage = false)
        }

        val comicList = result.data!!.map(::popularMangaFromObject)
        val hasNextPage = comicList.size == POPULAR_PER_PAGE

        return MangasPage(comicList, hasNextPage)
    }

    private fun popularMangaFromObject(comic: BilibiliComicDto): SManga = SManga.create().apply {
        title = comic.title
        thumbnail_url = comic.verticalCover
        url = "/detail/mc${comic.seasonId}"
    }

    override fun latestUpdatesRequest(page: Int): Request {
        val requestPayload = buildJsonObject {
            put("area_id", -1)
            put("is_finish", -1)
            put("is_free", 1)
            put("order", 1)
            put("page_num", page)
            put("page_size", POPULAR_PER_PAGE)
            put("style_id", -1)
            put("style_prefer", "[]")
        }
        val requestBody = requestPayload.toString().toRequestBody(JSON_MEDIA_TYPE)

        val newHeaders = headersBuilder()
            .add("Content-Length", requestBody.contentLength().toString())
            .add("Content-Type", requestBody.contentType().toString())
            .build()

        return POST(
            "$baseUrl/$BASE_API_ENDPOINT/ClassPage?device=pc&platform=web",
            headers = newHeaders,
            body = requestBody
        )
    }

    override fun latestUpdatesParse(response: Response): MangasPage {
        val result = json.decodeFromString<BilibiliResultDto<List<BilibiliComicDto>>>(response.body!!.string())

        if (result.code != 0) {
            return MangasPage(emptyList(), hasNextPage = false)
        }

        val comicList = result.data!!.map(::latestMangaFromObject)
        val hasNextPage = comicList.size == POPULAR_PER_PAGE

        return MangasPage(comicList, hasNextPage)
    }

    private fun latestMangaFromObject(comic: BilibiliComicDto): SManga = SManga.create().apply {
        title = comic.title
        thumbnail_url = comic.verticalCover
        url = "/detail/mc${comic.seasonId}"
    }

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        if (query.startsWith(PREFIX_ID_SEARCH) && query.matches(ID_SEARCH_PATTERN)) {
            val comicId = query
                .removePrefix(PREFIX_ID_SEARCH)
                .removePrefix("mc")
            return mangaDetailsApiRequest("/detail/mc$comicId")
        }

        val order = filters.filterIsInstance<SortFilter>()
            .firstOrNull()?.state ?: 0

        val status = filters.filterIsInstance<StatusFilter>()
            .firstOrNull()?.state?.minus(1) ?: -1

        val styleId = filters.filterIsInstance<GenreFilter>()
            .firstOrNull()?.selected?.id ?: -1

        val pageSize = if (query.isBlank()) POPULAR_PER_PAGE else SEARCH_PER_PAGE

        val jsonPayload = buildJsonObject {
            put("area_id", -1)
            put("is_finish", status)
            put("is_free", 1)
            put("order", order)
            put("page_num", page)
            put("page_size", pageSize)
            put("style_id", styleId)
            put("style_prefer", "[]")

            if (query.isNotBlank()) {
                put("need_shield_prefer", true)
                put("key_word", query)
            }
        }
        val requestBody = jsonPayload.toString().toRequestBody(JSON_MEDIA_TYPE)

        val refererUrl = if (query.isBlank()) "$baseUrl/genre" else
            "$baseUrl/search".toHttpUrl().newBuilder()
                .addQueryParameter("keyword", query)
                .toString()
        val newHeaders = headersBuilder()
            .add("Content-Length", requestBody.contentLength().toString())
            .add("Content-Type", requestBody.contentType().toString())
            .set("Referer", refererUrl)
            .build()

        val apiPath = if (query.isBlank()) "ClassPage" else "Search"

        return POST(
            "$baseUrl/$BASE_API_ENDPOINT/$apiPath?device=pc&platform=web",
            headers = newHeaders,
            body = requestBody
        )
    }

    override fun searchMangaParse(response: Response): MangasPage {
        if (response.request.url.toString().contains("ComicDetail")) {
            val comic = mangaDetailsParse(response)
            return MangasPage(listOf(comic), hasNextPage = false)
        }

        if (response.request.url.toString().contains("ClassPage")) {
            val result = json.decodeFromString<BilibiliResultDto<List<BilibiliComicDto>>>(response.body!!.string())

            if (result.code != 0) {
                return MangasPage(emptyList(), hasNextPage = false)
            }

            val comicList = result.data!!.map(::searchMangaFromObject)
            val hasNextPage = comicList.size == POPULAR_PER_PAGE

            return MangasPage(comicList, hasNextPage)
        }

        val result = json.decodeFromString<BilibiliResultDto<BilibiliSearchDto>>(response.body!!.string())

        if (result.code != 0) {
            return MangasPage(emptyList(), hasNextPage = false)
        }

        val comicList = result.data!!.list.map(::searchMangaFromObject)
        val hasNextPage = comicList.size == SEARCH_PER_PAGE

        return MangasPage(comicList, hasNextPage)
    }

    private fun searchMangaFromObject(comic: BilibiliComicDto): SManga = SManga.create().apply {
        title = Jsoup.parse(comic.title).text()
        thumbnail_url = comic.verticalCover

        val comicId = if (comic.id == 0) comic.seasonId else comic.id
        url = "/detail/mc$comicId"
    }

    // Workaround to allow "Open in browser" use the real URL.
    override fun fetchMangaDetails(manga: SManga): Observable<SManga> {
        return client.newCall(mangaDetailsApiRequest(manga.url))
            .asObservableSuccess()
            .map { response ->
                mangaDetailsParse(response).apply { initialized = true }
            }
    }

    private fun mangaDetailsApiRequest(mangaUrl: String): Request {
        val comicId = mangaUrl.substringAfterLast("/mc").toInt()

        val jsonPayload = buildJsonObject { put("comic_id", comicId) }
        val requestBody = jsonPayload.toString().toRequestBody(JSON_MEDIA_TYPE)

        val newHeaders = headersBuilder()
            .add("Content-Length", requestBody.contentLength().toString())
            .add("Content-Type", requestBody.contentType().toString())
            .set("Referer", baseUrl + mangaUrl)
            .build()

        return POST(
            "$baseUrl/$BASE_API_ENDPOINT/ComicDetail?device=pc&platform=web",
            headers = newHeaders,
            body = requestBody
        )
    }

    override fun mangaDetailsParse(response: Response): SManga = SManga.create().apply {
        val result = json.decodeFromString<BilibiliResultDto<BilibiliComicDto>>(response.body!!.string())
        val comic = result.data!!

        title = comic.title
        author = comic.authorName.joinToString()
        status = if (comic.isFinish == 1) SManga.COMPLETED else SManga.ONGOING
        genre = comic.styles.joinToString()
        description = comic.classicLines
        thumbnail_url = comic.verticalCover
        url = "/detail/mc" + comic.id
    }

    // Chapters are available in the same url of the manga details.
    override fun chapterListRequest(manga: SManga): Request = mangaDetailsApiRequest(manga.url)

    override fun chapterListParse(response: Response): List<SChapter> {
        val result = json.decodeFromString<BilibiliResultDto<BilibiliComicDto>>(response.body!!.string())

        if (result.code != 0)
            return emptyList()

        return result.data!!.episodeList
            .filter { episode -> episode.isLocked.not() }
            .map { ep -> chapterFromObject(ep, result.data.id) }
    }

    private fun chapterFromObject(episode: BilibiliEpisodeDto, comicId: Int): SChapter = SChapter.create().apply {
        name = "Ep. " + episode.order.toString().removeSuffix(".0") +
            " - " + episode.title
        chapter_number = episode.order
        scanlator = this@BilibiliComics.name
        date_upload = episode.publicationTime.substringBefore("T").toDate()
        url = "/mc$comicId/${episode.id}"
    }

    override fun pageListRequest(chapter: SChapter): Request {
        val chapterId = chapter.url.substringAfterLast("/").toInt()

        val jsonPayload = buildJsonObject { put("ep_id", chapterId) }
        val requestBody = jsonPayload.toString().toRequestBody(JSON_MEDIA_TYPE)

        val newHeaders = headersBuilder()
            .add("Content-Length", requestBody.contentLength().toString())
            .add("Content-Type", requestBody.contentType().toString())
            .set("Referer", baseUrl + chapter.url)
            .build()

        return POST(
            "$baseUrl/$BASE_API_ENDPOINT/GetImageIndex?device=pc&platform=web",
            headers = newHeaders,
            body = requestBody
        )
    }

    override fun pageListParse(response: Response): List<Page> {
        val result = json.decodeFromString<BilibiliResultDto<BilibiliReader>>(response.body!!.string())

        if (result.code != 0) {
            return emptyList()
        }

        return result.data!!.images
            .mapIndexed { i, page -> Page(i, page.path, "") }
    }

    override fun imageUrlRequest(page: Page): Request {
        val jsonPayload = buildJsonObject {
            put("urls", buildJsonArray { add(page.url) }.toString())
        }
        val requestBody = jsonPayload.toString().toRequestBody(JSON_MEDIA_TYPE)

        val newHeaders = headersBuilder()
            .add("Content-Length", requestBody.contentLength().toString())
            .add("Content-Type", requestBody.contentType().toString())
            .build()

        return POST(
            "$baseUrl/$BASE_API_ENDPOINT/ImageToken?device=pc&platform=web",
            headers = newHeaders,
            body = requestBody
        )
    }

    override fun imageUrlParse(response: Response): String {
        val result = json.decodeFromString<BilibiliResultDto<List<BilibiliPageDto>>>(response.body!!.string())
        val page = result.data!![0]

        return "${page.url}?token=${page.token}"
    }

    private data class Genre(val name: String, val id: Int) {
        override fun toString(): String = name
    }

    private class GenreFilter(genres: Array<Genre>) : Filter.Select<Genre>("Genre", genres) {
        val selected: Genre
            get() = values[state]
    }

    private class SortFilter(options: Array<String>) : Filter.Select<String>("Sort by", options)
    private class StatusFilter(statuses: Array<String>) : Filter.Select<String>("Status", statuses)

    private fun getAllGenres(): Array<Genre> = arrayOf(
        Genre("All", -1),
        Genre("Action", 19),
        Genre("Adventure", 22),
        Genre("BL", 3),
        Genre("Comedy", 14),
        Genre("Eastern", 30),
        Genre("Fantasy", 11),
        Genre("GL", 16),
        Genre("Harem", 15),
        Genre("Historical", 12),
        Genre("Horror", 23),
        Genre("Mistery", 17),
        Genre("Romance", 13),
        Genre("Slice of Life", 21),
        Genre("Suspense", 41),
        Genre("Teen", 20)
    )

    private fun getAllSortOptions(): Array<String> = arrayOf("Popular", "Updated")

    private fun getAllStatus(): Array<String> = arrayOf("All", "Ongoing", "Completed")

    override fun getFilterList(): FilterList = FilterList(
        StatusFilter(getAllStatus()),
        SortFilter(getAllSortOptions()),
        GenreFilter(getAllGenres())
    )

    private fun String.toDate(): Long {
        return try {
            DATE_FORMATTER.parse(this)?.time ?: 0L
        } catch (e: ParseException) {
            0L
        }
    }

    companion object {
        private const val BASE_API_ENDPOINT = "twirp/comic.v1.Comic"

        private const val ACCEPT_JSON = "application/json, text/plain, */*"

        private val JSON_MEDIA_TYPE = "application/json;charset=UTF-8".toMediaType()

        private const val POPULAR_PER_PAGE = 18
        private const val SEARCH_PER_PAGE = 9

        const val PREFIX_ID_SEARCH = "id:"
        private val ID_SEARCH_PATTERN = "^id:(mc)?(\\d+)$".toRegex()

        private val DATE_FORMATTER by lazy { SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH) }
    }
}
