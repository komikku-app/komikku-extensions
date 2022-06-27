package eu.kanade.tachiyomi.extension.zh.copymanga

import android.app.Application
import android.content.SharedPreferences
import android.util.Log
import androidx.preference.ListPreference
import androidx.preference.PreferenceScreen
import androidx.preference.SwitchPreferenceCompat
import eu.kanade.tachiyomi.extension.zh.copymanga.MangaDto.Companion.parseChapterGroups
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.asObservableSuccess
import eu.kanade.tachiyomi.source.ConfigurableSource
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import rx.Observable
import rx.Single
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy
import kotlin.concurrent.thread

class CopyManga : HttpSource(), ConfigurableSource {
    override val name = "拷贝漫画"
    override val lang = "zh"
    override val supportsLatest = true

    private val json: Json by injectLazy()

    private val preferences: SharedPreferences =
        Injekt.get<Application>().getSharedPreferences("source_$id", 0x0000)

    private var domain = DOMAINS[preferences.getString(DOMAIN_PREF, "0")!!.toInt().coerceIn(0, DOMAINS.size - 1)]
    override val baseUrl = WWW_PREFIX + domain
    private var apiUrl = API_PREFIX + domain // www. 也可以

    override val client: OkHttpClient = network.client.newBuilder()
        .addInterceptor(NonblockingRateLimitInterceptor(2, 4)) // 2 requests per 4 seconds
        .build()

    override fun headersBuilder() = headersBuilder(preferences.getBoolean(OVERSEAS_CDN_PREF, false))
    private fun headersBuilder(useOverseasCdn: Boolean) = Headers.Builder()
        .add("User-Agent", System.getProperty("http.agent")!!)
        .add("region", if (useOverseasCdn) "0" else "1")

    private var apiHeaders = headersBuilder().build()

    private var useWebp = preferences.getBoolean(WEBP_PREF, true)

    init {
        MangaDto.convertToSc = preferences.getBoolean(SC_TITLE_PREF, false)
    }

    override fun popularMangaRequest(page: Int): Request {
        val offset = PAGE_SIZE * (page - 1)
        return GET("$apiUrl/api/v3/recs?pos=3200102&limit=$PAGE_SIZE&offset=$offset", apiHeaders)
    }

    override fun popularMangaParse(response: Response): MangasPage {
        val page: ListDto<MangaWrapperDto> = response.parseAs()
        val hasNextPage = page.offset + page.limit < page.total
        return MangasPage(page.list.map { it.toSManga() }, hasNextPage)
    }

    override fun latestUpdatesRequest(page: Int): Request {
        val offset = PAGE_SIZE * (page - 1)
        return GET("$apiUrl/api/v3/update/newest?limit=$PAGE_SIZE&offset=$offset", apiHeaders)
    }

    override fun latestUpdatesParse(response: Response) = popularMangaParse(response)

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        val offset = PAGE_SIZE * (page - 1)
        val builder = apiUrl.toHttpUrl().newBuilder()
            .addQueryParameter("limit", "$PAGE_SIZE")
            .addQueryParameter("offset", "$offset")
        if (query.isNotBlank()) {
            builder.addPathSegments("api/v3/search/comic")
                .addQueryParameter("q", query)
            filters.filterIsInstance<SearchFilter>().firstOrNull()?.addQuery(builder)
        } else {
            builder.addPathSegments("api/v3/comics")
            filters.filterIsInstance<CopyMangaFilter>().forEach {
                if (it !is SearchFilter) it.addQuery(builder)
            }
        }
        return Request.Builder().url(builder.build()).headers(apiHeaders).build()
    }

    override fun searchMangaParse(response: Response): MangasPage {
        val page: ListDto<MangaDto> = response.parseAs()
        val hasNextPage = page.offset + page.limit < page.total
        return MangasPage(page.list.map { it.toSManga() }, hasNextPage)
    }

    // 让 WebView 打开网页而不是 API
    override fun mangaDetailsRequest(manga: SManga) = GET(WWW_PREFIX + domain + manga.url, apiHeaders)

    private fun realMangaDetailsRequest(manga: SManga) =
        GET("$apiUrl/api/v3/comic2/${manga.url.removePrefix(MangaDto.URL_PREFIX)}", apiHeaders)

    override fun fetchMangaDetails(manga: SManga): Observable<SManga> =
        client.newCall(realMangaDetailsRequest(manga)).asObservableSuccess().map { mangaDetailsParse(it) }

    override fun mangaDetailsParse(response: Response): SManga =
        response.parseAs<MangaWrapperDto>().toSMangaDetails()

    override fun fetchChapterList(manga: SManga): Observable<List<SChapter>> = Single.create<List<SChapter>> {
        val result = ArrayList<SChapter>()
        val groups = manga.description?.parseChapterGroups() ?: run {
            val response = client.newCall(realMangaDetailsRequest(manga)).execute()
            response.parseAs<MangaWrapperDto>().groups!!.values
        }
        val mangaSlug = manga.url.removePrefix(MangaDto.URL_PREFIX)
        result.fetchChapterGroup(mangaSlug, "default", "")
        for (group in groups) {
            result.fetchChapterGroup(mangaSlug, group.path_word, group.name)
        }
        it.onSuccess(result)
    }.toObservable()

    private fun ArrayList<SChapter>.fetchChapterGroup(manga: String, key: String, name: String) {
        val result = ArrayList<SChapter>(0)
        var offset = 0
        var hasNextPage = true
        while (hasNextPage) {
            val response = client.newCall(GET("$apiUrl/api/v3/comic/$manga/group/$key/chapters?limit=$CHAPTER_PAGE_SIZE&offset=$offset", apiHeaders)).execute()
            val chapters: ListDto<ChapterDto> = response.parseAs()
            result.ensureCapacity(chapters.total)
            chapters.list.mapTo(result) { it.toSChapter(name) }
            offset += CHAPTER_PAGE_SIZE
            hasNextPage = offset < chapters.total
        }
        addAll(result.asReversed())
    }

    override fun chapterListRequest(manga: SManga) = throw UnsupportedOperationException("Not used.")
    override fun chapterListParse(response: Response) = throw UnsupportedOperationException("Not used.")

    // 新版 API 中间是 /chapter2/ 并且返回值需要排序
    override fun pageListRequest(chapter: SChapter) = GET("$apiUrl/api/v3${chapter.url}", apiHeaders)

    override fun pageListParse(response: Response): List<Page> =
        response.parseAs<ChapterPageListWrapperDto>().chapter.contents.mapIndexed { i, it ->
            Page(i, imageUrl = it.url)
        }

    override fun imageUrlParse(response: Response) = throw UnsupportedOperationException("Not used.")

    override fun imageRequest(page: Page): Request {
        val imageUrl = page.imageUrl!!
        return if (useWebp && imageUrl.endsWith(".jpg")) {
            GET(imageUrl.removeSuffix(".jpg") + ".webp")
        } else {
            GET(imageUrl)
        }
    }

    private inline fun <reified T> Response.parseAs(): T = use {
        if (code == 200) {
            json.decodeFromStream<ResultDto<T>>(body!!.byteStream()).results
        } else {
            throw Exception(json.decodeFromStream<ResultMessageDto>(body!!.byteStream()).message)
        }
    }

    private var genres: Array<Param> = emptyArray()
    private var isFetchingGenres = false

    override fun getFilterList(): FilterList {
        val genreFilter = if (genres.isEmpty()) {
            fetchGenres()
            Filter.Header("点击“重置”尝试刷新题材分类")
        } else {
            GenreFilter(genres)
        }
        return FilterList(
            SearchFilter(),
            Filter.Separator(),
            Filter.Header("分类（搜索文本时无效）"),
            genreFilter,
            RegionFilter(),
            StatusFilter(),
            SortFilter(),
        )
    }

    private fun fetchGenres() {
        if (genres.isNotEmpty() || isFetchingGenres) return
        isFetchingGenres = true
        thread {
            try {
                val response = client.newCall(GET("$apiUrl/api/v3/theme/comic/count?limit=500", apiHeaders)).execute()
                val list = response.parseAs<ListDto<KeywordDto>>().list
                val result = ArrayList<Param>(list.size + 1).apply { add(Param("全部", "")) }
                genres = list.mapTo(result) { it.toParam() }.toTypedArray()
            } catch (e: Exception) {
                Log.e("CopyManga", "failed to fetch genres", e)
            } finally {
                isFetchingGenres = false
            }
        }
    }

    override fun setupPreferenceScreen(screen: PreferenceScreen) {
        ListPreference(screen.context).apply {
            key = DOMAIN_PREF
            title = "网址域名"
            summary = "连接不稳定时可以尝试切换"
            entries = DOMAINS
            entryValues = DOMAIN_INDICES
            setDefaultValue("0")
            setOnPreferenceChangeListener { _, newValue ->
                val index = newValue as String
                preferences.edit().putString(DOMAIN_PREF, index).apply()
                domain = DOMAINS[index.toInt()]
                apiUrl = API_PREFIX + domain
                true
            }
        }.let { screen.addPreference(it) }

        SwitchPreferenceCompat(screen.context).apply {
            key = OVERSEAS_CDN_PREF
            title = "使用“港台及海外线路”"
            summary = "连接不稳定时可以尝试切换，关闭时使用“大陆用户线路”，已阅读章节需要清空缓存才能生效"
            setDefaultValue(false)
            setOnPreferenceChangeListener { _, newValue ->
                val useOverseasCdn = newValue as Boolean
                preferences.edit().putBoolean(OVERSEAS_CDN_PREF, useOverseasCdn).apply()
                apiHeaders = headersBuilder(useOverseasCdn).build()
                true
            }
        }.let { screen.addPreference(it) }

        SwitchPreferenceCompat(screen.context).apply {
            key = WEBP_PREF
            title = "使用 WebP 图片格式"
            summary = "默认开启，可以节省网站流量"
            setDefaultValue(true)
            setOnPreferenceChangeListener { _, newValue ->
                val webp = newValue as Boolean
                preferences.edit().putBoolean(WEBP_PREF, webp).apply()
                useWebp = webp
                true
            }
        }.let { screen.addPreference(it) }

        SwitchPreferenceCompat(screen.context).apply {
            key = SC_TITLE_PREF
            title = "将作品标题转换为简体中文"
            summary = "修改后，已添加漫画需要迁移才能更新标题"
            setDefaultValue(false)
            setOnPreferenceChangeListener { _, newValue ->
                val convertToSc = newValue as Boolean
                preferences.edit().putBoolean(SC_TITLE_PREF, convertToSc).apply()
                MangaDto.convertToSc = convertToSc
                true
            }
        }.let { screen.addPreference(it) }
    }

    companion object {
        private const val DOMAIN_PREF = "domain"
        private const val OVERSEAS_CDN_PREF = "changeCDN"
        private const val SC_TITLE_PREF = "showSCTitle"
        private const val WEBP_PREF = "webp"
        // private const val CHROME_VERSION_PREF = "chromeVersion" // default value was "103"

        private const val WWW_PREFIX = "https://www."
        private const val API_PREFIX = "https://api."
        private val DOMAINS = arrayOf("copymanga.org", "copymanga.info", "copymanga.net")
        private val DOMAIN_INDICES = arrayOf("0", "1", "2")

        private const val PAGE_SIZE = 20
        private const val CHAPTER_PAGE_SIZE = 500
    }
}
