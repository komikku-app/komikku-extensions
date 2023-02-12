package eu.kanade.tachiyomi.extension.vi.medoctruyentranh

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import eu.kanade.tachiyomi.util.asJsoup
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import uy.kohesive.injekt.injectLazy
import java.text.SimpleDateFormat
import java.util.Locale

class MeDocTruyenTranh : ParsedHttpSource() {

    override val name = "MeDocTruyenTranh"

    override val baseUrl = "https://www.medoctruyentranh.net"

    override val lang = "vi"

    override val supportsLatest = false

    override val client = network.cloudflareClient

    private val json: Json by injectLazy()

    override fun popularMangaSelector() = "div.classifyList a"

    override fun searchMangaSelector() = ".listCon a"

    override fun popularMangaRequest(page: Int): Request {
        return GET("$baseUrl/tim-truyen/toan-bo" + if (page > 1) "/$page" else "", headers)
    }

    override fun popularMangaParse(response: Response): MangasPage {
        val document = response.asJsoup()

        // trying to build URLs from this JSONObject could cause issues but we need it to get thumbnails
        val nextData = document.select("script#__NEXT_DATA__").first()!!
            .let { json.parseToJsonElement(it.data()).jsonObject }

        val titleCoverMap = nextData["props"]!!
            .jsonObject["pageProps"]!!
            .jsonObject["initialState"]!!
            .jsonObject["classify"]!!
            .jsonObject["comics"]!!
            .jsonArray.associate {
                Pair(
                    it.jsonObject["title"]!!.jsonPrimitive.content,
                    it.jsonObject["coverimg"]!!.jsonPrimitive.content,
                )
            }

        val mangas = document.select(popularMangaSelector()).map {
            popularMangaFromElement(it).apply {
                thumbnail_url = titleCoverMap[this.title]
            }
        }

        return MangasPage(mangas, document.select(popularMangaNextPageSelector()) != null)
    }

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        return GET("$baseUrl/search/$query", headers)
    }

    override fun popularMangaFromElement(element: Element): SManga {
        return SManga.create().apply {
            title = element.select("div.storytitle").text()
            setUrlWithoutDomain(element.attr("href"))
        }
    }

    override fun popularMangaNextPageSelector() = "div.page_floor a.focus + a + a"

    override fun searchMangaFromElement(element: Element): SManga {
        val manga = SManga.create()
        val jsonData = element.ownerDocument()!!.select("#__NEXT_DATA__").first()!!.data()

        manga.setUrlWithoutDomain(element.attr("href"))
        manga.title = element.select("div.storytitle").text()

        val indexOfManga = jsonData.indexOf(manga.title)
        val startIndex = jsonData.indexOf("coverimg", indexOfManga) + 11
        val endIndex = jsonData.indexOf("}", startIndex) - 1
        manga.thumbnail_url = jsonData.substring(startIndex, endIndex)
        return manga
    }

    override fun searchMangaNextPageSelector() = popularMangaNextPageSelector()

    override fun mangaDetailsParse(document: Document): SManga = SManga.create().apply {
        val nextData = document.select("script#__NEXT_DATA__").first()!!
            .let { json.parseToJsonElement(it.data()).jsonObject }

        val mangaDetail = nextData["props"]!!
            .jsonObject["pageProps"]!!
            .jsonObject["initialState"]!!
            .jsonObject["detail"]!!
            .jsonObject["story_item"]!!
            .jsonObject

        title = mangaDetail["title"]!!.jsonPrimitive.content
        author = mangaDetail["author_list"]!!.jsonArray.joinToString { it.jsonPrimitive.content }
        genre = mangaDetail["category_list"]!!.jsonArray.joinToString { it.jsonPrimitive.content }
        description = mangaDetail["summary"]!!.jsonPrimitive.content
        status = parseStatus(mangaDetail["is_updating"]!!.jsonPrimitive.content)
        thumbnail_url = mangaDetail["coverimg"]!!.jsonPrimitive.content
    }

    private fun parseStatus(status: String) = when {
        status.contains("1") -> SManga.ONGOING
        status.contains("0") -> SManga.COMPLETED
        else -> SManga.UNKNOWN
    }

    override fun chapterListSelector() = "div.chapters  a"

    override fun chapterListParse(response: Response): List<SChapter> {
        val nextData = response.asJsoup().select("script#__NEXT_DATA__").first()!!
            .let { json.parseToJsonElement(it.data()).jsonObject }

        return nextData["props"]!!
            .jsonObject["pageProps"]!!
            .jsonObject["initialState"]!!
            .jsonObject["detail"]!!
            .jsonObject["story_chapters"]!!
            .jsonArray
            .flatMap { it.jsonArray }
            .map {
                val chapterObj = it.jsonObject

                SChapter.create().apply {
                    name = chapterObj["title"]!!.jsonPrimitive.content
                    date_upload = parseChapterDate(chapterObj["time"]!!.jsonPrimitive.content)
                    setUrlWithoutDomain("${response.request.url}/${chapterObj["chapter_index"]!!.jsonPrimitive.content}")
                }
            }
            .reversed()
    }

    private fun parseChapterDate(date: String): Long {
        // 2019-05-09T07:09:58
        val dateFormat = SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss",
            Locale.US,
        )
        return dateFormat.parse(date)?.time ?: 0L
    }

    override fun pageListParse(document: Document): List<Page> {
        val nextData = document.select("script#__NEXT_DATA__").firstOrNull()
            ?.let { json.parseToJsonElement(it.data()).jsonObject }
            ?: return emptyList()

        return nextData["props"]!!
            .jsonObject["pageProps"]!!
            .jsonObject["initialState"]!!
            .jsonObject["read"]!!
            .jsonObject["detail_item"]!!
            .jsonObject["elements"]!!
            .jsonArray
            .mapIndexed { i, jsonEl ->
                Page(i, "", jsonEl.jsonObject["content"]!!.jsonPrimitive.content)
            }
    }

    override fun imageUrlParse(document: Document) = throw UnsupportedOperationException("This method should not be called!")

    override fun latestUpdatesSelector() = throw UnsupportedOperationException("This method should not be called!")

    override fun latestUpdatesFromElement(element: Element) = throw UnsupportedOperationException("This method should not be called!")

    override fun latestUpdatesNextPageSelector() = throw UnsupportedOperationException("This method should not be called!")

    override fun latestUpdatesRequest(page: Int) = throw UnsupportedOperationException("This method should not be called!")

    override fun chapterFromElement(element: Element): SChapter = throw UnsupportedOperationException("This method should not be called!")
}
