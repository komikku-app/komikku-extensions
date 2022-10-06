package eu.kanade.tachiyomi.extension.all.mangaplus

import android.app.Application
import android.content.SharedPreferences
import androidx.preference.ListPreference
import androidx.preference.PreferenceScreen
import androidx.preference.SwitchPreferenceCompat
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.asObservableSuccess
import eu.kanade.tachiyomi.network.interceptor.rateLimitHost
import eu.kanade.tachiyomi.source.ConfigurableSource
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import rx.Observable
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy
import java.util.UUID

class MangaPlus(
    override val lang: String,
    private val internalLang: String,
    private val langCode: Language
) : HttpSource(), ConfigurableSource {

    override val name = "MANGA Plus by SHUEISHA"

    override val baseUrl = "https://mangaplus.shueisha.co.jp"

    override val supportsLatest = true

    override fun headersBuilder(): Headers.Builder = Headers.Builder()
        .add("Origin", baseUrl)
        .add("Referer", baseUrl)
        .add("User-Agent", USER_AGENT)
        .add("Session-Token", UUID.randomUUID().toString())

    override val client: OkHttpClient = network.client.newBuilder()
        .addInterceptor(::imageIntercept)
        .addInterceptor(::thumbnailIntercept)
        .rateLimitHost(API_URL.toHttpUrl(), 1)
        .rateLimitHost(baseUrl.toHttpUrl(), 2)
        .build()

    private val json: Json by injectLazy()

    private val intl by lazy { MangaPlusIntl(langCode) }

    private val preferences: SharedPreferences by lazy {
        Injekt.get<Application>().getSharedPreferences("source_$id", 0x0000)
    }

    private val imageQuality: String
        get() = preferences.getString("${QUALITY_PREF_KEY}_$lang", QUALITY_PREF_DEFAULT_VALUE)!!

    private val splitImages: Boolean
        get() = preferences.getBoolean("${SPLIT_PREF_KEY}_$lang", SPLIT_PREF_DEFAULT_VALUE)

    private var titleList: List<Title>? = null

    override fun popularMangaRequest(page: Int): Request {
        val newHeaders = headersBuilder()
            .set("Referer", "$baseUrl/manga_list/hot")
            .build()

        return GET("$API_URL/title_list/ranking?format=json", newHeaders)
    }

    override fun popularMangaParse(response: Response): MangasPage {
        val result = response.asMangaPlusResponse()

        checkNotNull(result.success) {
            result.error!!.langPopup(langCode)?.body ?: intl.unknownError
        }

        titleList = result.success.titleRankingView!!.titles
            .filter { it.language == langCode }

        val mangas = titleList!!.map(Title::toSManga)

        return MangasPage(mangas, false)
    }

    override fun latestUpdatesRequest(page: Int): Request {
        val newHeaders = headersBuilder()
            .set("Referer", "$baseUrl/updates")
            .build()

        return GET("$API_URL/web/web_homeV3?lang=$internalLang&format=json", newHeaders)
    }

    override fun latestUpdatesParse(response: Response): MangasPage {
        val result = response.asMangaPlusResponse()

        checkNotNull(result.success) {
            result.error!!.langPopup(langCode)?.body ?: intl.unknownError
        }

        // Fetch all titles to get newer thumbnail URLs in the interceptor.
        val popularResponse = client.newCall(popularMangaRequest(1)).execute()
            .asMangaPlusResponse()

        if (popularResponse.success != null) {
            titleList = popularResponse.success.titleRankingView!!.titles
                .filter { it.language == langCode }
        }

        val mangas = result.success.webHomeViewV3!!.groups
            .flatMap(UpdatedTitleV2Group::titleGroups)
            .flatMap(OriginalTitleGroup::titles)
            .map(UpdatedTitle::title)
            .filter { it.language == langCode }
            .map(Title::toSManga)
            .distinctBy(SManga::title)

        return MangasPage(mangas, false)
    }

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        if (query.matches(ID_SEARCH_PATTERN)) {
            return titleDetailsRequest(query.removePrefix(PREFIX_ID_SEARCH))
        } else if (query.matches(CHAPTER_ID_SEARCH_PATTERN)) {
            return mangaViewerRequest(query.removePrefix(PREFIX_CHAPTER_ID_SEARCH))
        }

        val newHeaders = headersBuilder()
            .set("Referer", "$baseUrl/manga_list/all")
            .build()

        val apiUrl = "$API_URL/title_list/allV2".toHttpUrl().newBuilder()
            .addQueryParameter("filter", query.trim())
            .addQueryParameter("format", "json")

        return GET(apiUrl.toString(), newHeaders)
    }

    override fun searchMangaParse(response: Response): MangasPage {
        val result = response.asMangaPlusResponse()

        checkNotNull(result.success) {
            result.error!!.langPopup(langCode)?.body ?: intl.unknownError
        }

        if (result.success.titleDetailView != null) {
            val mangaPlusTitle = result.success.titleDetailView.title
                .takeIf { it.language == langCode }
                ?: return MangasPage(emptyList(), hasNextPage = false)

            return MangasPage(listOf(mangaPlusTitle.toSManga()), hasNextPage = false)
        }

        if (result.success.mangaViewer != null) {
            checkNotNull(result.success.mangaViewer.titleId) { intl.chapterExpired }

            val titleId = result.success.mangaViewer.titleId
            val cacheTitle = titleList.orEmpty().firstOrNull { it.titleId == titleId }

            val manga = if (cacheTitle != null) {
                SManga.create().apply {
                    title = result.success.mangaViewer.titleName!!
                    thumbnail_url = cacheTitle.portraitImageUrl
                    url = "#/titles/$titleId"
                }
            } else {
                val titleRequest = titleDetailsRequest(titleId.toString())
                val titleResult = client.newCall(titleRequest).execute().asMangaPlusResponse()

                checkNotNull(titleResult.success) {
                    titleResult.error!!.langPopup(langCode)?.body ?: intl.unknownError
                }

                titleResult.success.titleDetailView!!
                    .takeIf { it.title.language == langCode }
                    ?.toSManga()
            }

            return MangasPage(listOfNotNull(manga), hasNextPage = false)
        }

        val filter = response.request.url.queryParameter("filter").orEmpty()

        titleList = result.success.allTitlesViewV2!!.allTitlesGroup
            .flatMap(AllTitlesGroup::titles)
            .filter { it.language == langCode }
            .filter { title ->
                title.name.contains(filter, ignoreCase = true) ||
                    title.author.contains(filter, ignoreCase = true)
            }

        val mangas = titleList!!.map(Title::toSManga)

        return MangasPage(mangas, hasNextPage = false)
    }

    private fun titleDetailsRequest(mangaUrl: String): Request {
        val titleId = mangaUrl.substringAfterLast("/")

        val newHeaders = headersBuilder()
            .set("Referer", "$baseUrl/titles/$titleId")
            .build()

        return GET("$API_URL/title_detail?title_id=$titleId&format=json", newHeaders)
    }

    // Workaround to allow "Open in browser" use the real URL.
    override fun fetchMangaDetails(manga: SManga): Observable<SManga> {
        return client.newCall(titleDetailsRequest(manga.url))
            .asObservableSuccess()
            .map { response ->
                mangaDetailsParse(response).apply { initialized = true }
            }
    }

    override fun mangaDetailsRequest(manga: SManga): Request {
        // Remove the '#' and map to the new url format used in website.
        return GET(baseUrl + manga.url.substring(1), headers)
    }

    override fun mangaDetailsParse(response: Response): SManga {
        val result = response.asMangaPlusResponse()

        checkNotNull(result.success) {
            val error = result.error!!.langPopup(langCode)

            when {
                error?.subject == NOT_FOUND_SUBJECT -> intl.titleRemoved
                !error?.body.isNullOrEmpty() -> error!!.body
                else -> intl.unknownError
            }
        }

        val titleDetails = result.success.titleDetailView!!
            .takeIf { it.title.language == langCode }
            ?: throw Exception(intl.notAvailable)

        return titleDetails.toSManga()
    }

    override fun chapterListRequest(manga: SManga): Request = titleDetailsRequest(manga.url)

    override fun chapterListParse(response: Response): List<SChapter> {
        val result = response.asMangaPlusResponse()

        checkNotNull(result.success) {
            val error = result.error!!.langPopup(langCode)

            when {
                error?.subject == NOT_FOUND_SUBJECT -> intl.titleRemoved
                !error?.body.isNullOrEmpty() -> error!!.body
                else -> intl.unknownError
            }
        }

        val titleDetailView = result.success.titleDetailView!!

        val chapters = titleDetailView.firstChapterList + titleDetailView.lastChapterList

        return chapters.reversed()
            // If the subTitle is null, then the chapter time expired.
            .filter { it.subTitle != null }
            .map(Chapter::toSChapter)
    }

    override fun pageListRequest(chapter: SChapter): Request {
        val chapterId = chapter.url.substringAfterLast("/")

        return mangaViewerRequest(chapterId)
    }

    private fun mangaViewerRequest(chapterId: String): Request {
        val newHeaders = headersBuilder()
            .set("Referer", "$baseUrl/viewer/$chapterId")
            .build()

        val url = "$API_URL/manga_viewer".toHttpUrlOrNull()!!.newBuilder()
            .addQueryParameter("chapter_id", chapterId)
            .addQueryParameter("split", if (splitImages) "yes" else "no")
            .addQueryParameter("img_quality", imageQuality)
            .addQueryParameter("format", "json")
            .toString()

        return GET(url, newHeaders)
    }

    override fun pageListParse(response: Response): List<Page> {
        val result = response.asMangaPlusResponse()

        checkNotNull(result.success) {
            val error = result.error!!.langPopup(langCode)

            when {
                error?.subject == NOT_FOUND_SUBJECT -> intl.chapterExpired
                !error?.body.isNullOrEmpty() -> error!!.body
                else -> intl.unknownError
            }
        }

        val referer = response.request.header("Referer")!!

        return result.success.mangaViewer!!.pages
            .mapNotNull(MangaPlusPage::mangaPage)
            .mapIndexed { i, page ->
                val encryptionKey = if (page.encryptionKey == null) "" else "#${page.encryptionKey}"
                Page(i, referer, page.imageUrl + encryptionKey)
            }
    }

    override fun fetchImageUrl(page: Page): Observable<String> = Observable.just(page.imageUrl!!)

    override fun imageUrlParse(response: Response): String = ""

    override fun imageRequest(page: Page): Request {
        val newHeaders = headersBuilder()
            .removeAll("Origin")
            .set("Referer", page.url)
            .build()

        return GET(page.imageUrl!!, newHeaders)
    }

    override fun setupPreferenceScreen(screen: PreferenceScreen) {
        val qualityPref = ListPreference(screen.context).apply {
            key = "${QUALITY_PREF_KEY}_$lang"
            title = intl.imageQuality
            entries = arrayOf(intl.imageQualityLow, intl.imageQualityMedium, intl.imageQualityHigh)
            entryValues = QUALITY_PREF_ENTRY_VALUES
            setDefaultValue(QUALITY_PREF_DEFAULT_VALUE)
            summary = "%s"
        }

        val splitPref = SwitchPreferenceCompat(screen.context).apply {
            key = "${SPLIT_PREF_KEY}_$lang"
            title = intl.splitDoublePages
            summary = intl.splitDoublePagesSummary
            setDefaultValue(SPLIT_PREF_DEFAULT_VALUE)
        }

        screen.addPreference(qualityPref)
        screen.addPreference(splitPref)
    }

    private fun imageIntercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)
        val encryptionKey = request.url.fragment

        if (encryptionKey.isNullOrEmpty()) {
            return response
        }

        val contentType = response.header("Content-Type", "image/jpeg")!!
        val image = response.body!!.bytes().decodeXorCipher(encryptionKey)
        val body = image.toResponseBody(contentType.toMediaTypeOrNull())

        return response.newBuilder()
            .body(body)
            .build()
    }

    private fun thumbnailIntercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        // Check if it is 404 to maintain compatibility when the extension used Weserv.
        val isBadCode = (response.code == 401 || response.code == 404)

        if (isBadCode && request.url.toString().contains(TITLE_THUMBNAIL_PATH)) {
            val titleId = request.url.toString()
                .substringBefore("/$TITLE_THUMBNAIL_PATH")
                .substringAfterLast("/")
                .toInt()
            val title = titleList?.find { it.titleId == titleId } ?: return response

            response.close()
            val thumbnailRequest = GET(title.portraitImageUrl, request.headers)
            return chain.proceed(thumbnailRequest)
        }

        return response
    }

    private fun ByteArray.decodeXorCipher(key: String): ByteArray {
        val keyStream = key.chunked(2)
            .map { it.toInt(16) }

        return mapIndexed { i, byte -> byte.toInt() xor keyStream[i % keyStream.size] }
            .map(Int::toByte)
            .toByteArray()
    }

    private fun Response.asMangaPlusResponse(): MangaPlusResponse = use {
        json.decodeFromString(body!!.string())
    }

    companion object {
        private const val API_URL = "https://jumpg-webapi.tokyo-cdn.com/api"
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/106.0.0.0 Safari/537.36"

        private const val QUALITY_PREF_KEY = "imageResolution"
        private val QUALITY_PREF_ENTRY_VALUES = arrayOf("low", "high", "super_high")
        private val QUALITY_PREF_DEFAULT_VALUE = QUALITY_PREF_ENTRY_VALUES[2]

        private const val SPLIT_PREF_KEY = "splitImage"
        private const val SPLIT_PREF_DEFAULT_VALUE = true

        private const val NOT_FOUND_SUBJECT = "Not Found"

        private const val TITLE_THUMBNAIL_PATH = "title_thumbnail_portrait_list"

        const val PREFIX_ID_SEARCH = "id:"
        private val ID_SEARCH_PATTERN = "^id:(\\d+)$".toRegex()
        const val PREFIX_CHAPTER_ID_SEARCH = "chapter-id:"
        private val CHAPTER_ID_SEARCH_PATTERN = "^chapter-id:(\\d+)$".toRegex()
    }
}
