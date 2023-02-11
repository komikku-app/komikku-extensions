package eu.kanade.tachiyomi.extension.ca.fansubscat

import eu.kanade.tachiyomi.AppInfo
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.asObservableSuccess
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.float
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import rx.Observable
import uy.kohesive.injekt.injectLazy

class FansubsCat : HttpSource() {

    override val name = "Fansubs.cat"

    override val baseUrl = "https://manga.fansubs.cat"

    override val lang = "ca"

    override val supportsLatest = true

    override fun headersBuilder(): Headers.Builder = Headers.Builder()
        .add("User-Agent", "Tachiyomi/FansubsCat/${AppInfo.getVersionName()}")

    override val client: OkHttpClient = network.client

    private val json: Json by injectLazy()

    private val apiBaseUrl = "https://api.fansubs.cat"

    private fun parseMangaFromJson(response: Response): MangasPage {
        val jsonObject = json.decodeFromString<JsonObject>(response.body.string())

        val mangas = jsonObject["result"]!!.jsonArray.map { json ->
            SManga.create().apply {
                url = json.jsonObject["slug"]!!.jsonPrimitive.content
                title = json.jsonObject["name"]!!.jsonPrimitive.content
                thumbnail_url = json.jsonObject["thumbnail_url"]!!.jsonPrimitive.content
                author = json.jsonObject["author"]!!.jsonPrimitive.contentOrNull
                description = json.jsonObject["synopsis"]!!.jsonPrimitive.contentOrNull
                status = json.jsonObject["status"]!!.jsonPrimitive.content.toStatus()
                genre = json.jsonObject["genres"]!!.jsonPrimitive.contentOrNull
            }
        }

        return MangasPage(mangas, mangas.size >= 20)
    }

    private fun parseChapterListFromJson(response: Response): List<SChapter> {
        val jsonObject = json.decodeFromString<JsonObject>(response.body.string())

        return jsonObject["result"]!!.jsonArray.map { json ->
            SChapter.create().apply {
                url = json.jsonObject["id"]!!.jsonPrimitive.content
                name = json.jsonObject["title"]!!.jsonPrimitive.content
                chapter_number = json.jsonObject["number"]!!.jsonPrimitive.float
                scanlator = json.jsonObject["fansub"]!!.jsonPrimitive.content
                date_upload = json.jsonObject["created"]!!.jsonPrimitive.long
            }
        }
    }

    private fun parsePageListFromJson(response: Response): List<Page> {
        val jsonObject = json.decodeFromString<JsonObject>(response.body.string())

        return jsonObject["result"]!!.jsonArray.mapIndexed { i, it ->
            Page(i, it.jsonObject["url"]!!.jsonPrimitive.content, it.jsonObject["url"]!!.jsonPrimitive.content)
        }
    }

    // Popular

    override fun popularMangaRequest(page: Int): Request {
        return GET("$apiBaseUrl/manga/popular/$page", headers)
    }

    override fun popularMangaParse(response: Response): MangasPage = parseMangaFromJson(response)

    // Latest

    override fun latestUpdatesRequest(page: Int): Request {
        return GET("$apiBaseUrl/manga/recent/$page", headers)
    }

    override fun latestUpdatesParse(response: Response): MangasPage = parseMangaFromJson(response)

    // Search

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        val url = "$apiBaseUrl/manga/search/$page".toHttpUrlOrNull()!!.newBuilder()
            .addQueryParameter("query", query)
        return GET(url.toString(), headers)
    }

    override fun searchMangaParse(response: Response): MangasPage = parseMangaFromJson(response)

    // Details

    // Workaround to allow "Open in browser" to use the real URL
    override fun fetchMangaDetails(manga: SManga): Observable<SManga> =
        client.newCall(apiMangaDetailsRequest(manga)).asObservableSuccess()
            .map { mangaDetailsParse(it).apply { initialized = true } }

    // Return the real URL for "Open in browser"
    override fun mangaDetailsRequest(manga: SManga) = GET("$baseUrl/${manga.url}", headers)

    private fun apiMangaDetailsRequest(manga: SManga): Request {
        return GET("$apiBaseUrl/manga/details/${manga.url.substringAfterLast('/')}", headers)
    }

    override fun mangaDetailsParse(response: Response): SManga {
        val jsonObject = json.decodeFromString<JsonObject>(response.body.string())
        val resultObject = jsonObject.jsonObject["result"]!!.jsonObject

        return SManga.create().apply {
            url = resultObject["slug"]!!.jsonPrimitive.content
            title = resultObject["name"]!!.jsonPrimitive.content
            thumbnail_url = resultObject["thumbnail_url"]!!.jsonPrimitive.content
            author = resultObject["author"]!!.jsonPrimitive.contentOrNull
            description = resultObject["synopsis"]!!.jsonPrimitive.contentOrNull
            status = resultObject["status"]!!.jsonPrimitive.content.toStatus()
            genre = resultObject["genres"]!!.jsonPrimitive.contentOrNull
        }
    }

    private fun String?.toStatus() = when {
        this == null -> SManga.UNKNOWN
        this.contains("ongoing", ignoreCase = true) -> SManga.ONGOING
        this.contains("finished", ignoreCase = true) -> SManga.COMPLETED
        else -> SManga.UNKNOWN
    }

    // Chapters

    override fun chapterListRequest(manga: SManga): Request = GET("$apiBaseUrl/manga/chapters/${manga.url.substringAfterLast('/')}", headers)

    override fun chapterListParse(response: Response): List<SChapter> = parseChapterListFromJson(response)

    // Pages

    override fun pageListRequest(chapter: SChapter): Request = GET("$apiBaseUrl/manga/pages/${chapter.url}", headers)

    override fun pageListParse(response: Response): List<Page> = parsePageListFromJson(response)

    override fun imageUrlParse(response: Response): String = throw UnsupportedOperationException("Not used")
}
