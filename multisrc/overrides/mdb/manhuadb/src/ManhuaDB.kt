package eu.kanade.tachiyomi.extension.zh.manhuadb

import android.util.Base64
import eu.kanade.tachiyomi.multisrc.mdb.MDB
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Response
import org.jsoup.nodes.Element
import org.jsoup.select.Evaluator
import org.jsoup.select.QueryParser
import uy.kohesive.injekt.injectLazy

class ManhuaDB : MDB("漫画DB", "https://www.manhuadb.com") {

    override val supportsLatest = false

    override fun listUrl(params: String) = "$baseUrl/manhua/list-$params.html"
    override fun extractParams(listUrl: String) = listUrl.substringAfter("/list-").removeSuffix(".html")
    override fun searchUrl(page: Int, query: String) = "$baseUrl/search?q=$query&p=$page"

    override fun popularMangaNextPageSelector() = "nav > div.form-inline > :nth-last-child(2):not(.disabled)"

    override fun latestUpdatesRequest(page: Int) = throw UnsupportedOperationException("Not used.")
    override fun latestUpdatesNextPageSelector() = throw UnsupportedOperationException("Not used.")
    override fun latestUpdatesSelector() = throw UnsupportedOperationException("Not used.")
    override fun latestUpdatesFromElement(element: Element) = throw UnsupportedOperationException("Not used.")

    override val authorSelector: Evaluator = QueryParser.parse("a.comic-creator")
    override fun transformDescription(description: String) = description.substringBeforeLast("欢迎在漫画DB观看")

    override fun chapterListParse(response: Response) = super.chapterListParse(response).asReversed()

    private val json: Json by injectLazy()

    // https://www.manhuadb.com/assets/js/vg-read.js
    override fun parseImages(imgData: String, readerConfig: Element): List<String> {
        val list: List<JsonObject> = Base64.decode(imgData, Base64.DEFAULT)
            .let { json.decodeFromString(String(it)) }
        val host = readerConfig.attr("data-host")
        val dir = readerConfig.attr("data-img_pre")
        return list.map { host + dir + it["img"]!!.jsonPrimitive.content }
    }
}
