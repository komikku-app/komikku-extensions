package eu.kanade.tachiyomi.extension.pt.mundowebtoon

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.interceptor.rateLimit
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import okhttp3.FormBody
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class MundoWebtoon : ParsedHttpSource() {

    override val name = "Mundo Webtoon"

    override val baseUrl = "https://mundowebtoon.com"

    override val lang = "pt-BR"

    override val supportsLatest = true

    override val client: OkHttpClient = network.cloudflareClient.newBuilder()
        .addInterceptor(::sanitizeHtmlIntercept)
        .rateLimit(1, 2, TimeUnit.SECONDS)
        .build()

    override fun headersBuilder(): Headers.Builder = Headers.Builder()
        .add("Accept", ACCEPT)
        .add("Accept-Language", ACCEPT_LANGUAGE)
        .add("Referer", "$baseUrl/")

    override fun popularMangaRequest(page: Int): Request = GET(baseUrl, headers)

    override fun popularMangaSelector(): String =
        "div.section:contains(mais lídos) + div.section div.andro_product"

    override fun popularMangaFromElement(element: Element): SManga = SManga.create().apply {
        title = element.select("h6.andro_product-title small").text().withoutLanguage()
        thumbnail_url = element.select("div.andro_product-thumb img").srcAttr()
        setUrlWithoutDomain(element.select("div.andro_product-thumb > a").attr("abs:href"))
    }

    override fun popularMangaNextPageSelector(): String? = null

    override fun latestUpdatesRequest(page: Int): Request {
        val path = if (page > 1) "/index.php?pagina=$page" else ""
        return GET("$baseUrl$path", headers)
    }

    override fun latestUpdatesSelector() = "div.row.atualizacoes div.andro_product"

    override fun latestUpdatesFromElement(element: Element): SManga = SManga.create().apply {
        title = element.select("h5.andro_product-title").text().withoutLanguage()
        thumbnail_url = element.select("div.andro_product-thumb img").srcAttr()
        setUrlWithoutDomain(element.select("div.andro_product-thumb > a").attr("abs:href"))
    }

    override fun latestUpdatesNextPageSelector() = "ul.paginacao li:last-child:not(.active) a"

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        val newHeaders = headers.newBuilder()
            .set("Referer", "$baseUrl/mangas")
            .build()

        val url = "$baseUrl/mangas".toHttpUrl().newBuilder()
            .addQueryParameter("busca", query)
            .toString()

        return GET(url, newHeaders)
    }

    override fun searchMangaSelector() = "div.container div.andro_product"

    override fun searchMangaFromElement(element: Element): SManga = SManga.create().apply {
        title = element.select("span.andro_product-title").text().withoutLanguage()
        thumbnail_url = element.select("div.andro_product-thumb img").srcAttr()
        setUrlWithoutDomain(element.select("div.andro_product-thumb > a").attr("abs:href"))
    }

    override fun searchMangaNextPageSelector(): String? = null

    override fun mangaDetailsParse(document: Document): SManga = SManga.create().apply {
        val infoElement = document.selectFirst("div.andro_product-single-content")!!

        title = infoElement.select("div.mangaTitulo h3").text().withoutLanguage()
        author = infoElement.select("div.BlDataItem a[href*=autor]")
            .joinToString(", ") { it.text() }
        artist = infoElement.select("div.BlDataItem a[href*=artista]")
            .joinToString(", ") { it.text() }
        genre = infoElement.select("div.col-md-12 a.label-warning[href*=genero]").toList()
            .filter { it.text().isNotEmpty() }
            .joinToString { it.text().trim() }
        status = infoElement.selectFirst("div.BlDataItem a[href*=status]")
            ?.text()?.toStatus() ?: SManga.UNKNOWN
        description = infoElement.select("div.andro_product-excerpt").text()
        thumbnail_url = document.select("div.andro_product-single-thumb img").srcAttr()
    }

    override fun chapterListSelector() = "div.CapitulosListaTodos div.CapitulosListaItem"

    override fun chapterFromElement(element: Element): SChapter = SChapter.create().apply {
        name = element.selectFirst("h5")!!.ownText()
        scanlator = element.select("a.color_gray[target='_blank']")
            .joinToString(", ") { it.text() }
        date_upload = element.select("h5 span[style]").text().toDate()
        setUrlWithoutDomain(element.selectFirst("a")!!.attr("abs:href"))
    }

    override fun pageListRequest(chapter: SChapter): Request {
        val chapterUrl = (baseUrl + chapter.url).toHttpUrl()

        val payload = FormBody.Builder()
            .add("data", chapterUrl.pathSegments[1])
            .add("num", chapterUrl.pathSegments[2])
            .add("modo", "1")
            .add("busca", "img")
            .build()

        val newHeaders = headersBuilder()
            .add("Content-Length", payload.contentLength().toString())
            .add("Content-Type", payload.contentType().toString())
            .set("Referer", baseUrl + chapter.url)
            .build()

        return POST("$baseUrl/leitor_image.php", newHeaders, payload)
    }

    override fun pageListParse(document: Document): List<Page> {
        return document.select("img[pag]")
            .mapIndexed { i, element ->
                Page(i, document.location(), element.attr("abs:src"))
            }
    }

    override fun imageUrlParse(document: Document) = ""

    override fun imageRequest(page: Page): Request {
        val newHeaders = headersBuilder()
            .set("Accept", ACCEPT_IMAGE)
            .set("Referer", page.url)
            .build()

        return GET(page.imageUrl!!, newHeaders)
    }

    private fun sanitizeHtmlIntercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())

        if (!response.headers["Content-Type"].orEmpty().contains("text/html")) {
            return response
        }

        val newBody = response.body.string()
            .replace("\t", "")
            .replace(SCRIPT_REGEX, "")
            .replace(HEAD_REGEX, "<head></head>")
            .replace(COMMENT_REGEX, "")
            .toResponseBody(HTML_MEDIA_TYPE)

        response.close()

        return response.newBuilder()
            .body(newBody)
            .build()
    }

    private fun Elements.srcAttr(): String =
        attr(if (hasAttr("data-src")) "data-src" else "src")

    private fun String.toDate(): Long {
        return runCatching { DATE_FORMATTER.parse(trim())?. time }
            .getOrNull() ?: 0L
    }

    private fun String.toStatus() = when (this) {
        "Ativo" -> SManga.ONGOING
        "Completo" -> SManga.COMPLETED
        else -> SManga.UNKNOWN
    }

    private fun String.withoutLanguage(): String = replace(FLAG_REGEX, "").trim()

    companion object {
        private const val ACCEPT = "text/html,application/xhtml+xml,application/xml;q=0.9," +
            "image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9"
        private const val ACCEPT_IMAGE = "image/webp,image/apng,image/*,*/*;q=0.8"
        private const val ACCEPT_LANGUAGE = "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7,es;q=0.6,gl;q=0.5"

        private val FLAG_REGEX = "\\((Pt[-/]br|Scan)\\)".toRegex(RegexOption.IGNORE_CASE)
        private val SCRIPT_REGEX = "<script>.*</script>"
            .toRegex(setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE))
        private val HEAD_REGEX = "<head>.*</head>"
            .toRegex(setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE))
        private val COMMENT_REGEX = "<!--.*-->".toRegex(RegexOption.MULTILINE)

        private val HTML_MEDIA_TYPE = "text/html".toMediaType()

        private val DATE_FORMATTER by lazy {
            SimpleDateFormat("(dd/MM/yyyy)", Locale.ENGLISH)
        }
    }
}
