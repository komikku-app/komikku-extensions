package eu.kanade.tachiyomi.extension.pt.argosscan

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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.add
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import rx.Observable
import uy.kohesive.injekt.injectLazy
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class ArgosScan : HttpSource() {

    // Website changed from Madara to a custom CMS.
    override val versionId = 2

    override val name = "Argos Scan"

    override val baseUrl = "http://argosscan.com"

    override val lang = "pt-BR"

    override val supportsLatest = true

    override val client: OkHttpClient = network.cloudflareClient.newBuilder()
        .addInterceptor(RateLimitInterceptor(1, 2, TimeUnit.SECONDS))
        .build()

    private val json: Json by injectLazy()

    private fun genericMangaFromObject(project: ArgosProjectDto): SManga = SManga.create().apply {
        title = project.name!!
        url = "/obras/${project.id}"
        thumbnail_url = "$baseUrl/images/${project.id}/${project.cover!!}"
    }

    override fun popularMangaRequest(page: Int): Request {
        val payload = buildPopularQueryPayload(page)

        val body = payload.toString().toRequestBody(JSON_MEDIA_TYPE)

        val newHeaders = headersBuilder()
            .add("Content-Length", body.contentLength().toString())
            .add("Content-Type", body.contentType().toString())
            .build()

        return POST(GRAPHQL_URL, newHeaders, body)
    }

    override fun popularMangaParse(response: Response): MangasPage {
        val result = json.parseToJsonElement(response.body!!.string()).jsonObject

        val projectList = result["data"]!!.jsonObject["getProjects"]!!
            .let { json.decodeFromJsonElement<ArgosProjectListDto>(it) }

        val mangaList = projectList.projects
            .map(::genericMangaFromObject)

        val hasNextPage = projectList.currentPage < projectList.totalPages

        return MangasPage(mangaList, hasNextPage)
    }

    override fun latestUpdatesRequest(page: Int): Request {
        val payload = buildLatestQueryPayload(page)

        val body = payload.toString().toRequestBody(JSON_MEDIA_TYPE)

        val newHeaders = headersBuilder()
            .add("Content-Length", body.contentLength().toString())
            .add("Content-Type", body.contentType().toString())
            .build()

        return POST(GRAPHQL_URL, newHeaders, body)
    }

    override fun latestUpdatesParse(response: Response): MangasPage = popularMangaParse(response)

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        val payload = buildSearchQueryPayload(query, page)

        val body = payload.toString().toRequestBody(JSON_MEDIA_TYPE)

        val newHeaders = headersBuilder()
            .add("Content-Length", body.contentLength().toString())
            .add("Content-Type", body.contentType().toString())
            .build()

        return POST(GRAPHQL_URL, newHeaders, body)
    }

    override fun searchMangaParse(response: Response): MangasPage = popularMangaParse(response)

    // Workaround to allow "Open in browser" use the real URL.
    override fun fetchMangaDetails(manga: SManga): Observable<SManga> {
        return client.newCall(mangaDetailsApiRequest(manga))
            .asObservableSuccess()
            .map { response ->
                mangaDetailsParse(response).apply { initialized = true }
            }
    }

    private fun mangaDetailsApiRequest(manga: SManga): Request {
        val mangaId = manga.url.substringAfter("obras/").toInt()

        val payload = buildMangaDetailsQueryPayload(mangaId)

        val body = payload.toString().toRequestBody(JSON_MEDIA_TYPE)

        val newHeaders = headersBuilder()
            .add("Content-Length", body.contentLength().toString())
            .add("Content-Type", body.contentType().toString())
            .build()

        return POST(GRAPHQL_URL, newHeaders, body)
    }

    override fun mangaDetailsParse(response: Response): SManga = SManga.create().apply {
        val result = json.parseToJsonElement(response.body!!.string()).jsonObject
        val project = result["data"]!!.jsonObject["project"]!!.jsonObject
            .let { json.decodeFromJsonElement<ArgosProjectDto>(it) }

        title = project.name!!
        thumbnail_url = "$baseUrl/images/${project.id}/${project.cover!!}"
        description = project.description.orEmpty()
        author = project.authors.orEmpty().joinToString(", ")
        status = SManga.ONGOING
        genre = project.tags.orEmpty().joinToString(", ") { it.name }
    }

    override fun chapterListRequest(manga: SManga): Request = mangaDetailsApiRequest(manga)

    override fun chapterListParse(response: Response): List<SChapter> {
        val result = json.parseToJsonElement(response.body!!.string()).jsonObject
        val project = result["data"]!!.jsonObject["project"]!!.jsonObject
            .let { json.decodeFromJsonElement<ArgosProjectDto>(it) }

        return project.chapters.map(::chapterFromObject)
    }

    private fun chapterFromObject(chapter: ArgosChapterDto): SChapter = SChapter.create().apply {
        name = chapter.title!!
        chapter_number = chapter.number?.toFloat() ?: -1f
        scanlator = this@ArgosScan.name
        date_upload = chapter.createAt!!.toDate()
        url = "/leitor/${chapter.id}"
    }

    override fun pageListRequest(chapter: SChapter): Request {
        val chapterId = chapter.url.substringAfter("leitor/")

        val payload = buildPagesQueryPayload(chapterId)

        val body = payload.toString().toRequestBody(JSON_MEDIA_TYPE)

        val newHeaders = headersBuilder()
            .add("Content-Length", body.contentLength().toString())
            .add("Content-Type", body.contentType().toString())
            .build()

        return POST(GRAPHQL_URL, newHeaders, body)
    }

    override fun pageListParse(response: Response): List<Page> {
        val result = json.parseToJsonElement(response.body!!.string()).jsonObject

        val chapterDto = result["data"]!!.jsonObject["getChapters"]!!.jsonObject["chapters"]!!.jsonArray[0]
            .let { json.decodeFromJsonElement<ArgosChapterDto>(it) }

        val referer = "$baseUrl/leitor/${chapterDto.id}"

        return chapterDto.images.orEmpty().mapIndexed { i, page ->
            Page(i, referer, "$baseUrl/images/${chapterDto.project!!.id}/$page")
        }
    }

    override fun imageUrlParse(response: Response): String = ""

    override fun imageRequest(page: Page): Request {
        val newHeaders = headersBuilder()
            .set("Referer", page.url)
            .build()

        return GET(page.imageUrl!!, newHeaders)
    }

    private fun String.toDate(): Long {
        return runCatching { DATE_PARSER.parse(this)?.time }
            .getOrNull() ?: 0L
    }

    companion object {
        private const val GRAPHQL_URL = "https://argosscan.com/graphql"

        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaTypeOrNull()

        private val DATE_PARSER by lazy {
            SimpleDateFormat("yyyy-MM-dd", Locale("pt", "BR"))
        }
    }
}
