package eu.kanade.tachiyomi.extension.en.graphitecomics

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
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
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
import uy.kohesive.injekt.injectLazy
import java.lang.UnsupportedOperationException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class GraphiteComics : HttpSource() {

    override val name = "Graphite Comics"

    override val baseUrl = "http://graphitecomics.com"

    override val lang = "en"

    override val supportsLatest = false

    override val client: OkHttpClient = network.cloudflareClient.newBuilder()
        .addInterceptor(RateLimitInterceptor(2, 1, TimeUnit.SECONDS))
        .build()

    private val json: Json by injectLazy()

    override fun headersBuilder(): Headers.Builder = super.headersBuilder()
        .add("Accept", ACCEPT_ALL)
        .add("Origin", baseUrl)
        .add("Referer", "$baseUrl/")

    private fun genericComicBookFromObject(comic: GraphiteComic): SManga =
        SManga.create().apply {
            title = comic.name
            url = "/title/${comic.publisherSlug}/${comic.slug}"
            thumbnail_url = comic.logo?.url
        }

    override fun popularMangaRequest(page: Int): Request {
        val query = buildQuery {
            """
            query (%limit: Int) {
                topTitles(limit: %limit) {
                    name
                    slug
                    publisher_slug
                    logo { url }
                }
            }
            """.trimIndent()
        }

        val payload = buildJsonObject {
            put("query", query)
            putJsonObject("variables") {
                put("limit", POPULAR_LIMIT)
            }
        }

        val body = payload.toString().toRequestBody(JSON_MEDIA_TYPE)

        val newHeaders = headersBuilder()
            .set("Accept", ACCEPT_JSON)
            .add("Content-Length", body.contentLength().toString())
            .add("Content-Type", body.contentType().toString())
            .build()

        return POST(GRAPHQL_URL, newHeaders, body)
    }

    override fun popularMangaParse(response: Response): MangasPage {
        val result = json.parseToJsonElement(response.body!!.string()).jsonObject

        val comicList = result["data"]!!.jsonObject["topTitles"]!!
            .let { json.decodeFromJsonElement<List<GraphiteComic>>(it) }
            .map(::genericComicBookFromObject)

        return MangasPage(comicList, hasNextPage = false)
    }

    override fun latestUpdatesRequest(page: Int): Request = throw UnsupportedOperationException("Not used")

    override fun latestUpdatesParse(response: Response): MangasPage = throw UnsupportedOperationException("Not used")

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        val searchUrl = "$baseUrl/api/title/search".toHttpUrl().newBuilder()
            .addPathSegment(query)
            .addQueryParameter("limit", POPULAR_LIMIT.toString())
            .toString()

        val refererUrl = "$baseUrl/s".toHttpUrl().newBuilder()
            .addPathSegment(query)
            .toString()

        val newHeaders = headersBuilder()
            .set("Accept", ACCEPT_JSON)
            .set("Referer", refererUrl)
            .build()

        return GET(searchUrl, newHeaders)
    }

    override fun searchMangaParse(response: Response): MangasPage {
        val comicList = json.decodeFromString<List<GraphiteComic>>(response.body!!.string())
            .map(::genericComicBookFromObject)

        return MangasPage(comicList, hasNextPage = false)
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
        val newHeaders = headersBuilder()
            .set("Accept", ACCEPT_JSON)
            .set("Referer", baseUrl + manga.url)
            .build()

        val publisherSlug = manga.url
            .substringAfter("/title/")
            .substringBefore("/")

        val comicSlug = manga.url.substringAfterLast("/")

        val apiUrl = "$baseUrl/api/title/find/null/".toHttpUrl().newBuilder()
            .addQueryParameter("publisher_slug", publisherSlug)
            .addQueryParameter("slug", comicSlug)
            .toString()

        return GET(apiUrl, newHeaders)
    }

    override fun mangaDetailsParse(response: Response): SManga = SManga.create().apply {
        val comic = json.decodeFromString<GraphiteComic>(response.body!!.string())

        title = comic.name
        author = comic.creator.joinToString(", ") { it.name }
        description = comic.description
        genre = comic.genres
            .sortedBy { it.name }
            .joinToString(", ") { it.name }
        thumbnail_url = comic.logo?.url
    }

    override fun chapterListRequest(manga: SManga): Request = mangaDetailsApiRequest(manga)

    private fun issueListRequest(comicId: String, comicUrl: String): Request {
        val newHeaders = headersBuilder()
            .set("Accept", ACCEPT_JSON)
            .set("Referer", baseUrl + comicUrl)
            .build()

        return GET("$baseUrl/api/title/issues/$comicId", newHeaders)
    }

    override fun chapterListParse(response: Response): List<SChapter> {
        // Need to get the comic id first to fetch the issues.
        val comic = json.decodeFromString<GraphiteComic>(response.body!!.string())
        val comicUrl = "/title/${comic.publisherSlug}/${comic.slug}"

        val issueRequest = issueListRequest(comic.id, comicUrl)
        val issueResponse = client.newCall(issueRequest).execute()
        val issues = json.decodeFromString<List<GraphiteIssue>>(issueResponse.body!!.string())

        return issues
            .sortedBy { issue -> issue.volumeNumber * 10 + issue.number }
            .filter { issue -> issue.accessRule.isNullOrBlank() }
            .map { issue -> chapterFromObject(issue, comic) }
            .reversed()
    }

    private fun chapterFromObject(issue: GraphiteIssue, comic: GraphiteComic): SChapter =
        SChapter.create().apply {
            name = "${issue.number} - ${issue.name}"
            scanlator = comic.publisher?.name
            date_upload = issue.createdAt.toDate()
            url = "/issue/${comic.publisherSlug}/${comic.slug}/${issue.slug}"
        }

    override fun pageListRequest(chapter: SChapter): Request {
        val newHeaders = headersBuilder()
            .set("Accept", ACCEPT_JSON)
            .set("Referer", baseUrl + chapter.url)
            .build()

        val urlPaths = chapter.url
            .removePrefix("/issue/")
            .split("/")

        val apiUrl = "$baseUrl/api/issue/find/null/".toHttpUrl().newBuilder()
            .addQueryParameter("publisher_slug", urlPaths[0])
            .addQueryParameter("title_slug", urlPaths[1])
            .addQueryParameter("slug", urlPaths[2])
            .toString()

        return GET(apiUrl, newHeaders)
    }

    override fun pageListParse(response: Response): List<Page> {
        val issue = json.decodeFromString<GraphiteIssue>(response.body!!.string())
        val issueUrl = "$baseUrl/issue/${issue.publisherSlug}/${issue.titleSlug}/${issue.slug}"

        return issue.pages
            .mapIndexed { i, page ->
                Page(i, "$issueUrl/${i + 1}", "$baseUrl/api/page/image/${page.id}")
            }
    }

    override fun imageUrlParse(response: Response): String = ""

    override fun imageRequest(page: Page): Request {
        val newHeaders = headersBuilder()
            .add("Accept", ACCEPT_IMAGE)
            .add("Host", baseUrl.toHttpUrl().host)
            .set("Referer", page.url)
            .build()

        return GET(page.imageUrl!!, newHeaders)
    }

    private fun buildQuery(queryAction: () -> String) = queryAction().replace("%", "$")

    private fun String.toDate(): Long {
        return runCatching { DATE_FORMATTER.parse(substringBefore("T"))?.time }
            .getOrNull() ?: 0L
    }

    companion object {
        private const val ACCEPT_ALL = "*/*"
        private const val ACCEPT_JSON = "application/json, text/plain, */*"
        private const val ACCEPT_IMAGE = "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8"

        private const val GRAPHQL_URL = "https://graphitecomics.com/graphql"

        private const val POPULAR_LIMIT = 50

        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaTypeOrNull()

        private val DATE_FORMATTER by lazy { SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH) }
    }
}
