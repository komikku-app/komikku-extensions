package eu.kanade.tachiyomi.extension.zh.zerobyw

import android.app.Application
import android.content.SharedPreferences
import android.net.Uri
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceScreen
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.ConfigurableSource
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class Zerobyw : ParsedHttpSource(), ConfigurableSource {
    override val name: String = "zero搬运网"
    override val lang: String = "zh"
    override val supportsLatest: Boolean = false
    private val preferences: SharedPreferences =
        Injekt.get<Application>().getSharedPreferences("source_$id", 0x0000)

    // Url can be found at https://cdn.jsdelivr.net/gh/zerozzz123456/1/url.json
    // or just search for "zerobyw" in google
    private val defaultBaseUrl = "http://www.zerobywblac.com"

    override val baseUrl = preferences.getString("ZEROBYW_BASEURL", defaultBaseUrl)!!

    // Popular
    // Website does not provide popular manga, this is actually latest manga

    override fun popularMangaRequest(page: Int) = GET("$baseUrl/plugin.php?id=jameson_manhua&c=index&a=ku&&page=$page", headers)
    override fun popularMangaNextPageSelector(): String = "div.pg > a.nxt"
    override fun popularMangaSelector(): String = "div.uk-card"
    override fun popularMangaFromElement(element: Element): SManga = SManga.create().apply {
        title = getTitle(element.select("p.mt5 > a").text())
        setUrlWithoutDomain(element.select("p.mt5 > a").attr("abs:href"))
        thumbnail_url = element.select("img").attr("src")
    }

    // Latest

    override fun latestUpdatesRequest(page: Int) = throw Exception("Not used")
    override fun latestUpdatesNextPageSelector() = throw Exception("Not used")
    override fun latestUpdatesSelector() = throw Exception("Not used")
    override fun latestUpdatesFromElement(element: Element) = throw Exception("Not used")

    // Search

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        val uri = Uri.parse(baseUrl).buildUpon()
        if (query.isNotBlank()) {
            uri.appendPath("plugin.php")
                .appendQueryParameter("id", "jameson_manhua")
                .appendQueryParameter("a", "search")
                .appendQueryParameter("c", "index")
                .appendQueryParameter("keyword", query)
                .appendQueryParameter("page", page.toString())
        } else {
            uri.appendPath("plugin.php")
                .appendQueryParameter("id", "jameson_manhua")
                .appendQueryParameter("c", "index")
                .appendQueryParameter("a", "ku")
            filters.forEach {
                if (it is UriSelectFilterPath && it.toUri().second.isNotEmpty()) {
                    uri.appendQueryParameter(it.toUri().first, it.toUri().second)
                }
            }
            uri.appendQueryParameter("page", page.toString())
        }
        return GET(uri.toString(), headers)
    }

    override fun searchMangaNextPageSelector(): String = "div.pg > a.nxt"
    override fun searchMangaSelector(): String = "a.uk-card, div.uk-card"
    override fun searchMangaFromElement(element: Element): SManga = SManga.create().apply {
        title = getTitle(element.select("p.mt5").text())
        setUrlWithoutDomain(element.select("a").attr("abs:href"))
        thumbnail_url = element.select("img").attr("src")
    }

    // Details

    override fun mangaDetailsParse(document: Document): SManga = SManga.create().apply {
        title = getTitle(document.select("li.uk-active > h3.uk-heading-line").text())
        thumbnail_url = document.select("div.uk-width-medium > img").attr("abs:src")
        author = document.selectFirst("div.cl > a.uk-label")!!.text().substring(3)
        artist = author
        genre = document.select("div.cl > a.uk-label, div.cl > span.uk-label").eachText().joinToString(", ")
        description = document.select("li > div.uk-alert").html().replace("<br>", "")
        status = when (document.select("div.cl > span.uk-label").last()!!.text()) {
            "连载中" -> SManga.ONGOING
            "已完结" -> SManga.COMPLETED
            else -> SManga.UNKNOWN
        }
    }

    // Chapters

    override fun chapterListSelector(): String = "div.uk-grid-collapse > div.muludiv"
    override fun chapterFromElement(element: Element): SChapter = SChapter.create().apply {
        setUrlWithoutDomain(element.select("a.uk-button-default").attr("abs:href"))
        name = element.select("a.uk-button-default").text()
    }
    override fun chapterListParse(response: Response): List<SChapter> {
        return super.chapterListParse(response).reversed()
    }

    // Pages

    override fun pageListParse(document: Document): List<Page> = mutableListOf<Page>().apply {
        val images = document.select("div.uk-text-center > img")
        if (images.size == 0) {
            var message = document.select("div#messagetext > p")
            if (message.size == 0) {
                message = document.select("div.uk-alert > p")
            }
            if (message.size != 0) {
                throw Exception(message.text())
            }
        }
        images.forEach {
            add(Page(size, "", it.attr("src")))
        }
    }

    override fun imageUrlParse(document: Document): String = throw Exception("Not Used")

    // Filters

    override fun getFilterList() = FilterList(
        Filter.Header("如果使用文本搜索"),
        Filter.Header("过滤器将被忽略"),
        CategoryFilter(),
        StatusFilter(),
        AttributeFilter(),
    )

    private class CategoryFilter : UriSelectFilterPath(
        "category_id",
        "分类",
        arrayOf(
            Pair("", "全部"),
            Pair("1", "卖肉"),
            Pair("15", "战斗"),
            Pair("32", "日常"),
            Pair("6", "后宫"),
            Pair("13", "搞笑"),
            Pair("28", "日常"),
            Pair("31", "爱情"),
            Pair("22", "冒险"),
            Pair("23", "奇幻"),
            Pair("26", "战斗"),
            Pair("29", "体育"),
            Pair("34", "机战"),
            Pair("35", "职业"),
            Pair("36", "汉化组跟上，不再更新"),
        ),
    )

    private class StatusFilter : UriSelectFilterPath(
        "jindu",
        "进度",
        arrayOf(
            Pair("", "全部"),
            Pair("0", "连载中"),
            Pair("1", "已完结"),
        ),
    )

    private class AttributeFilter : UriSelectFilterPath(
        "shuxing",
        "性质",
        arrayOf(
            Pair("", "全部"),
            Pair("一半中文一半生肉", "一半中文一半生肉"),
            Pair("全生肉", "全生肉"),
            Pair("全中文", "全中文"),
        ),
    )

    /**
     * Class that creates a select filter. Each entry in the dropdown has a name and a display name.
     * If an entry is selected it is appended as a query parameter onto the end of the URI.
     */
    // vals: <name, display>
    private open class UriSelectFilterPath(
        val key: String,
        displayName: String,
        val vals: Array<Pair<String, String>>,
    ) : Filter.Select<String>(displayName, vals.map { it.second }.toTypedArray()) {
        fun toUri() = Pair(key, vals[state].first)
    }

    private fun getTitle(title: String): String {
        val result = Regex("【\\d+").find(title)
        return if (result != null) {
            title.substringBefore(result.value)
        } else {
            title.substringBefore("【")
        }
    }

    override fun setupPreferenceScreen(screen: PreferenceScreen) {
        EditTextPreference(screen.context)
            .apply {
                key = "ZEROBYW_BASEURL"
                title = "zerobyw网址"
                setDefaultValue(defaultBaseUrl)
                summary = "可在 https://cdn.jsdelivr.net/gh/zerozzz123456/1/url.json 中找到网址，或者通过google搜索\"zerobyw\"得到"
            }
            .let { screen.addPreference(it) }
    }
}
