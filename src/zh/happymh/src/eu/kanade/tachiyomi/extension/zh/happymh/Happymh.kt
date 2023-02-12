package eu.kanade.tachiyomi.extension.zh.happymh

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.util.asJsoup
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import uy.kohesive.injekt.injectLazy

class Happymh : HttpSource() {
    override val name: String = "嗨皮漫画"
    override val lang: String = "zh"
    override val supportsLatest: Boolean = true
    override val baseUrl: String = "https://m.happymh.com"
    override val client: OkHttpClient = network.cloudflareClient
    private val json: Json by injectLazy()

    // Popular

    // Requires login, otherwise result is the same as latest updates
    override fun popularMangaRequest(page: Int): Request {
        val header = headersBuilder().add("referer", "$baseUrl/latest").build()
        return GET("$baseUrl/apis/c/index?pn=$page&series_status=-1&order=views", header)
    }

    override fun popularMangaParse(response: Response): MangasPage {
        val data = json.parseToJsonElement(response.body.string()).jsonObject["data"]!!.jsonObject
        val items = data["items"]!!.jsonArray.map {
            SManga.create().apply {
                val obj = it.jsonObject
                title = obj["name"]!!.jsonPrimitive.content
                url = "/manga/${obj["manga_code"]!!.jsonPrimitive.content}"
                thumbnail_url = obj["cover"]!!.jsonPrimitive.content
            }
        }
        val isEnd = data["isEnd"]!!.jsonPrimitive.boolean
        return MangasPage(items, !isEnd)
    }

    // Latest

    override fun latestUpdatesRequest(page: Int): Request {
        val header = headersBuilder().add("referer", "$baseUrl/latest").build()
        return GET("$baseUrl/apis/c/index?pn=$page&series_status=-1&order=last_date", header)
    }

    override fun latestUpdatesParse(response: Response): MangasPage = popularMangaParse(response)

    // Search

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        val body = FormBody.Builder().addEncoded("searchkey", query).build()
        val header = headersBuilder().add("referer", "$baseUrl/ssearch").build()
        return POST("$baseUrl/apis/m/ssearch?pn=$page", header, body)
    }

    override fun searchMangaParse(response: Response): MangasPage {
        // I do not find a way to go to next page, so I always set hasNextPage to false
        return MangasPage(popularMangaParse(response).mangas, false)
    }

    // Details

    override fun mangaDetailsParse(response: Response): SManga = SManga.create().apply {
        val document = response.asJsoup()
        title = document.selectFirst("div.mg-property > h2.mg-title")!!.text()
        thumbnail_url = document.selectFirst("div.mg-cover > mip-img")!!.attr("abs:src")
        author = document.selectFirst("div.mg-property > p.mg-sub-title:nth-of-type(2)")!!.text()
        artist = author
        genre = document.select("div.mg-property > p.mg-cate > a").eachText().joinToString(", ")
        description = document.selectFirst("div.manga-introduction > mip-showmore#showmore")!!.text()
    }

    // Chapters

    override fun chapterListParse(response: Response): List<SChapter> {
        val comicId = response.request.url.pathSegments.last()
        val document = response.asJsoup()
        val script = document.selectFirst("mip-data > script:containsData(chapterList)")!!.html()
        return json.parseToJsonElement(script).jsonObject["chapterList"]!!.jsonArray.map {
            SChapter.create().apply {
                val chapterId = it.jsonObject["id"]!!.jsonPrimitive.content
                url = "/v2.0/apis/manga/read?code=$comicId&cid=$chapterId"
                name = it.jsonObject["chapterName"]!!.jsonPrimitive.content
            }
        }
    }

    // Pages

    override fun pageListRequest(chapter: SChapter): Request {
        // Some chapters return 403 without this header
        val header = headersBuilder().add("X-Requested-With", "XMLHttpRequest").build()
        return GET(baseUrl + chapter.url, header)
    }

    override fun pageListParse(response: Response): List<Page> {
        return json.parseToJsonElement(response.body.string())
            .jsonObject["data"]!!.jsonObject["scans"]!!.jsonArray
            // If n == 1, the image is from next chapter
            .filter { it.jsonObject["n"]!!.jsonPrimitive.int == 0 }
            .mapIndexed { index, it ->
                Page(index, "", it.jsonObject["url"]!!.jsonPrimitive.content)
            }
    }

    override fun imageUrlParse(response: Response): String = throw Exception("Not Used")
}
