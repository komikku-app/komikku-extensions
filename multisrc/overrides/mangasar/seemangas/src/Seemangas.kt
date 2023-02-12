package eu.kanade.tachiyomi.extension.pt.seemangas

import eu.kanade.tachiyomi.multisrc.mangasar.MangaSar
import eu.kanade.tachiyomi.multisrc.mangasar.MangaSarLatestDto
import eu.kanade.tachiyomi.multisrc.mangasar.MangaSarReaderDto
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.util.asJsoup
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.FormBody
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Element

class Seemangas : MangaSar(
    "Seemangas",
    "https://seemangas.com",
    "pt-BR",
) {

    override fun popularMangaSelector() = "ul.sidebar-popular li.popular-treending"

    override fun popularMangaFromElement(element: Element): SManga = SManga.create().apply {
        title = element.selectFirst("h4.title")!!.text()
        thumbnail_url = element.selectFirst("div.tumbl img")!!.attr("data-lazy-src")
        setUrlWithoutDomain(element.selectFirst("a")!!.attr("abs:href"))
    }

    override fun latestUpdatesRequest(page: Int): Request {
        val payload = FormBody.Builder()
            .add("action", "get_lancamentos")
            .add("pagina", page.toString())
            .build()

        val newHeaders = headersBuilder()
            .add("Content-Length", payload.contentLength().toString())
            .add("Content-Type", payload.contentType().toString())
            .add("X-Requested-With", "XMLHttpRequest")
            .build()

        return POST("$baseUrl/wp-admin/admin-ajax.php", newHeaders, payload)
    }

    override fun latestUpdatesParse(response: Response): MangasPage {
        val result = response.parseAs<MangaSarLatestDto>()

        val latestMangas = result.releases
            .map(::latestUpdatesFromObject)
            .distinctBy { it.url }

        return MangasPage(latestMangas, hasNextPage = result.releases.isNotEmpty())
    }

    override fun mangaDetailsParse(response: Response): SManga {
        val document = response.asJsoup()
        val infoElement = document.selectFirst("div.box-single:has(div.mangapage)")!!

        return SManga.create().apply {
            title = infoElement.selectFirst("h1.kw-title")!!.text()
            author = infoElement.selectFirst("div.mdq.author")!!.text().trim()
            description = infoElement.selectFirst("div.sinopse-page")!!.text()
            genre = infoElement.select("div.generos a.widget-btn")!!.joinToString { it.text() }
            status = infoElement.selectFirst("span.mdq")!!.text().toStatus()
            thumbnail_url = infoElement.selectFirst("div.thumb img")!!.attr("abs:data-lazy-src")
        }
    }
    override fun chapterListPaginatedRequest(mangaUrl: String, page: Int): Request {
        return GET(baseUrl + mangaUrl, headers)
    }

    override fun chapterListParse(response: Response): List<SChapter> {
        return response.asJsoup()
            .select("ul.full-chapters-list > li > a")
            .map(::chapterFromElement)
    }

    private fun chapterFromElement(element: Element): SChapter = SChapter.create().apply {
        name = element.selectFirst("span.cap-text")!!.text()
        date_upload = element.selectFirst("span.chapter-date")?.text()?.toDate() ?: 0L
        setUrlWithoutDomain(element.attr("href"))
    }

    override fun pageListApiRequest(chapterUrl: String, serieId: String, token: String): Request {
        val chapterId = CHAPTER_ID_REGEX.find(chapterUrl)!!.groupValues[1]

        val payload = FormBody.Builder()
            .add("action", "get_image_list")
            .add("id_serie", chapterId)
            .add("secury", token)
            .build()

        val newHeaders = apiHeadersBuilder()
            .add("Content-Length", payload.contentLength().toString())
            .add("Content-Type", payload.contentType().toString())
            .set("Referer", chapterUrl)
            .build()

        return POST("$baseUrl/wp-admin/admin-ajax.php", newHeaders, payload)
    }

    override fun pageListParse(response: Response): List<Page> {
        val document = response.asJsoup()
        val apiParams = document.selectFirst("script:containsData(id_serie)")?.data()
            ?: throw Exception(TOKEN_NOT_FOUND)

        val chapterUrl = response.request.url.toString()
        val infoReader = apiParams
            .substringAfter("{")
            .substringBeforeLast("}")
        val readerParams = json.parseToJsonElement("{$infoReader}").jsonObject
        val serieId = readerParams["id_serie"]!!.jsonPrimitive.content
        val token = readerParams["token"]!!.jsonPrimitive.content

        val apiRequest = pageListApiRequest(chapterUrl, serieId, token)
        val apiResponse = client.newCall(apiRequest).execute().parseAs<MangaSarReaderDto>()

        return apiResponse.images
            .filter { it.url.startsWith("http") }
            .mapIndexed { i, page -> Page(i, chapterUrl, page.url) }
    }

    private fun String.toStatus(): Int = when (this) {
        "Em andamento" -> SManga.ONGOING
        "Completo" -> SManga.COMPLETED
        else -> SManga.UNKNOWN
    }

    companion object {
        private val CHAPTER_ID_REGEX = "(\\d+)$".toRegex()
    }
}
