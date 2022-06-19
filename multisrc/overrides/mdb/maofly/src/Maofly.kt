package eu.kanade.tachiyomi.extension.zh.maofly

import eu.kanade.tachiyomi.AppInfo
import eu.kanade.tachiyomi.multisrc.mdb.MDB
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.util.asJsoup
import okhttp3.Response
import org.jsoup.nodes.Element
import org.jsoup.select.Evaluator
import org.jsoup.select.QueryParser
import rufus.lzstring4java.LZString
import java.text.SimpleDateFormat
import java.util.Locale

class Maofly : MDB("漫画猫", "https://www.maofly.com") {

    override val supportsLatest = true

    override fun listUrl(params: String) = "$baseUrl/list/$params.html"
    override fun extractParams(listUrl: String) = listUrl.substringAfter("/list/").removeSuffix(".html")
    override fun searchUrl(page: Int, query: String) = "$baseUrl/search.html?q=$query&page=$page"

    override fun popularMangaNextPageSelector() = "div.pagination > li:last-child" // in the last page it's a span

    override fun latestUpdatesRequest(page: Int) = GET("$baseUrl/update-page-$page.html", headers)
    override fun latestUpdatesNextPageSelector() = popularMangaNextPageSelector()
    override fun latestUpdatesSelector() = searchMangaSelector()
    override fun latestUpdatesFromElement(element: Element) = popularMangaFromElement(element)

    override fun transformTitle(title: String) = title.run { substring(1, length - 1) } // 《title》
    override val authorSelector: Evaluator = QueryParser.parse("td.pub-duration")
    override fun transformDescription(description: String) =
        description.substringAfter("的漫画作品。").substringBeforeLast(" 。。欢迎您到漫画猫畅快阅读。")

    override fun chapterListParse(response: Response): List<SChapter> {
        val document = response.asJsoup()
        return document.select(chapterListSelector()).map { chapterFromElement(it) }.apply {
            if (!isNewDateLogic) return@apply
            this[0].date_upload = document.selectFirst(dateSelector).text()
                .let { dateFormat.parse(it)!!.time }
        }
    }

    // https://www.maofly.com/static/js/vg-read-v1.js
    override fun parseImages(imgData: String, readerConfig: Element): List<String> {
        val list = LZString.decompressFromBase64(imgData).split(',')
        val host = readerConfig.attr("data-chapter-domain")
        return list.map { "$host/uploads/$it" }
    }

    companion object {
        private val dateSelector = QueryParser.parse("th:contains(上次更新) + td")
        private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)
        private val isNewDateLogic = AppInfo.getVersionCode() >= 81
    }
}
