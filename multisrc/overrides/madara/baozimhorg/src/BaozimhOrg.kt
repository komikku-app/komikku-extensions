package eu.kanade.tachiyomi.extension.zh.baozimhorg

import android.app.Application
import androidx.preference.ListPreference
import androidx.preference.PreferenceScreen
import eu.kanade.tachiyomi.multisrc.madara.Madara
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.ConfigurableSource
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.util.asJsoup
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Evaluator
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.text.SimpleDateFormat
import java.util.Locale

// Uses Elementor + Madara theme.
class BaozimhOrg : ConfigurableSource, Madara(
    "包子漫画导航",
    "",
    "zh",
    SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
) {

    override val baseUrl: String

    init {
        val mirrors = MIRRORS
        val mirrorIndex = Injekt.get<Application>().getSharedPreferences("source_$id", 0x0000)
            .getString(MIRROR_PREF, "0")!!.toInt().coerceAtMost(mirrors.size - 1)
        baseUrl = "https://" + mirrors[mirrorIndex]
    }

    override val client = network.client

    override val useLoadMoreSearch = false
    override val sendViewCount = false

    override fun popularMangaRequest(page: Int) = GET("$baseUrl/hots/$page/", headers)
    override fun latestUpdatesRequest(page: Int) = GET("$baseUrl/dayup/$page/", headers)
    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        if (query.isNotEmpty()) {
            val url = "$baseUrl/page/$page/".toHttpUrl().newBuilder()
                .addQueryParameter("s", query)
            return Request.Builder().url(url.build()).headers(headers).build()
        }
        for (filter in filters) {
            if (filter is UriPartFilter) return GET(baseUrl + filter.toUriPart() + "page/$page/", headers)
        }
        return popularMangaRequest(page)
    }

    override val popularMangaUrlSelector = "h3 > a"

    override fun latestUpdatesParse(response: Response) = popularMangaParse(response)
    override fun searchMangaParse(response: Response) = popularMangaParse(response)
    override fun popularMangaParse(response: Response): MangasPage {
        val document = response.asJsoup().also(::parseGenresInternal)
        val mangas = document.select(Evaluator.Tag("article")).map(::popularMangaFromElement)
        val hasNextPage = document.selectFirst(Evaluator.Class("next"))?.tagName() == "a"
        return MangasPage(mangas, hasNextPage)
    }

    override val mangaDetailsSelectorStatus = "none"
    override val mangaDetailsSelectorDescription = "div.summary_content > div.post-content_item:last-child"
    override val seriesTypeSelector = "none"
    override val altNameSelector = "none"

    override fun mangaDetailsParse(document: Document): SManga {
        val manga = super.mangaDetailsParse(document)
        val genre = manga.genre ?: return manga
        val genreList = genre.split(", ") as ArrayList<String>
        for ((index, tag) in genreList.withIndex()) {
            val status = when (tag) {
                "连载中" -> SManga.ONGOING
                "已完结" -> SManga.COMPLETED
                else -> SManga.UNKNOWN
            }
            if (status != SManga.UNKNOWN) {
                manga.status = status
                genreList.removeAt(index)
                manga.genre = genreList.joinToString()
                return manga
            }
        }
        return manga
    }

    override fun chapterFromElement(element: Element) = SChapter.create().apply {
        val link = element.selectFirst(Evaluator.Tag("a"))
        url = link.attr("href").removePrefix(baseUrl)
        name = link.ownText()
        date_upload = parseChapterDate(link.child(0).text())
    }

    override fun parseRelativeDate(date: String) =
        super.parseRelativeDate(date.replace("小时", "hour"))

    // Jsoup won't ignore duplicates inside <noscript> tag
    override val pageListParseSelector = ".text-left img.lazyload"

    var genres: Array<Pair<String, String>> = emptyArray()

    private fun parseGenresInternal(document: Document) {
        if (genres.isNotEmpty()) return
        val box = document.selectFirst("[data-elementor-type=header] + div nav > ul") ?: return
        val items = box.select(Evaluator.Class("menu-item-type-custom"))
        genres = buildList(items.size + 1) {
            add(Pair("全部", "/allmanga/"))
            items.mapTo(this) {
                val link = it.child(0)
                Pair(link.ownText(), link.attr("href"))
            }
        }.toTypedArray()
    }

    override fun getFilterList(): FilterList =
        if (genres.isEmpty()) {
            FilterList(listOf(Filter.Header("点击“重置”刷新分类")))
        } else {
            val list = listOf(
                Filter.Header("分类（搜索文本时无效）"),
                UriPartFilter("分类", genres),
            )
            FilterList(list)
        }

    override fun setupPreferenceScreen(screen: PreferenceScreen) {
        ListPreference(screen.context).apply {
            val mirrors = MIRRORS
            key = MIRROR_PREF
            title = "镜像网址"
            summary = "%s\n重启生效"
            entries = mirrors
            entryValues = Array(mirrors.size) { it.toString() }
            setDefaultValue("0")
        }.let(screen::addPreference)
    }

    companion object {
        private const val MIRROR_PREF = "MIRROR"
        private val MIRRORS get() = arrayOf("baozimh.org", "cn.godamanga.com")
    }
}
