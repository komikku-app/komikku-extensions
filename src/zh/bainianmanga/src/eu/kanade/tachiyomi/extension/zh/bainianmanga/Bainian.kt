package eu.kanade.tachiyomi.extension.zh.bainianmanga

import android.app.Application
import android.content.SharedPreferences
import androidx.preference.PreferenceScreen
import androidx.preference.SwitchPreferenceCompat
import eu.kanade.tachiyomi.AppInfo
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.interceptor.rateLimit
import eu.kanade.tachiyomi.source.ConfigurableSource
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import eu.kanade.tachiyomi.util.asJsoup
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Evaluator
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.text.SimpleDateFormat
import java.util.Locale

class Bainian : ParsedHttpSource(), ConfigurableSource {

    override val name = "百年漫画"
    override val lang = "zh"
    override val supportsLatest = true

    private val preferences: SharedPreferences =
        Injekt.get<Application>().getSharedPreferences("source_$id", 0x0000)

    private val useMirror = preferences.getBoolean(USE_MIRROR_PREF, false)
    override val baseUrl = if (useMirror) "https://www.mhqj.com" else "https://www.bnman.net"
    private fun String.stripMirror() = if (useMirror) "/comic" + removePrefix("/manhuadaquan") else this
    private fun String.toMirror() = if (useMirror) baseUrl + "/manhuadaquan" + removePrefix("/comic") else baseUrl + this

    override val client: OkHttpClient = network.client.newBuilder().rateLimit(2).build()

    override fun popularMangaRequest(page: Int) = GET("$baseUrl/page/hot/$page.html", headers)
    override fun popularMangaNextPageSelector() = ".pagination > li:last-child > a"
    override fun popularMangaSelector() = "ul#list_img > li > a"
    override fun popularMangaFromElement(element: Element) = SManga.create().apply {
        url = element.attr("href").stripMirror()
        val children = element.children()
        title = children[2].text()
        thumbnail_url = children[0].attr("src")
    }

    override fun popularMangaParse(response: Response): MangasPage {
        val document = response.asJsoup()
        if (filters.isEmpty()) parseFilters(document) // parse filters here
        val mangas = document.select(popularMangaSelector()).map(::popularMangaFromElement)
        val hasNextPage = document.selectFirst(popularMangaNextPageSelector()) != null
        return MangasPage(mangas, hasNextPage)
    }

    override fun latestUpdatesRequest(page: Int) = GET("$baseUrl/page/new/$page.html", headers)
    override fun latestUpdatesNextPageSelector() = popularMangaNextPageSelector()
    override fun latestUpdatesSelector() = popularMangaSelector()
    override fun latestUpdatesFromElement(element: Element) = popularMangaFromElement(element)

    override fun latestUpdatesParse(response: Response): MangasPage {
        val document = response.asJsoup()
        if (filters.isEmpty()) parseFilters(document) // parse filters here
        val mangas = document.select(latestUpdatesSelector()).map(::latestUpdatesFromElement)
        val hasNextPage = document.selectFirst(latestUpdatesNextPageSelector()) != null
        return MangasPage(mangas, hasNextPage)
    }

    override fun searchMangaNextPageSelector() = popularMangaNextPageSelector()
    override fun searchMangaSelector() = popularMangaSelector()
    override fun searchMangaFromElement(element: Element) = popularMangaFromElement(element)
    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        if (query.isNotEmpty()) {
            return GET("$baseUrl/search/$query/$page.html", headers)
        }
        for (filter in filters) if (filter is CategoryFilter) {
            val path = filter.getPath()
            if (path.isNotEmpty()) return GET("$baseUrl$path/$page.html", headers)
        }
        return popularMangaRequest(page)
    }

    override fun mangaDetailsRequest(manga: SManga) = GET(manga.url.toMirror(), headers)
    override fun mangaDetailsParse(document: Document) = SManga.create().apply {
        val details = document.selectFirst(Evaluator.Class("info")).child(0).children()
        title = details[0].text()
        author = details[3].child(1).text()
        description = document.selectFirst(Evaluator.Class("mt10")).text()
        genre = "${details[2].child(1).text()}, ${details[4].child(1).text()}"
        status = when (document.selectFirst(".title01 > h2").text()) {
            "连载中" -> SManga.ONGOING
            "已完结" -> SManga.COMPLETED
            else -> SManga.UNKNOWN
        }
        thumbnail_url = document.selectFirst(".bpic > img").attr("src")
        initialized = true
    }

    override fun chapterListRequest(manga: SManga) = mangaDetailsRequest(manga)
    override fun chapterListSelector() = "ul.jslist01 > li > a:not([href^=http])"
    override fun chapterFromElement(element: Element) = SChapter.create().apply {
        url = element.attr("href").stripMirror()
        name = element.text()
    }

    override fun chapterListParse(response: Response): List<SChapter> {
        val document = response.asJsoup()
        val list = document.select(chapterListSelector()).map { chapterFromElement(it) }
        if (list.isNotEmpty() && isNewDateLogic) {
            // div.info > ul:eq(0) > li:eq(5) > p:eq(1)
            val date = document.selectFirst(Evaluator.Class("info")).child(0).child(5).child(1).text()
            list[0].date_upload = dateFormat.parse(date)?.time ?: 0
        }
        return list
    }

    override fun pageListRequest(chapter: SChapter) = GET(chapter.url.toMirror(), headers)
    override fun pageListParse(document: Document): List<Page> {
        val script = document.selectFirst("body > script").data()
        val leftIndex = script.indexOf('[')
        val rightIndex = script.lastIndexOf(']')
        if (rightIndex - leftIndex <= 1) return emptyList() // empty string or empty list
        // '["...","..."]' - check baseUrl/static/manhua/comic.js
        val images = script.substring(leftIndex + 2, rightIndex - 1).replace("\\", "").split("\",\"")
        return images.mapIndexed { i, it ->
            val imageUrl = when {
                it.startsWith("http") -> it.replace("cxcyfhpt.com", "ygeol.net")
                else -> "https://img.jiequegongchang.com/$it"
            }
            // some hosts have invalid SSL certificates
            val imageUrlInHttp = "http:" + imageUrl.substringAfter(':')
            Page(i, imageUrl = imageUrlInHttp)
        }
    }

    override fun imageUrlParse(document: Document) = throw UnsupportedOperationException("Not used.")

    private class CategoryFilter(name: String, values: Array<String>, private val paths: List<String>) :
        Filter.Select<String>(name, values) {
        fun getPath() = paths[state]
    }

    private class FilterData(private val name: String, private val values: Array<String>, private val paths: List<String>) {
        fun toFilter() = CategoryFilter(name, values, paths)
    }

    private var filters: List<FilterData> = emptyList()

    private fun parseFilters(document: Document) {
        val categories = document.selectFirst(Evaluator.Class("select")).child(0).children().filter { it.tagName() == "dl" }
        filters = categories.map { element ->
            val children = element.children()
            val size = children.size
            val values = ArrayList<String>(size).apply { add("全部") }
            val paths = ArrayList<String>(size).apply { add("") }
            val iterator = children.iterator().apply { next() } // skip first
            while (iterator.hasNext()) {
                val next = iterator.next()
                values.add(next.text())
                paths.add(next.child(0).attr("href").removeSuffix(".html"))
            }
            FilterData(children[0].text(), values.toTypedArray(), paths)
        }
    }

    override fun getFilterList(): FilterList {
        val list: List<Filter<*>> =
            if (filters.isNotEmpty()) buildList(filters.size + 3) {
                add(Filter.Header("如果使用文本搜索，将会忽略分类筛选"))
                add(Filter.Header("最多使用一个分类筛选，多选时只有第一个有效"))
                add(Filter.Header("提示：分类筛选非常不准"))
                filters.forEach { add(it.toFilter()) }
            } else buildList(2) {
                add(Filter.Header("点击“重置”即可刷新分类，如果失败，"))
                add(Filter.Header("请尝试重新从图源列表点击进入图源"))
            }
        return FilterList(list)
    }

    override fun setupPreferenceScreen(screen: PreferenceScreen) {
        SwitchPreferenceCompat(screen.context).apply {
            key = USE_MIRROR_PREF
            title = "使用镜像网站"
            summary = "使用“漫画全集”网站，重启生效"
            setDefaultValue(false)
            setOnPreferenceChangeListener { _, newValue ->
                preferences.edit().putBoolean(USE_MIRROR_PREF, newValue as Boolean).apply()
                true
            }
        }.let { screen.addPreference(it) }
    }

    companion object {
        private const val USE_MIRROR_PREF = "USE_MIRROR"

        private val isNewDateLogic = AppInfo.getVersionCode() >= 81
        private val dateFormat by lazy { SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH) }
    }
}
