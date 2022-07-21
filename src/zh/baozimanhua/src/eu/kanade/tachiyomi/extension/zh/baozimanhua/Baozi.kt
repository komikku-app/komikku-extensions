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
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Evaluator
import rx.Observable
import rx.Single
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
        level = preferences.getString(BaoziBanner.PREF, DEFAULT_LEVEL)!!.toInt()
    )

    override val client = network.client.newBuilder()
        .rateLimit(2)
        .addInterceptor(bannerInterceptor)
        .build()

    override fun chapterListSelector() = throw UnsupportedOperationException("Not used.")

    override fun chapterListParse(response: Response): List<SChapter> {
        val document = response.asJsoup()
        return if (document.select(".l-box > .pure-g").size == 1) { // only latest chapters
            document.select(".l-box > .pure-g > div")
        } else {
            // chapters are listed oldest to newest in the source
            document.select(".l-box > .pure-g[id^=chapter] > div").reversed()
        }.map { chapterFromElement(it) }.apply {
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
            setUrlWithoutDomain(element.attr("href")!!.trim())
            title = element.attr("title")!!.trim()
            thumbnail_url = element.select("> amp-img").attr("src")!!.trim()
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

    override fun fetchPageList(chapter: SChapter): Observable<List<Page>> = Single.create<List<Page>> {
        val pageList = ArrayList<Page>(0)
        var url = baseUrl + chapter.url
        var pageCount = 0
        var i = 0
        do {
            val document = client.newCall(GET(url, headers)).execute().asJsoup()
            if (i == 0) {
                pageCount = document.selectFirst(Evaluator.Class("comic-text__amp"))
                    ?.run { text().substringAfter('/').toInt() } ?: break
                pageList.ensureCapacity(pageCount)
            }
            document.select(".comic-contain amp-img").mapTo(pageList) { element ->
                Page(i++, imageUrl = element.attr("src"))
            }
            url = document.selectFirst(Evaluator.Id("next-chapter"))?.attr("href") ?: ""
        } while (i < pageCount)
        it.onSuccess(pageList)
    }.toObservable()

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
            GET("$baseUrl/search?q=$query", headers)
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
            title = MIRROR_PREF_TITLE
            entries = MIRRORS
            entryValues = MIRRORS
            summary = MIRROR_PREF_SUMMARY
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
    }

    companion object {
        const val ID_SEARCH_PREFIX = "id:"

        private const val MIRROR_PREF = "MIRROR"
        private const val MIRROR_PREF_TITLE = "使用镜像网址"
        private const val MIRROR_PREF_SUMMARY = "重启生效，已选择：%s"
        private val MIRRORS = arrayOf("cn.baozimh.com", "cn.webmota.com")

        private const val DEFAULT_LEVEL = BaoziBanner.NORMAL.toString()

        private val DATE_FORMAT by lazy { SimpleDateFormat("yyyy年MM月dd日", Locale.ENGLISH) }
        private val isNewDateLogic = AppInfo.getVersionCode() >= 81
    }
}
