package eu.kanade.tachiyomi.extension.pt.nixmangas

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

class NixMangas : HttpSource() {

    override val name = "Nix Mangás"

    override val baseUrl = "https://nixmangas.com"

    override val lang = "pt-BR"

    override val supportsLatest = true

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
        return GET("$API_URL/mangas?page=$page", apiHeaders)
    }

    override fun latestUpdatesParse(response: Response): MangasPage {
        val result = response.parseAs<NixMangasPaginatedContent<NixMangasWorkDto>>()
        val workList = result.data.map(NixMangasWorkDto::toSManga)

        return MangasPage(workList, result.hasNextPage)
    }

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        // The search with query isn't working in their direct API for some reason,
        // so we use their site wrapped API instead for now.
        val apiUrl = "$baseUrl/obras".toHttpUrl().newBuilder()
            .addQueryParameter("page", page.toString())
            .addQueryParameter("q", query)
            .addQueryParameter("_data", "routes/__app/obras/index")
            .toString()

        return GET(apiUrl, apiHeaders)
    }

    override fun searchMangaParse(response: Response): MangasPage {
        val result = response.parseAs<NixMangasSearchDto>()
        val workList = result.mangas.data.map(NixMangasWorkDto::toSManga)

        return MangasPage(workList, result.mangas.hasNextPage)
    }

    override fun getMangaUrl(manga: SManga): String = baseUrl + manga.url

    override fun mangaDetailsRequest(manga: SManga): Request {
        // Their API doesn't have an endpoint for the manga details, so we
        // use their site wrapped API instead for now.
        val apiUrl = (baseUrl + manga.url).toHttpUrl().newBuilder()
            .addQueryParameter("_data", "routes/__app/obras/\$slug")
            .toString()

        return GET(apiUrl, apiHeaders)
    }

    override fun mangaDetailsParse(response: Response): SManga {
        val result = response.parseAs<NixMangasDetailsDto>()

        return result.manga.toSManga()
    }

    override fun chapterListRequest(manga: SManga): Request = mangaDetailsRequest(manga)

    override fun chapterListParse(response: Response): List<SChapter> {
        val result = response.parseAs<NixMangasDetailsDto>()
        val currentTimeStamp = System.currentTimeMillis()

        return result.manga.chapters
            .filter { it.isPublished }
            .map(NixMangasChapterDto::toSChapter)
            .filter { it.date_upload <= currentTimeStamp }
            .sortedByDescending(SChapter::chapter_number)
    }

    override fun getChapterUrl(chapter: SChapter): String = baseUrl + chapter.url

    override fun pageListRequest(chapter: SChapter): Request {
        val apiUrl = (baseUrl + chapter.url).toHttpUrl().newBuilder()
            .addQueryParameter("_data", "routes/__leitor/ler.\$manga.\$chapter")
            .toString()

        return GET(apiUrl, apiHeaders)
    }

    override fun pageListParse(response: Response): List<Page> {
        val result = response.parseAs<NixMangasReaderDto>()
        val chapterUrl = "$baseUrl/ler/${result.chapter.slug}"

        return result.chapter.pages.mapIndexed { i, pageDto ->
            Page(i, chapterUrl, pageDto.pageUrl)
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

        private const val API_URL = "https://api.nixmangas.com/v1"
        private const val CDN_URL = "https://cdn.nixmangas.com"
    }
}
