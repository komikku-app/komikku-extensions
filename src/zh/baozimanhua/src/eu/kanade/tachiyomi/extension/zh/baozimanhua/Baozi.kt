package eu.kanade.tachiyomi.extension.zh.baozimanhua

import android.app.Application
import android.content.SharedPreferences
import androidx.preference.ListPreference
import androidx.preference.PreferenceScreen
import com.github.stevenyomi.baozibanner.BaoziBanner
import eu.kanade.tachiyomi.AppInfo
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.asObservableSuccess
import eu.kanade.tachiyomi.network.interceptor.rateLimit
import eu.kanade.tachiyomi.source.ConfigurableSource
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import eu.kanade.tachiyomi.util.asJsoup
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Evaluator
import rx.Observable
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.text.SimpleDateFormat
import java.util.Locale

class Baozi : ParsedHttpSource(), ConfigurableSource {

    override val id = 5724751873601868259

    override val name = "包子漫画"

    private val preferences: SharedPreferences =
        Injekt.get<Application>().getSharedPreferences("source_$id", 0x0000)

    override val baseUrl = "https://${preferences.getString(MIRROR_PREF, MIRRORS[0])}"

    override val lang = "zh"

    override val supportsLatest = true

    private val bannerInterceptor = BaoziBanner(
        level = preferences.getString(BaoziBanner.PREF, DEFAULT_LEVEL)!!.toInt(),
    )

    override val client = network.cloudflareClient.newBuilder()
        .rateLimit(2)
        .addInterceptor(bannerInterceptor)
        .addNetworkInterceptor(MissingImageInterceptor)
        .build()

    override fun headersBuilder() = super.headersBuilder()
        .add("Referer", "$baseUrl/")

    override fun chapterListSelector() = throw UnsupportedOperationException("Not used.")

    override fun chapterListParse(response: Response): List<SChapter> {
        val document = response.asJsoup()
        val fullListTitle = document.selectFirst(".section-title:containsOwn(章节目录), .section-title:containsOwn(章節目錄)")
        return if (fullListTitle == null) { // only latest chapters
            document.select(Evaluator.Class("comics-chapters"))
        } else {
            // chapters are listed oldest to newest in the source
            fullListTitle.parent()!!.select(Evaluator.Class("comics-chapters")).reversed()
        }.map { chapterFromElement(it) }.apply {
            val chapterOrderPref = preferences.getString(CHAPTER_ORDER_PREF, CHAPTER_ORDER_DISABLED)
            if (chapterOrderPref != CHAPTER_ORDER_DISABLED) {
                val isAggressive = chapterOrderPref == CHAPTER_ORDER_AGGRESSIVE
                forEach {
                    if (isAggressive || it.name.any(Char::isDigit)) {
                        it.url = it.url + '#' + it.name // += will use one more StringBuilder
                    }
                }
            }
            if (!isNewDateLogic) return@apply
            val date = document.select("em").text()
            if (date.contains('年')) {
                this[0].date_upload = date.removePrefix("(").removeSuffix(" 更新)")
                    .let { DATE_FORMAT.parse(it) }?.time ?: 0L
            } // 否则要么是没有，要么必然是今天（格式如 "今天 xx:xx", "x小时前", "x分钟前"）可以偷懒直接用默认的获取时间
        }
    }

    override fun chapterFromElement(element: Element): SChapter {
        return SChapter.create().apply {
            setUrlWithoutDomain(element.select("a").attr("href").trim())
            name = element.text()
        }
    }

    override fun popularMangaSelector(): String = "div.pure-g div a.comics-card__poster"

    override fun popularMangaFromElement(element: Element): SManga {
        return SManga.create().apply {
            setUrlWithoutDomain(element.attr("href").trim())
            title = element.attr("title").trim()
            thumbnail_url = element.select("> amp-img").attr("src").trim()
        }
    }

    override fun popularMangaNextPageSelector(): String? = null

    override fun popularMangaRequest(page: Int): Request = GET("$baseUrl/classify?page=$page", headers)

    override fun popularMangaParse(response: Response): MangasPage {
        val mangas = super.popularMangaParse(response).mangas
        return MangasPage(mangas, mangas.size == 36)
    }

    override fun latestUpdatesSelector(): String = "div.pure-g div a.comics-card__poster"

    override fun latestUpdatesFromElement(element: Element): SManga {
        return popularMangaFromElement(element)
    }

    override fun latestUpdatesNextPageSelector(): String? = null

    override fun latestUpdatesRequest(page: Int): Request = GET("$baseUrl/list/new", headers)

    override fun mangaDetailsParse(document: Document): SManga {
        return SManga.create().apply {
            title = document.select("h1.comics-detail__title").text()
            thumbnail_url = document.select("div.pure-g div > amp-img").attr("src").trim()
            author = document.select("h2.comics-detail__author").text()
            description = document.select("p.comics-detail__desc").text()
            status = when (document.selectFirst("div.tag-list > span.tag")!!.text()) {
                "连载中", "連載中" -> SManga.ONGOING
                "已完结", "已完結" -> SManga.COMPLETED
                else -> SManga.UNKNOWN
            }
        }
    }

    override fun fetchPageList(chapter: SChapter): Observable<List<Page>> = Observable.fromCallable {
        val pageNumberSelector = Evaluator.Class("comic-text__amp")
        val pageList = ArrayList<Page>(0)
        var url = baseUrl + chapter.url
        var i = 0
        while (true) {
            val document = client.newCall(GET(url, headers)).execute().asJsoup()
            document.select(".comic-contain amp-img").dropWhile { element ->
                element.selectFirst(pageNumberSelector)!!.text().substringBefore('/').toInt() <= i
            }.mapTo(pageList) { element ->
                Page(i++, imageUrl = element.attr("src"))
            }
            url = document.selectFirst(Evaluator.Id("next-chapter"))
                ?.takeIf {
                    val text = it.text()
                    text == "下一页" || text == "下一頁"
                }
                ?.attr("href")
                ?: break
        }
        pageList
    }

    override fun pageListParse(document: Document) = throw UnsupportedOperationException("Not used.")

    override fun imageUrlParse(document: Document) = throw UnsupportedOperationException("Not used.")

    override fun searchMangaSelector() = throw UnsupportedOperationException("Not used.")

    override fun searchMangaFromElement(element: Element) = throw UnsupportedOperationException("Not used.")

    override fun searchMangaNextPageSelector() = throw UnsupportedOperationException("Not used.")

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
            val baseUrl = baseUrl.replace("webmota.com", "baozimh.com")
            val url = baseUrl.toHttpUrl().newBuilder()
                .addEncodedPathSegment("search")
                .addQueryParameter("q", query)
                .toString()
            GET(url, headers)
        } else {
            val parts = filters.filterIsInstance<UriPartFilter>().joinToString("&") { it.toUriPart() }
            GET("$baseUrl/classify?page=$page&$parts", headers)
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

    override fun getFilterList() = getFilters()

    override fun setupPreferenceScreen(screen: PreferenceScreen) {
        ListPreference(screen.context).apply {
            key = MIRROR_PREF
            title = "使用镜像网址"
            entries = MIRRORS
            entryValues = MIRRORS
            summary = "已选择：%s\n" +
                "重启生效，切换简繁体后需要迁移才能刷新漫画标题。\n" +
                "搜索漫画时自动使用 baozimh.com 域名以避免出错。"
            setDefaultValue(MIRRORS[0])
        }.let { screen.addPreference(it) }

        ListPreference(screen.context).apply {
            key = BaoziBanner.PREF
            title = BaoziBanner.PREF_TITLE
            summary = BaoziBanner.PREF_SUMMARY
            entries = BaoziBanner.PREF_ENTRIES
            entryValues = BaoziBanner.PREF_VALUES
            setDefaultValue(DEFAULT_LEVEL)
            setOnPreferenceChangeListener { _, newValue ->
                bannerInterceptor.level = (newValue as String).toInt()
                true
            }
        }.let { screen.addPreference(it) }

        ListPreference(screen.context).apply {
            key = CHAPTER_ORDER_PREF
            title = "修复章节顺序错误导致的错标已读"
            summary = "已选择：%s\n" +
                "部分作品的章节顺序错误，最新章节总是显示为一个旧章节，导致检查更新时新章节被错标为已读。" +
                "开启后，将会正确判断新章节和已读情况，但是错误的章节顺序不会改变。" +
                "如果作品有章节标号重复，开启或关闭后第一次刷新会导致它们的阅读状态同步。" +
                "开启或关闭强力模式后第一次刷新会将所有未标号的章节标记为未读。"
            entries = arrayOf("关闭", "开启 (对有标号的章节有效)", "强力模式 (对所有章节有效)")
            entryValues = arrayOf(CHAPTER_ORDER_DISABLED, CHAPTER_ORDER_ENABLED, CHAPTER_ORDER_AGGRESSIVE)
            setDefaultValue(CHAPTER_ORDER_DISABLED)
        }.let { screen.addPreference(it) }
    }

    companion object {
        const val ID_SEARCH_PREFIX = "id:"

        private const val MIRROR_PREF = "MIRROR"
        private val MIRRORS = arrayOf(
            "cn.baozimh.com",
            "cn.webmota.com",
            "tw.baozimh.com",
            "tw.webmota.com",
            "www.baozimh.com",
            "www.webmota.com",
        )

        private const val DEFAULT_LEVEL = BaoziBanner.NORMAL.toString()

        private const val CHAPTER_ORDER_PREF = "CHAPTER_ORDER"
        private const val CHAPTER_ORDER_DISABLED = "0"
        private const val CHAPTER_ORDER_ENABLED = "1"
        private const val CHAPTER_ORDER_AGGRESSIVE = "2"

        private val DATE_FORMAT by lazy { SimpleDateFormat("yyyy年MM月dd日", Locale.ENGLISH) }
        private val isNewDateLogic = AppInfo.getVersionCode() >= 81
    }
}
