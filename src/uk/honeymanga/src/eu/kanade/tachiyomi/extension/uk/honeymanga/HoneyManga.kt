package eu.kanade.tachiyomi.extension.uk.honeymanga

import eu.kanade.tachiyomi.extension.uk.honeymanga.dtos.HoneyMangaChapterDto
import eu.kanade.tachiyomi.extension.uk.honeymanga.dtos.HoneyMangaChapterPagesDto
import eu.kanade.tachiyomi.extension.uk.honeymanga.dtos.HoneyMangaDto
import eu.kanade.tachiyomi.extension.uk.honeymanga.dtos.HoneyMangaResponseDto
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.interceptor.rateLimitHost
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import rx.Observable
import java.text.SimpleDateFormat
import java.util.Locale

class HoneyManga : HttpSource() {

    override val name = "HoneyManga"
    override val baseUrl = "https://honey-manga.com.ua"
    override val lang = "uk"
    override val supportsLatest = true

    override fun headersBuilder(): Headers.Builder = Headers.Builder()
        .add("Origin", baseUrl)
        .add("Referer", baseUrl)
        .add("User-Agent", userAgent)

    override val client: OkHttpClient = network.client.newBuilder()
        .rateLimitHost(API_URL.toHttpUrl(), 10)
        .build()

    // ----- requests -----

    override fun popularMangaRequest(page: Int): Request {
        return POST(
            "$API_URL/v2/manga/cursor-list",
            headers,
            makeHoneyMangaRequestBody(page, "likes"),
        )
    }

    override fun latestUpdatesRequest(page: Int): Request {
        return POST(
            "$API_URL/v2/manga/cursor-list",
            headers,
            makeHoneyMangaRequestBody(page, "lastUpdated"),
        )
    }

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        if (query.length >= 3) {
            return GET(
                "$SEARCH_API_URL/api/v1/title/search-matching".toHttpUrl()
                    .newBuilder()
                    .addQueryParameter("query", query)
                    .toString(),
                headers,
            )
        } else {
            throw UnsupportedOperationException("Запит має містити щонайменше 3 символи / The query must contain at least 3 characters")
        }
    }

    override fun chapterListRequest(manga: SManga): Request {
        val url = "$API_URL/chapter".toHttpUrl().newBuilder()
            .addQueryParameter("mangaId", manga.url.substringAfterLast('/'))
            .addQueryParameter("sortOrder", "DESC")
            .addQueryParameter("page", "1")
            .addQueryParameter("pageSize", "10000") // most likely there will not be any more pageSize
            .build().toString()
        return GET(url, headers)
    }

    override fun mangaDetailsRequest(manga: SManga): Request = mangaDetailsRequest(manga.url)

    override fun pageListRequest(chapter: SChapter): Request {
        val chapterId = chapter.url.substringBeforeLast('/').substringAfterLast('/')
        val url = "$API_URL/chapter/frames/$chapterId"
        return GET(url, headers)
    }

    // ----- parse -----

    override fun popularMangaParse(response: Response): MangasPage = parseAsMangaResponseDto(response)

    override fun latestUpdatesParse(response: Response): MangasPage = parseAsMangaResponseDto(response)

    override fun searchMangaParse(response: Response): MangasPage = parseAsMangaResponseArray(response)

    override fun mangaDetailsParse(response: Response): SManga {
        return makeSManga(response.asClass())
    }

    override fun chapterListParse(response: Response): List<SChapter> {
        val result = response.asClass<List<HoneyMangaChapterDto>>()
        return result.map {
            SChapter.create().apply {
                url = "$baseUrl/read/${it.id}/${it.mangaId}"
                name = "Vol. ${it.volume} Ch. ${it.chapterNum}" + (if (it.subChapterNum == 0) "" else ".${it.subChapterNum}")
                date_upload = parseDate(it.lastUpdated)
            }
        }
    }

    override fun pageListParse(response: Response): List<Page> {
        return response.asClass<HoneyMangaChapterPagesDto>().resourceIds.toList().map {
            Page(it.first.toInt(), "", "$IMAGE_STORAGE_URL/${it.second.jsonPrimitive.content}")
        }
    }

    override fun imageUrlParse(response: Response): String = ""

    override fun imageRequest(page: Page): Request = GET(page.imageUrl!!, headers)

    override fun getMangaUrl(manga: SManga): String = manga.url

    override fun getChapterUrl(chapter: SChapter): String = chapter.url

    override fun fetchImageUrl(page: Page): Observable<String> = Observable.just(page.imageUrl!!)

    // ----- private methods -----

    private fun mangaDetailsRequest(mangaUrl: String): Request {
        val mangaId = mangaUrl.substringAfterLast('/')
        val url = "https://data.api.honey-manga.com.ua/manga/$mangaId"
        return GET(url, headers)
    }

    private fun parseAsMangaResponseDto(response: Response): MangasPage {
        val mangaList = response.asClass<HoneyMangaResponseDto>().data
        return makeMangasPage(mangaList)
    }

    private fun parseAsMangaResponseArray(response: Response): MangasPage {
        val mangaList = response.asClass<List<HoneyMangaDto>>()
        return makeMangasPage(mangaList)
    }

    private fun makeMangasPage(mangaList: List<HoneyMangaDto>): MangasPage {
        return MangasPage(
            makeSMangaList(mangaList),
            mangaList.size == DEFAULT_PAGE_SIZE,
        )
    }

    private fun makeSMangaList(mangaList: List<HoneyMangaDto>): List<SManga> {
        return mangaList.map(::makeSManga)
    }

    private fun makeSManga(mangaDto: HoneyMangaDto): SManga {
        return SManga.create().apply {
            title = mangaDto.title
            thumbnail_url =
                "https://manga-storage.fra1.digitaloceanspaces.com/public-resources/${mangaDto.posterId}"
            url = "$baseUrl/book/${mangaDto.id}"
            description = mangaDto.description
            genre = mangaDto.type
        }
    }

    companion object {

        // utils and constants

        private const val API_URL = "https://data.api.honey-manga.com.ua"

        private const val SEARCH_API_URL = "https://search.api.honey-manga.com.ua"

        private const val IMAGE_STORAGE_URL = "https://manga-storage.fra1.digitaloceanspaces.com/public-resources"

        private val userAgent = System.getProperty("http.agent")!!

        private val DATE_FORMATTER by lazy {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ROOT)
        }

        private fun parseDate(dateString: String): Long = runCatching { DATE_FORMATTER.parse(dateString)?.time }.getOrNull() ?: 0L

        private const val DEFAULT_PAGE_SIZE = 30

        private val json by lazy {
            Json {
                isLenient = true
                ignoreUnknownKeys = true
            }
        }

        private inline fun <reified R : Any> Response.asClass() = use { json.decodeFromString<R>(body.string()) }

        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaTypeOrNull()

        private fun makeHoneyMangaRequestBody(page: Int, sortBy: String) = buildJsonObject {
            put("page", page)
            put("pageSize", DEFAULT_PAGE_SIZE)
            putJsonObject("sort") {
                put("sortBy", sortBy)
                put("sortOrder", "DESC")
            }
        }.toString().toRequestBody(JSON_MEDIA_TYPE)
    }
}
