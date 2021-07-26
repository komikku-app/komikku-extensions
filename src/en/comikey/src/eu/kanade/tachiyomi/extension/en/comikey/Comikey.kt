package eu.kanade.tachiyomi.extension.en.comikey

import android.app.Application
import android.content.SharedPreferences
import eu.kanade.tachiyomi.extension.en.comikey.dto.MangaDetailsDto
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.asObservableSuccess
import eu.kanade.tachiyomi.source.ConfigurableSource
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.util.asJsoup
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import rx.Observable
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.net.URLEncoder
import java.text.SimpleDateFormat

class Comikey : HttpSource(), ConfigurableSource {
    override val name = "Comikey"

    override val baseUrl = "https://comikey.com"

    private val apiUrl = "$baseUrl/sapi"

    override val lang = "en"

    override val supportsLatest = true

    override val client: OkHttpClient = network.cloudflareClient

    private val json = Json {
        isLenient = true
        ignoreUnknownKeys = true
    }

    companion object {
        const val SLUG_SEARCH_PREFIX = "slug:"
    }

    // Home page functions

    override fun popularMangaRequest(page: Int) = GET("$baseUrl/comics/?order=-views&page=$page", headers)

    override fun latestUpdatesRequest(page: Int) = GET("$baseUrl/comics/?page=$page", headers)

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        return GET("$baseUrl/comics/?q=${URLEncoder.encode(query, "utf-8")}&page=$page", headers)
    }

    override fun popularMangaParse(response: Response) = mangaParse(response)

    override fun latestUpdatesParse(response: Response) = mangaParse(response)

    override fun searchMangaParse(response: Response) = mangaParse(response)

    override fun fetchSearchManga(page: Int, query: String, filters: FilterList): Observable<MangasPage> {
        return if (query.startsWith(SLUG_SEARCH_PREFIX)) {
            val manga = SManga.create().apply {
                url = "/comics/" + query.removePrefix(SLUG_SEARCH_PREFIX)
            }
            return fetchMangaDetails(manga).map { mangaWithDetails ->
                MangasPage(listOf(mangaWithDetails), false)
            }
        } else {
            super.fetchSearchManga(page, query, filters)
        }
    }

    private fun mangaParse(response: Response): MangasPage {

        val responseJson = response.asJsoup()

        val mangaList = responseJson.select("section#series-list div.series-listing[data-view=list] > ul > li")
            .map {
                SManga.create().apply {
                    title = it.selectFirst("span.title a").text()
                    url = it.selectFirst("span.title a[href]").attr("href")

                    val subtitle = it.selectFirst("span.subtitle").text().removePrefix("by")
                    author = subtitle.substringBefore("|").trim()
                    artist = subtitle.substringAfter("|").trim()

                    thumbnail_url = it.selectFirst("div.image[style*=url(]")
                        ?.attr("style")
                        ?.substringAfter("url(")?.substringBefore(")")
                        ?: "https://comikey.com/static/images/svgs/no-cover.svg"

                    genre = it.select("div.categories > ul.category-listing > li > span.category-button")
                        .joinToString(", ") { el -> el.text() }

                    description = it.selectFirst("div.description").text()
                    status = SManga.UNKNOWN
                    initialized = true // we already have all of the fields
                }
            }
        // we have a next page if the "Next Page" button is not disabled
        val hasNextPage = responseJson.selectFirst("li.page-item.active ~ li.page-item.disabled") == null &&
            responseJson.selectFirst("li.page-item.active ~ li.page-item:not(.disabled)") != null

        return MangasPage(mangaList, hasNextPage)
    }

    // Manga page functions

    override fun fetchMangaDetails(manga: SManga): Observable<SManga> {
        return getMangaId(manga).flatMap { id ->
            client.newCall(mangaDetailsRequest(id))
                .asObservableSuccess()
                .map { response ->
                    mangaDetailsParse(response).apply { initialized = true }
                }
        }
    }

    private fun mangaDetailsRequest(id: Int) = GET("$apiUrl/comics/$id?format=json", headers)

    override fun mangaDetailsParse(response: Response): SManga {
        val details = json.decodeFromString<MangaDetailsDto>(response.body!!.string())
        return SManga.create().apply {
            title = details.name!!
            url = details.link!!
            author = details.author?.map { it?.name }?.joinToString(", ")
            artist = details.artist?.map { it?.name }?.joinToString(", ")
            thumbnail_url = details.cover
            genre = details.tags?.map { it?.name }?.joinToString(", ")
            description = details.excerpt + "\n\n" + details.description
            status = SManga.UNKNOWN
            initialized = true
        }
    }

    private fun getMangaId(manga: SManga): Observable<Int> {
        val mangaId = manga.url.trimEnd('/').substringAfterLast('/').toIntOrNull()
        return if (mangaId != null) {
            Observable.just(mangaId)
        } else {
            client.newCall(GET(baseUrl + manga.url, headers))
                .asObservableSuccess()
                .map { response ->
                    manga.url = response.asJsoup().selectFirst("meta[property=og:url]").attr("content")
                    manga.url.trimEnd('/').substringAfterLast('/').toInt()
                }
        }
    }

    private fun rssFeedRequest(mangaId: Int) = GET("$apiUrl/comics/$mangaId/feed.rss", headers)

    override fun fetchChapterList(manga: SManga): Observable<List<SChapter>> {
        val chapterList = getMangaId(manga).flatMap { mangaId ->
            client.newCall(rssFeedRequest(mangaId))
                .asObservableSuccess()
                .map { response ->
                    chapterListParse(response, mangaId)
                }
        }
        return if (preferences.getBoolean("filterOwnedChapter", false)) {
            chapterList.flatMap { it.filterChapterList() }
        } else {
            chapterList
        }
    }

    override fun chapterListRequest(manga: SManga) = throw UnsupportedOperationException("Not used (chapterListRequest)")

    override fun chapterListParse(response: Response) = throw UnsupportedOperationException("Not used (chapterListParse)")

    private fun chapterListParse(response: Response, mangaId: Int): List<SChapter> {
        return Jsoup.parse(response.body!!.string(), response.request.url.toString(), Parser.xmlParser())
            .select("channel > item").map { item ->
                SChapter.create().apply {
                    val chapterGuid = item.selectFirst("guid").text().substringAfterLast(':')
                    url = "$apiUrl/comics/$mangaId/read?format=json&content=$chapterGuid"
                    name = item.selectFirst("title").text()
                    date_upload = SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", java.util.Locale.US)
                        .parse(item.selectFirst("pubDate").text())
                        ?.time ?: 0L
                }
            }.reversed()
    }

    private data class IndexedChapter(val index: Int, val chapter: SChapter) : Comparable<IndexedChapter> {
        override fun compareTo(other: IndexedChapter) = this.index.compareTo(other.index)
    }

    // determine which chapters the user has access to, and which are locked behind a paywall
    private fun List<SChapter>.filterChapterList(): Observable<List<SChapter>> {
        return Observable.from(this.mapIndexed { index, chapter -> IndexedChapter(index, chapter) })
            .filterByObservable { (_, chapter) ->
                chapter.isAvailable()
            }.toSortedList()
            .map { indexed -> indexed.map { it.chapter } }
    }

    private fun <T> Observable<T>.filterByObservable(predicate: rx.functions.Func1<in T, Observable<Boolean>>): Observable<T> {
        return this.flatMap { item ->
            predicate.call(item)
                .first()
                .filter { it }
                .map { item }
        }
    }

    private fun SChapter.isAvailable(): Observable<Boolean> {
        return client.newCall(pageListRequest(this))
            .asObservableSuccess()
            .map { response ->
                response.body?.string()
                    ?.let { Json.parseToJsonElement(it) }
                    ?.jsonObject?.get("ok")
                    ?.jsonPrimitive?.booleanOrNull
                    ?: true // Default to displaying the chapter if we get an error
            }
    }

    // Chapter page functions

    private val urlForbidden = "https://fakeimg.pl/1800x2252/FFFFFF/000000/?font_size=120&text=This%20chapter%20is%20not%20available%20for%20free.%0A%0AIf%20you%20have%20purchased%20this%20chapter%2C%20please%20%0Aopen%20the%20website%20in%20web%20view%20and%20log%20in."

    override fun fetchPageList(chapter: SChapter): Observable<List<Page>> {
        return client.newCall(pageListRequest(chapter))
            .asObservableSuccess()
            .flatMap { response ->
                val request = getActualPageList(response)
                    ?: return@flatMap Observable.just(listOf(Page(0, urlForbidden, urlForbidden)))

                client.newCall(request)
                    .asObservableSuccess()
                    .map { responseActual ->
                        pageListParse(responseActual)
                    }
            }
    }

    override fun pageListRequest(chapter: SChapter) = GET(chapter.url, headers)

    private fun getActualPageList(response: Response): Request? {
        val element = Json.parseToJsonElement(response.body!!.string()).jsonObject
        val ok = element["ok"]?.jsonPrimitive?.booleanOrNull
        if (ok != null && !ok) {
            return null
        }
        val url = element["href"]?.jsonPrimitive?.content
        return GET(url!!, headers)
    }

    override fun pageListParse(response: Response): List<Page> {
        return Json.parseToJsonElement(response.body!!.string())
            .jsonObject["readingOrder"]!!
            .jsonArray.mapIndexed { index, element ->
                val url = element.jsonObject["href"]!!.jsonPrimitive.content
                Page(index, url, url)
            }
    }

    // the image url is always equal to the page url
    override fun fetchImageUrl(page: Page): Observable<String> = Observable.just(page.url)

    override fun imageUrlParse(response: Response) = throw UnsupportedOperationException("Not used (imageUrlParse)")

    // Preferences

    private val preferences: SharedPreferences by lazy {
        Injekt.get<Application>().getSharedPreferences("source_$id", 0x0000)
    }

    override fun setupPreferenceScreen(screen: androidx.preference.PreferenceScreen) {
        val filterOwnedChapterPref = androidx.preference.CheckBoxPreference(screen.context).apply {
            key = "filterOwnedChapter"
            title = "[Experimental] Only show free/owned chapters"
            setDefaultValue(false)

            setOnPreferenceChangeListener { _, newValue ->
                val checkValue = newValue as Boolean
                preferences.edit().putBoolean("filterOwnedChapter", checkValue).commit()
            }
        }

        screen.addPreference(filterOwnedChapterPref)
    }
}
