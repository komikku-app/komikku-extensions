package eu.kanade.tachiyomi.extension.zh.baozimanhua

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.asObservableSuccess
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
import rx.Observable

class Baozimanhua : ParsedHttpSource() {

    override val name = "Baozimanhua"

    override val baseUrl = "https://cn.baozimh.com"

    override val lang = "zh"

    override val supportsLatest = true

    override val client: OkHttpClient = network.cloudflareClient

    override fun chapterListSelector(): String = "div.pure-g[id^=chapter] > div"

    override fun chapterListParse(response: Response): List<SChapter> {
        val document = response.asJsoup()
        val chapters = document.select(chapterListSelector()).map { element ->
            chapterFromElement(element)
        }
        // chapters are listed oldest to newest in the source
        return chapters.reversed()
    }

    override fun chapterFromElement(element: Element): SChapter {
        return SChapter.create().apply {
            url = element.select("a").attr("href").trim()
            name = element.text()
        }
    }

    override fun popularMangaSelector(): String = "div.pure-g div a.comics-card__poster"

    override fun popularMangaFromElement(element: Element): SManga {
        return SManga.create().apply {
            url = element.attr("href")!!.trim()
            title = element.attr("title")!!.trim()
            thumbnail_url = element.select("> amp-img").attr("src")!!.trim()
        }
    }

    override fun popularMangaNextPageSelector() = throw java.lang.UnsupportedOperationException("Not used.")

    override fun popularMangaRequest(page: Int): Request = GET("https://www.baozimh.com/classify", headers)

    override fun popularMangaParse(response: Response): MangasPage {
        val document = response.asJsoup()
        val mangas = document.select(popularMangaSelector()).map { element ->
            popularMangaFromElement(element)
        }
        return MangasPage(mangas, mangas.size == 36)
    }

    override fun latestUpdatesSelector(): String = "div.pure-g div a.comics-card__poster"

    override fun latestUpdatesFromElement(element: Element): SManga {
        return popularMangaFromElement(element)
    }

    override fun latestUpdatesNextPageSelector() = throw java.lang.UnsupportedOperationException("Not used.")

    override fun latestUpdatesRequest(page: Int): Request = GET("$baseUrl/list/new", headers)

    override fun latestUpdatesParse(response: Response): MangasPage {
        val document = response.asJsoup()
        val mangas = document.select(popularMangaSelector()).map { element ->
            popularMangaFromElement(element)
        }
        return MangasPage(mangas, mangas.size == 12)
    }

    override fun mangaDetailsParse(document: Document): SManga {
        return SManga.create().apply {
            title = document.select("h1.comics-detail__title").text().trim()
            thumbnail_url = document.select("div.pure-g div > amp-img").attr("src").trim()
            author = document.select("h2.comics-detail__author").text().trim()
            description = document.select("p.comics-detail__desc").text().trim()
            status = when (document.selectFirst("div.tag-list > span.tag").text().trim()) {
                "连载中" -> SManga.ONGOING
                "已完结" -> SManga.COMPLETED
                "連載中" -> SManga.ONGOING
                "已完結" -> SManga.COMPLETED
                else -> SManga.UNKNOWN
            }
        }
    }

    override fun pageListParse(document: Document): List<Page> {
        val pages = document.select("section.comic-contain > amp-img").mapIndexed() { index, element ->
            Page(index, imageUrl = element.attr("src").trim())
        }
        return pages
    }

    override fun imageUrlParse(document: Document) = throw UnsupportedOperationException("Not used.")

    override fun searchMangaSelector() = throw java.lang.UnsupportedOperationException("Not used.")

    override fun searchMangaFromElement(element: Element) = throw java.lang.UnsupportedOperationException("Not used.")

    override fun searchMangaNextPageSelector() = throw java.lang.UnsupportedOperationException("Not used.")

    override fun fetchSearchManga(page: Int, query: String, filters: FilterList): Observable<MangasPage> {
        return if (query.startsWith(ID_SEARCH_PREFIX)) {
            val id = query.removePrefix(ID_SEARCH_PREFIX)
            client.newCall(searchMangaByIdRequest(id))
                .asObservableSuccess()
                .map { response -> searchMangaByIdParse(response, id) }
        } else {
            super.fetchSearchManga(page, query, filters)
        }
    }

    private fun searchMangaByIdRequest(id: String) = GET("$baseUrl/comic/$id", headers)

    private fun searchMangaByIdParse(response: Response, id: String): MangasPage {
        val sManga = mangaDetailsParse(response)
        sManga.url = "/comic/$id"
        return MangasPage(listOf(sManga), false)
    }

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        // impossible to search a manga and use the filters
        return if (query.isNotEmpty()) {
            GET("$baseUrl/search?q=$query", headers)
        } else {
            lateinit var tag: String
            lateinit var region: String
            lateinit var status: String
            lateinit var start: String
            filters.forEach { filter ->
                when (filter) {
                    is TagFilter -> {
                        tag = filter.toUriPart()
                    }
                    is RegionFilter -> {
                        region = filter.toUriPart()
                    }
                    is StatusFilter -> {
                        status = filter.toUriPart()
                    }
                    is StartFilter -> {
                        start = filter.toUriPart()
                    }
                }
            }
            GET("$baseUrl/classify?type=$tag&region=$region&state=$status&filter=$start&page=$page")
        }
    }

    override fun searchMangaParse(response: Response): MangasPage {
        val document = response.asJsoup()
        // Normal search
        return if (response.request.url.encodedPath.startsWith("search?")) {
            val mangas = document.select(popularMangaSelector()).map { element ->
                popularMangaFromElement(element)
            }
            MangasPage(mangas, false)
            // Filter search
        } else {
            val mangas = document.select(popularMangaSelector()).map { element ->
                popularMangaFromElement(element)
            }
            MangasPage(mangas, mangas.size == 36)
        }
    }

    override fun getFilterList() = FilterList(
        Filter.Header("注意：不影響按標題搜索"),
        TagFilter(),
        RegionFilter(),
        StatusFilter(),
        StartFilter()
    )

    private open class UriPartFilter(displayName: String, val vals: Array<Pair<String, String>>) :
        Filter.Select<String>(displayName, vals.map { it.first }.toTypedArray()) {
        fun toUriPart() = vals[state].second
    }

    private class TagFilter : UriPartFilter(
        "标签",
        arrayOf(
            Pair("全部", "all"),
            Pair("都市", "dushi"),
            Pair("冒险", "mouxian"),
            Pair("热血", "rexie"),
            Pair("恋爱", "lianai"),
            Pair("耽美", "danmei"),
            Pair("武侠", "wuxia"),
            Pair("格斗", "gedou"),
            Pair("科幻", "kehuan"),
            Pair("魔幻", "mohuan"),
            Pair("推理", "tuili"),
            Pair("玄幻", "xuanhuan"),
            Pair("日常", "richang"),
            Pair("生活", "shenghuo"),
            Pair("搞笑", "gaoxiao"),
            Pair("校园", "xiaoyuan"),
            Pair("奇幻", "qihuan"),
            Pair("萌系", "mengxi"),
            Pair("穿越", "chuanyue"),
            Pair("后宫", "hougong"),
            Pair("战争", "zhanzheng"),
            Pair("历史", "lishi"),
            Pair("剧情", "juqing"),
            Pair("同人", "tongren"),
            Pair("竞技", "jingji"),
            Pair("励志", "lizhi"),
            Pair("治愈", "zhiyu"),
            Pair("机甲", "jijia"),
            Pair("纯爱", "chunai"),
            Pair("美食", "meishi"),
            Pair("恶搞", "egao"),
            Pair("虐心", "nuexin"),
            Pair("动作", "dongzuo"),
            Pair("惊险", "liangxian"),
            Pair("唯美", "weimei"),
            Pair("复仇", "fuchou"),
            Pair("脑洞", "naodong"),
            Pair("宫斗", "gongdou"),
            Pair("运动", "yundong"),
            Pair("灵异", "lingyi"),
            Pair("古风", "gufeng"),
            Pair("权谋", "quanmou"),
            Pair("节操", "jiecao"),
            Pair("明星", "mingxing"),
            Pair("暗黑", "anhei"),
            Pair("社会", "shehui"),
            Pair("音乐舞蹈", "yinlewudao"),
            Pair("东方", "dongfang"),
            Pair("AA", "aa"),
            Pair("悬疑", "xuanyi"),
            Pair("轻小说", "qingxiaoshuo"),
            Pair("霸总", "bazong"),
            Pair("萝莉", "luoli"),
            Pair("战斗", "zhandou"),
            Pair("惊悚", "liangsong"),
            Pair("百合", "yuri"),
            Pair("大女主", "danuzhu"),
            Pair("幻想", "huanxiang"),
            Pair("少女", "shaonu"),
            Pair("少年", "shaonian"),
            Pair("性转", "xingzhuanhuan"),
            Pair("重生", "zhongsheng"),
            Pair("韩漫", "hanman"),
            Pair("其它", "qita")
        )
    )

    private class RegionFilter : UriPartFilter(
        "地区",
        arrayOf(
            Pair("全部", "all"),
            Pair("国漫", "cn"),
            Pair("日本", "jp"),
            Pair("韩国", "kr"),
            Pair("欧美", "en")
        )
    )

    private class StatusFilter : UriPartFilter(
        "进度",
        arrayOf(
            Pair("全部", "all"),
            Pair("连载中", "serial"),
            Pair("已完结", "pub")
        )
    )

    private class StartFilter : UriPartFilter(
        "标题开头",
        arrayOf(
            Pair("全部", "*"),
            Pair("ABCD", "ABCD"),
            Pair("EFGH", "EFGH"),
            Pair("IJKL", "IJKL"),
            Pair("MNOP", "MNOP"),
            Pair("QRST", "QRST"),
            Pair("UVW", "UVW"),
            Pair("XYZ", "XYZ"),
            Pair("0-9", "0-9")
        )
    )

    companion object {
        const val ID_SEARCH_PREFIX = "id:"
    }
}
