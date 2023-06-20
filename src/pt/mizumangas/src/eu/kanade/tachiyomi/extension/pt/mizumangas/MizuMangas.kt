package eu.kanade.tachiyomi.extension.pt.mizumangas

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.interceptor.rateLimitHost
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
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import rx.Observable
import uy.kohesive.injekt.injectLazy
import java.util.concurrent.TimeUnit

class MizuMangas : HttpSource() {

    override val name = "Mizu Mangás"

    override val baseUrl = "https://mizumangas.com.br"

    override val lang = "pt-BR"

    override val supportsLatest = true

    // Migrated from Madara to a custom CMS.
    override val versionId = 2

    override val client: OkHttpClient = network.cloudflareClient.newBuilder()
        .rateLimitHost(baseUrl.toHttpUrl(), 1, 1, TimeUnit.SECONDS)
        .rateLimitHost(API_URL.toHttpUrl(), 1, 1, TimeUnit.SECONDS)
        .rateLimitHost(CDN_URL.toHttpUrl(), 1, 2, TimeUnit.SECONDS)
        .build()

    private val json: Json by injectLazy()

    private val apiHeaders: Headers by lazy { apiHeadersBuilder().build() }

    override fun headersBuilder(): Headers.Builder = Headers.Builder()
        .add("Origin", baseUrl)
        .add("Referer", baseUrl)

    private fun apiHeadersBuilder(): Headers.Builder = headersBuilder()
        .add("Accept", ACCEPT_JSON)

    /**
     * The site doesn't have a popular section, so we use latest instead.
     */
    override fun popularMangaRequest(page: Int) = latestUpdatesRequest(page)

    override fun popularMangaParse(response: Response) = latestUpdatesParse(response)

    override fun latestUpdatesRequest(page: Int): Request {
        return GET("$API_URL/manga?page=$page&per_page=60", apiHeaders)
    }

    override fun latestUpdatesParse(response: Response): MangasPage {
        val result = response.parseAs<MizuMangasPaginatedContent<MizuMangasWorkDto>>()
        val workList = result.data.map(MizuMangasWorkDto::toSManga)

        return MangasPage(workList, result.hasNextPage)
    }

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        // The search with query isn't working in their direct API for some reason,
        // so we use their site wrapped API instead for now.
        val apiUrl = "$API_URL/search".toHttpUrl().newBuilder()
            .addPathSegment(query)
            .build()

        return GET(apiUrl, apiHeaders)
    }

    override fun searchMangaParse(response: Response): MangasPage {
        val result = response.parseAs<MizuMangasSearchDto>()
        val workList = result.mangas.map(MizuMangasWorkDto::toSManga)

        return MangasPage(workList, hasNextPage = false)
    }

    override fun getMangaUrl(manga: SManga): String = baseUrl + manga.url

    override fun mangaDetailsRequest(manga: SManga): Request {
        val id = manga.url.substringAfter("/manga/")

        return GET("$API_URL/manga/$id", apiHeaders)
    }

    override fun mangaDetailsParse(response: Response): SManga {
        return response.parseAs<MizuMangasWorkDto>().toSManga()
    }

    override fun chapterListRequest(manga: SManga): Request {
        val id = manga.url.substringAfter("/manga/")

        return GET("$API_URL/chapter/manga/all/$id", apiHeaders)
    }

    override fun chapterListParse(response: Response): List<SChapter> {
        return response.parseAs<List<MizuMangasChapterDto>>()
            .map(MizuMangasChapterDto::toSChapter)
    }

    override fun getChapterUrl(chapter: SChapter): String = baseUrl + chapter.url

    override fun pageListRequest(chapter: SChapter): Request {
        val id = chapter.url.substringAfter("/reader/")

        return GET("$API_URL/chapter/$id")
    }

    override fun pageListParse(response: Response): List<Page> {
        val result = response.parseAs<MizuMangasChapterDto>()
        val chapterUrl = "$baseUrl/manga/reader/${result.id}"

        return result.pages.mapIndexed { i, pageDto ->
            Page(i, chapterUrl, "$CDN_URL/${pageDto.page}")
        }
    }

    override fun fetchImageUrl(page: Page): Observable<String> = Observable.just(page.imageUrl!!)

    override fun imageUrlParse(response: Response): String = ""

    override fun imageRequest(page: Page): Request {
        val newHeaders = headersBuilder()
            .add("Accept", ACCEPT_IMAGE)
            .set("Referer", page.url)
            .build()

        return GET(page.imageUrl!!, newHeaders)
    }

    private inline fun <reified T> Response.parseAs(): T = use {
        json.decodeFromString(it.body.string())
    }

    companion object {
        private const val ACCEPT_IMAGE = "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8"
        private const val ACCEPT_JSON = "application/json"

        private const val API_URL = "https://api.mizumangas.com.br"
        const val CDN_URL = "https://cdn.mizumangas.com.br"
    }
}
