package eu.kanade.tachiyomi.extension.en.webnovel

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.asObservableSuccess
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.util.asJsoup
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Document
import rx.Observable
import uy.kohesive.injekt.injectLazy
import java.io.IOException
import java.util.Calendar
import java.util.Date

class Webnovel : HttpSource() {

    override val name = "Webnovel.com"

    override val baseUrl = "https://www.webnovel.com"

    private val baseApiUrl = "$baseUrl$BASE_API_ENDPOINT"

    private val baseCoverURl = baseUrl.replace("www", "img")

    private val baseCdnUrl = baseUrl.replace("www", "comic-image")

    override val lang = "en"

    override val supportsLatest = true

    override val client: OkHttpClient = network.cloudflareClient
        .newBuilder()
        .addNetworkInterceptor(::csrfTokenInterceptor)
        .addNetworkInterceptor(::expiredImageUrlInterceptor)
        .build()

    private val json: Json by injectLazy()

    // Popular
    override fun popularMangaRequest(page: Int): Request = searchMangaRequest(
        page = page,
        query = "",
        filters = FilterList(
            SortByFilter(default = 1)
        )
    )

    override fun popularMangaParse(response: Response): MangasPage = searchMangaParse(response)

    // Latest
    override fun latestUpdatesRequest(page: Int): Request = searchMangaRequest(
        page = page,
        query = "",
        filters = FilterList(
            SortByFilter(default = 5)
        )
    )

    override fun latestUpdatesParse(response: Response): MangasPage = searchMangaParse(response)

    // Search
    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        if (query.isNotBlank()) {
            val url = "$baseApiUrl$QUERY_SEARCH_PATH?type=manga&pageIndex=$page".toHttpUrl()
                .newBuilder()
                .addQueryParameter("keywords", query)
                .toString()

            return GET(url, headers)
        }
        val sort = filters.firstInstanceOrNull<SortByFilter>()?.selectedValue.orEmpty()
        val contentStatus = filters.firstInstanceOrNull<ContentStatusFilter>()?.selectedValue.orEmpty()
        val genre = filters.firstInstanceOrNull<GenreFilter>()?.selectedValue.orEmpty()

        val url = "$baseApiUrl$FILTER_SEARCH_PATH?categoryType=2&pageIndex=$page" +
            "&categoryId=$genre&bookStatus=$contentStatus&orderBy=$sort"

        return GET(url, headers)
    }

    override fun searchMangaParse(response: Response): MangasPage {
        val browseResponseDto = if (response.request.url.toString().contains(QUERY_SEARCH_PATH)) {
            response.checkAndParseAs<QuerySearchResponseDto>().browseResponse
        } else {
            // Due to the previous line this automatically parses as "BrowseResponseDto"
            response.checkAndParseAs()
        }

        val manga = browseResponseDto.items.map {
            SManga.create().apply {
                title = it.name
                url = it.id
                thumbnail_url = getCoverUrl(it.id)
            }
        }

        return MangasPage(manga, browseResponseDto.isLast == 0)
    }

    // Manga details
    // TODO: Cleanup this block when ext-lib 1.4 is released
    override fun mangaDetailsRequest(manga: SManga): Request {
        return GET("$baseUrl/comic/${manga.getId}", headers)
    }

    override fun fetchMangaDetails(manga: SManga): Observable<SManga> {
        return client.newCall(internalMangaDetailsRequest(manga))
            .asObservableSuccess()
            .map { response ->
                mangaDetailsParse(response)
            }
    }

    private fun internalMangaDetailsRequest(manga: SManga): Request {
        return GET("$baseApiUrl/comic/getComicDetailPage?comicId=${manga.getId}", headers)
    }

    override fun mangaDetailsParse(response: Response): SManga {
        val comic = response.checkAndParseAs<ComicDetailInfoResponseDto>().info
        return SManga.create().apply {
            title = comic.name
            url = comic.id
            thumbnail_url = getCoverUrl(comic.id)
            author = comic.authorName
            description = comic.description
            genre = comic.categoryName
            status = when (comic.actionStatus) {
                1 -> SManga.ONGOING
                2 -> SManga.COMPLETED
                else -> SManga.UNKNOWN
            }
        }
    }

    // chapters
    override fun chapterListRequest(manga: SManga): Request {
        return GET("$baseApiUrl/comic/getChapterList?comicId=${manga.getId}", headers)
    }

    override fun chapterListParse(response: Response): List<SChapter> {
        val chapterList = response.checkAndParseAs<ComicChapterListDto>()
        val comic = chapterList.comicInfo
        val chapters = chapterList.comicChapters.reversed().asSequence()

        val updateTimes = chapters.map { it.publishTime.toDate() }
        val filteredChapters = chapters
            // You can pay to get some chapter earlier than others. This privilege is divided into some tiers
            // We check if user has same tier unlocked as chapter's.
            .filter { it.userLevel == it.chapterLevel }

        // When new privileged chapter is released oldest privileged chapter becomes normal one (in most cases)
        // but since those normal chapter retain the original upload time we improvise. (This isn't optimal but meh)
        return filteredChapters.zip(updateTimes) { chapter, updateTime ->
            val namePrefix = when {
                chapter.isPremium && !chapter.isAccessibleByUser -> "\uD83D\uDD12 "
                else -> ""
            }
            SChapter.create().apply {
                name = namePrefix + chapter.name
                url = "${comic.id}:${chapter.id}"
                date_upload = updateTime
            }
        }.toList()
    }

    private val ComicChapterDto.isPremium get() = isVip != 0 || price != 0
    // This can mean the chapter is free or user has paid to unlock it (check with [isPremium] for this case)
    private val ComicChapterDto.isAccessibleByUser get() = isAuth == 1

    private fun String.toDate(): Long {
        if (contains("now", ignoreCase = true)) return Date().time

        val number = DIGIT_REGEX.find(this)?.value?.toIntOrNull() ?: return 0
        val cal = Calendar.getInstance()

        return when {
            contains("yr") -> cal.apply { add(Calendar.YEAR, -number) }.timeInMillis
            contains("mth") -> cal.apply { add(Calendar.MONTH, -number) }.timeInMillis
            contains("d") -> cal.apply { add(Calendar.DAY_OF_MONTH, -number) }.timeInMillis
            contains("h") -> cal.apply { add(Calendar.HOUR, -number) }.timeInMillis
            contains("min") -> cal.apply { add(Calendar.MINUTE, -number) }.timeInMillis
            else -> 0
        }
    }

    // Pages
    // TODO: Cleanup this block when ext-lib 1.4 is released
    override fun pageListRequest(chapter: SChapter): Request {
        val (comicId, chapterId) = chapter.getMangaAndChapterId
        return GET("$baseUrl/comic/$comicId/$chapterId", headers)
    }

    override fun fetchPageList(chapter: SChapter): Observable<List<Page>> {
        return client.newCall(internalPageListRequest(chapter))
            .asObservableSuccess()
            .map { response ->
                pageListParse(response)
            }
    }

    private fun internalPageListRequest(chapter: SChapter): Request {
        val (comicId, chapterId) = chapter.getMangaAndChapterId
        return pageListRequest(comicId, chapterId)
    }

    private val pageRequestHeaders by lazy {
        headers.newBuilder().set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:108.0) Gecko/20100101 Firefox/108.0").build()
    }

    private fun pageListRequest(comicId: String, chapterId: String): Request {
        return GET("$baseUrl/comic/$comicId/$chapterId", pageRequestHeaders)
    }

    data class ChapterPage(
        val id: String,
        val url: String
    )

    // LinkedHashMap with a capacity of 25. When exceeding the capacity the oldest entry is removed.
    private val chapterPageCache = object : LinkedHashMap<String, List<ChapterPage>>() {

        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<ChapterPage>>?): Boolean {
            return size > 25
        }
    }

    override fun pageListParse(response: Response): List<Page> {
        val document = response.asJsoup()
        val chapterId = response.request.url.pathSegments[2]
        return document.parseToChapterPage(chapterId).mapIndexed { i, chapterPage ->
            Page(i, imageUrl = chapterPage.url)
        }
    }

    private fun Document.parseToChapterPage(chapterId: String): List<ChapterPage> {
        return select("#comicPageContainer img").map {
            ChapterPage(
                id = it.attr("data-page"),
                url = it.attr("data-original")
            )
        }.also { chapterPageCache[chapterId] = it }
    }

    override fun imageUrlParse(response: Response): String = throw UnsupportedOperationException("Not Used")

    override fun getFilterList() = FilterList(
        Filter.Header("NOTE: Ignored if using text search!"),
        Filter.Separator(),
        ContentStatusFilter(),
        SortByFilter(),
        GenreFilter()
    )

    private val SManga.getId: String
        get() {
            if (url.toLongOrNull() == null) throw Exception(MIGRATE_MESSAGE)
            return url
        }

    private val SChapter.getMangaAndChapterId: Pair<String, String>
        get() {
            val (comicId, chapterId) = url.split(":")
            if (listOf(comicId, chapterId).any { it.toLongOrNull() == null }) throw Exception(MIGRATE_MESSAGE)
            return comicId to chapterId
        }

    private fun getCoverUrl(comicId: String): String {
        return "$baseCoverURl/bookcover/$comicId/0/600.jpg"
    }

    private fun csrfTokenInterceptor(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val originalRequestUrl = originalRequest.url
        if (!originalRequestUrl.toString().contains(BASE_API_ENDPOINT)) return chain.proceed(originalRequest)

        val csrfToken = originalRequest.header("cookie")
            ?.takeIf { csrfTokenName in it }
            ?.substringAfter("$csrfTokenName=")
            ?.substringBefore(";")
            ?: throw IOException("'$csrfTokenName' cookie not found.\nOpen in webview to set it.")

        val newUrl = originalRequestUrl.newBuilder()
            .addQueryParameter(csrfTokenName, csrfToken)
            .build()

        val newRequest = originalRequest.newBuilder().url(newUrl).build()
        return chain.proceed(newRequest)
    }

    private fun expiredImageUrlInterceptor(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val originalRequestUrl = originalRequest.url

        // If original request is not a page url or the url is still valid we just continue with og request
        if (!originalRequestUrl.toString().contains(baseCdnUrl) || isPageUrlStillValid(originalRequestUrl))
            return chain.proceed(originalRequest)

        val (_, comicId, chapterId, pageFileName) = originalRequest.url.pathSegments

        // Page url is not valid anymore so we check if cache has updated one
        val pageId = pageFileName.substringBefore("!")
        val cachedPageUrl = chapterPageCache[chapterId]?.firstOrNull { it.id == pageId }?.url
        if (cachedPageUrl != null && isPageUrlStillValid(cachedPageUrl.toHttpUrl())) return chain.proceed(originalRequest)

        // Time to get it from site
        val pageListResponse = chain.proceed(pageListRequest(comicId, chapterId))
        val chapterPages = pageListResponse.asJsoup().parseToChapterPage(chapterId)
        pageListResponse.close()

        val newPageUrl = chapterPages.firstOrNull { it.id == pageId }?.url?.toHttpUrl()
            ?: throw IOException("Couldn't regenerate expired image url")

        val newRequest = originalRequest.newBuilder().url(newPageUrl).build()
        return chain.proceed(newRequest)
    }

    private fun isPageUrlStillValid(imageUrl: HttpUrl): Boolean {
        val urlGenerationTime = imageUrl.queryParameter("t")?.toLongOrNull()
            ?: throw IOException("Parameter 't' missing from page url or isn't a long")

        // Urls are valid for 10 minutes after generation. We check for 9min and 30s just to be safe
        return (Date().time / 1000) - urlGenerationTime <= 570
    }

    private inline fun <reified T> Response.checkAndParseAs(): T = use {
        val parsed = json.decodeFromString<ResponseDto<T>>(it.body?.string().orEmpty())
        if (parsed.code != 0) error("Error ${parsed.code}: ${parsed.msg}")
        requireNotNull(parsed.data) { "Response data is null" }
    }

    private inline fun <reified T> List<*>.firstInstanceOrNull() = firstOrNull { it is T } as? T

    companion object {
        private const val BASE_API_ENDPOINT = "/go/pcm"

        private const val QUERY_SEARCH_PATH = "/search/result"
        private const val FILTER_SEARCH_PATH = "/category/categoryAjax"

        private const val MIGRATE_MESSAGE = "Migrate this entry from \"Webnovel.com\" to \"Webnovel.com\" to update url"

        private val DIGIT_REGEX = "(\\d+)".toRegex()

        private const val csrfTokenName = "_csrfToken"
    }
}
