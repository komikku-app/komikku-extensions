package eu.kanade.tachiyomi.multisrc.mangasar

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.interceptor.rateLimit
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.util.asJsoup
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.FormBody
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Element
import org.jsoup.parser.Parser
import rx.Observable
import uy.kohesive.injekt.injectLazy
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

abstract class MangaSar(
    override val name: String,
    override val baseUrl: String,
    override val lang: String,
) : HttpSource() {

    override val supportsLatest = true

    override val client: OkHttpClient = network.cloudflareClient.newBuilder()
        .addInterceptor(::searchIntercept)
        .rateLimit(1, 2, TimeUnit.SECONDS)
        .build()

    override fun headersBuilder(): Headers.Builder = Headers.Builder()
        .add("Accept", ACCEPT_HTML)
        .add("Accept-Language", ACCEPT_LANGUAGE)
        .add("Referer", "$baseUrl/")

    protected fun apiHeadersBuilder(): Headers.Builder = headersBuilder()
        .set("Accept", ACCEPT)
        .add("X-Requested-With", "XMLHttpRequest")

    private val apiHeaders: Headers by lazy { apiHeadersBuilder().build() }

    protected val json: Json by injectLazy()

    override fun popularMangaRequest(page: Int): Request {
        return GET(baseUrl, headers)
    }

    override fun popularMangaParse(response: Response): MangasPage {
        val document = response.asJsoup()

        val mangas = document.select(popularMangaSelector())
            .map(::popularMangaFromElement)

        return MangasPage(mangas, hasNextPage = false)
    }

    protected open fun popularMangaSelector(): String =
        "div:contains(Populares) ~ ul.mangasList li div.gridbox"

    protected open fun popularMangaFromElement(element: Element): SManga = SManga.create().apply {
        title = element.select("div.title a").first()!!.text()
        thumbnail_url = element.select("div.thumb img").first()!!.attr("abs:src")
        setUrlWithoutDomain(element.select("a").first()!!.attr("href"))
    }

    override fun latestUpdatesRequest(page: Int): Request {
        val form = FormBody.Builder()
            .add("pagina", page.toString())
            .build()

        val newHeaders = apiHeadersBuilder()
            .add("Content-Length", form.contentLength().toString())
            .add("Content-Type", form.contentType().toString())
            .build()

        return POST("$baseUrl/jsons/news/chapters.json", newHeaders, form)
    }

    override fun latestUpdatesParse(response: Response): MangasPage {
        val result = response.parseAs<MangaSarLatestDto>()

        val latestMangas = result.releases
            .map(::latestUpdatesFromObject)
            .distinctBy { it.url }

        val hasNextPage = result.page.toInt() < result.totalPage!!

        return MangasPage(latestMangas, hasNextPage)
    }

    protected fun latestUpdatesFromObject(release: MangaSarReleaseDto) = SManga.create().apply {
        title = release.name.withoutEntities()
        thumbnail_url = release.image
        url = release.link
    }

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        val url = "$baseUrl/wp-json/site/search/".toHttpUrlOrNull()!!.newBuilder()
            .addQueryParameter("keyword", query)
            .addQueryParameter("type", "undefined")
            .toString()

        return GET(url, apiHeaders)
    }

    override fun searchMangaParse(response: Response): MangasPage {
        val result = response.parseAs<Map<String, MangaSarTitleDto>>()

        val searchResults = result.values.map(::searchMangaFromObject)

        return MangasPage(searchResults, hasNextPage = false)
    }

    private fun searchMangaFromObject(manga: MangaSarTitleDto) = SManga.create().apply {
        title = manga.title
        thumbnail_url = manga.image
        setUrlWithoutDomain(manga.url)
    }

    override fun mangaDetailsParse(response: Response): SManga {
        val document = response.asJsoup()
        val infoElement = document.selectFirst("div.manga-single div.dados")!!

        return SManga.create().apply {
            title = infoElement.selectFirst("h1")!!.text()
            thumbnail_url = infoElement.selectFirst("div.thumb img")!!.attr("abs:src")
            description = infoElement.selectFirst("div.sinopse")!!.text()
            genre = infoElement.select("ul.generos li a span.button").joinToString { it.text() }
        }
    }

    override fun chapterListRequest(manga: SManga): Request = chapterListPaginatedRequest(manga.url)

    protected open fun chapterListPaginatedRequest(mangaUrl: String, page: Int = 1): Request {
        val mangaId = mangaUrl.substringAfterLast("/")

        val newHeaders = apiHeadersBuilder()
            .set("Referer", baseUrl + mangaUrl)
            .build()

        val url = "$baseUrl/jsons/series/chapters_list.json".toHttpUrlOrNull()!!.newBuilder()
            .addQueryParameter("page", page.toString())
            .addQueryParameter("order", "desc")
            .addQueryParameter("id_s", mangaId)
            .toString()

        return GET(url, newHeaders)
    }

    override fun chapterListParse(response: Response): List<SChapter> {
        val mangaUrl = response.request.header("Referer")!!.substringAfter(baseUrl)

        var result = response.parseAs<MangaSarPaginatedChaptersDto>()

        if (result.chapters.isNullOrEmpty()) {
            return emptyList()
        }

        val chapters = result.chapters!!
            .map(::chapterFromObject)
            .toMutableList()

        var page = result.page!! + 1
        val lastPage = result.totalPages

        while (++page <= lastPage!!) {
            val nextPageRequest = chapterListPaginatedRequest(mangaUrl, page)
            result = client.newCall(nextPageRequest).execute().parseAs()

            chapters += result.chapters!!
                .map(::chapterFromObject)
                .toMutableList()
        }

        return chapters
    }

    private fun chapterFromObject(chapter: MangaSarChapterDto): SChapter = SChapter.create().apply {
        name = "Cap. " + (if (chapter.number.booleanOrNull != null) "0" else chapter.number.content) +
            (if (chapter.name.isString) " - " + chapter.name.content else "")
        chapter_number = chapter.number.floatOrNull ?: -1f
        date_upload = chapter.dateCreated.substringBefore("T").toDate()
        setUrlWithoutDomain(chapter.link)
    }

    protected open fun pageListApiRequest(chapterUrl: String, serieId: String, token: String): Request {
        val newHeaders = apiHeadersBuilder()
            .set("Referer", chapterUrl)
            .build()

        val url = "$baseUrl/jsons/series/images_list.json".toHttpUrlOrNull()!!.newBuilder()
            .addQueryParameter("id_serie", serieId)
            .addQueryParameter("secury", token)
            .toString()

        return GET(url, newHeaders)
    }

    override fun pageListParse(response: Response): List<Page> {
        val document = response.asJsoup()
        val apiParams = document.selectFirst("script:containsData(id_serie)")?.data()
            ?: throw Exception(TOKEN_NOT_FOUND)

        val chapterUrl = response.request.url.toString()
        val serieId = apiParams.substringAfter("\"")
            .substringBefore("\"")
        val token = TOKEN_REGEX.find(apiParams)!!.groupValues[1]

        val apiRequest = pageListApiRequest(chapterUrl, serieId, token)
        val apiResponse = client.newCall(apiRequest).execute().parseAs<MangaSarReaderDto>()

        return apiResponse.images
            .filter { it.url.startsWith("http") }
            .mapIndexed { i, page -> Page(i, chapterUrl, page.url) }
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

    protected fun searchIntercept(chain: Interceptor.Chain): Response {
        if (chain.request().url.toString().contains("/search/")) {
            val homeRequest = popularMangaRequest(1)
            val document = chain.proceed(homeRequest).asJsoup()

            val apiParams = document.select("script:containsData(pAPI)").first()!!.data()
                .substringAfter("pAPI = ")
                .substringBeforeLast(";")
                .let { json.parseToJsonElement(it) }
                .jsonObject

            val newUrl = chain.request().url.newBuilder()
                .addQueryParameter("nonce", apiParams["nonce"]!!.jsonPrimitive.content)
                .build()

            val newRequest = chain.request().newBuilder()
                .url(newUrl)
                .build()

            return chain.proceed(newRequest)
        }

        return chain.proceed(chain.request())
    }

    protected inline fun <reified T> Response.parseAs(): T = use {
        json.decodeFromString(body.string())
    }

    protected fun String.toDate(): Long {
        return try {
            DATE_FORMATTER.parse(this)?.time ?: 0L
        } catch (e: ParseException) {
            0L
        }
    }

    private fun String.withoutEntities(): String {
        return Parser.unescapeEntities(this, true)
    }

    companion object {
        private const val ACCEPT = "application/json, text/plain, */*"
        private const val ACCEPT_HTML = "text/html,application/xhtml+xml,application/xml;q=0.9," +
            "image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9"
        private const val ACCEPT_IMAGE = "image/avif,image/webp,image/apng,image/*,*/*;q=0.8"
        private const val ACCEPT_LANGUAGE = "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7,es;q=0.6,gl;q=0.5"

        private val TOKEN_REGEX = "token\\s+= \"(.*)\"".toRegex()

        private val DATE_FORMATTER by lazy { SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH) }

        const val TOKEN_NOT_FOUND = "Não foi possível obter o token de leitura."
    }
}
