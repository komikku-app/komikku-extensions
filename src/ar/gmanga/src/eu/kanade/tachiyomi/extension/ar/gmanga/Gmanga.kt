package eu.kanade.tachiyomi.extension.ar.gmanga

import androidx.preference.PreferenceScreen
import eu.kanade.tachiyomi.extension.ar.gmanga.GmangaPreferences.Companion.PREF_CHAPTER_LISTING
import eu.kanade.tachiyomi.extension.ar.gmanga.GmangaPreferences.Companion.PREF_CHAPTER_LISTING_SHOW_POPULAR
import eu.kanade.tachiyomi.lib.ratelimit.RateLimitInterceptor
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.POST
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
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.float
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import uy.kohesive.injekt.injectLazy

class Gmanga : ConfigurableSource, HttpSource() {

    private val domain: String = "gmanga.me"

    override val baseUrl: String = "https://$domain"

    override val lang: String = "ar"

    override val name: String = "GMANGA"

    override val supportsLatest: Boolean = true

    private val json: Json by injectLazy()

    private val preferences = GmangaPreferences(id)

    private val rateLimitInterceptor = RateLimitInterceptor(4)

    override val client: OkHttpClient = network.client.newBuilder()
        .addNetworkInterceptor(rateLimitInterceptor)
        .build()

    override fun headersBuilder() = Headers.Builder().apply {
        add("User-Agent", USER_AGENT)
    }

    override fun setupPreferenceScreen(screen: PreferenceScreen) = preferences.setupPreferenceScreen(screen)

    override fun chapterListRequest(manga: SManga): Request {
        val mangaId = manga.url.substringAfterLast("/")
        return GET("$baseUrl/api/mangas/$mangaId/releases", headers)
    }

    @ExperimentalStdlibApi
    override fun chapterListParse(response: Response): List<SChapter> {
        val data = decryptResponse(response)

        val chapters: List<JsonArray> = buildList {
            val allChapters: ArrayList<JsonArray> = ArrayList()
            data["rows"]!!.jsonArray[0].jsonObject["rows"]!!.jsonArray.forEach { release ->
                val chapter = data["rows"]!!.jsonArray[2].jsonObject["rows"]!!.jsonArray.filter { it.jsonArray[0] == release.jsonArray[4] }
                allChapters.addAll(chapter.map { it.jsonArray })
                val team = data["rows"]!!.jsonArray[1].jsonObject["rows"]!!.jsonArray.filter { it.jsonArray[0] == release.jsonArray[5] }
                allChapters.addAll(team.map { it.jsonArray })
                allChapters.add(release.jsonArray)
            }

            when (preferences.getString(PREF_CHAPTER_LISTING)) {
                PREF_CHAPTER_LISTING_SHOW_POPULAR -> addAll(
                    allChapters.groupBy { it.jsonArray[4].jsonPrimitive.float }
                        .map { (_: Float, versions: List<JsonArray>) -> versions.maxByOrNull { it[5].jsonPrimitive.float }!! }
                )
                else -> addAll(allChapters)
            }
        }

        return chapters.map {
            SChapter.create().apply {
                chapter_number = it[8].jsonPrimitive.float

                val chapterName = it[10].jsonPrimitive.content.let { if (it.trim() != "") " - $it" else "" }

                url = "/r/${it[0]}"
                name = "${chapter_number.let { if (it % 1 > 0) it else it.toInt() }}$chapterName"
                date_upload = it[2].jsonPrimitive.long * 1000
                scanlator = it[13].jsonPrimitive.content
            }
        }
    }

    override fun imageUrlParse(response: Response): String = throw UnsupportedOperationException("Not used")

    override fun latestUpdatesParse(response: Response): MangasPage {
        val data = json.decodeFromString<JsonObject>(response.asJsoup().select(".js-react-on-rails-component").html())
        return MangasPage(
            data["mangaDataAction"]!!.jsonObject["newMangas"]!!.jsonArray.map {
                SManga.create().apply {
                    url = "/mangas/${it.jsonObject["id"]!!.jsonPrimitive.content}"
                    title = it.jsonObject["title"]!!.jsonPrimitive.content

                    thumbnail_url = it.jsonObject["cover"]!!.jsonPrimitive.contentOrNull?.let { coverFileName ->
                        val thumbnail = "medium_${coverFileName.substringBeforeLast(".")}.webp"
                        "https://media.$domain/uploads/manga/cover/${it.jsonObject["id"]!!.jsonPrimitive.content}/$thumbnail"
                    }
                }
            },
            false
        )
    }

    override fun latestUpdatesRequest(page: Int): Request {
        return GET("$baseUrl/mangas/latest", headers)
    }

    override fun mangaDetailsParse(response: Response): SManga {
        val data = json.decodeFromString<JsonObject>(response.asJsoup().select(".js-react-on-rails-component").html())
        val mangaData = data["mangaDataAction"]!!.jsonObject["mangaData"]!!.jsonObject
        return SManga.create().apply {
            description = mangaData["summary"]!!.jsonPrimitive.contentOrNull ?: ""
            artist = mangaData["artists"]!!.jsonArray.joinToString(", ") { it.jsonObject["name"]!!.jsonPrimitive.content }
            author = mangaData["authors"]!!.jsonArray.joinToString(", ") { it.jsonObject["name"]!!.jsonPrimitive.content }
            genre = mangaData["categories"]!!.jsonArray.joinToString(", ") { it.jsonObject["name"]!!.jsonPrimitive.content }
        }
    }

    override fun pageListParse(response: Response): List<Page> {
        val url = response.request.url.toString()
        val data = json.decodeFromString<JsonObject>(response.asJsoup().select(".js-react-on-rails-component").html())
        val releaseData = data["readerDataAction"]!!.jsonObject["readerData"]!!.jsonObject["release"]!!.jsonObject

        val hasWebP = releaseData["webp_pages"]!!.jsonArray.size > 0
        return releaseData[if (hasWebP) "webp_pages" else "pages"]!!.jsonArray.map { it.jsonPrimitive.content }.mapIndexed { index, pageUri ->
            Page(
                index,
                "$url#page_$index",
                "https://media.$domain/uploads/releases/${releaseData["storage_key"]!!.jsonPrimitive.content}/hq${if (hasWebP) "_webp" else ""}/$pageUri"
            )
        }
    }

    override fun popularMangaParse(response: Response) = searchMangaParse(response)

    override fun popularMangaRequest(page: Int) = searchMangaRequest(page, "", getFilterList())

    override fun searchMangaParse(response: Response): MangasPage {
        val data = decryptResponse(response)
        val mangas = data["mangas"]!!.jsonArray
        return MangasPage(
            mangas.jsonArray.map {
                SManga.create().apply {
                    url = "/mangas/${it.jsonObject["id"]!!.jsonPrimitive.content}"
                    title = it.jsonObject["title"]!!.jsonPrimitive.content
                    val thumbnail = "medium_${it.jsonObject["cover"]!!.jsonPrimitive.content.substringBeforeLast(".")}.webp"
                    thumbnail_url = "https://media.$domain/uploads/manga/cover/${it.jsonObject["id"]!!.jsonPrimitive.content}/$thumbnail"
                }
            },
            mangas.size == 50
        )
    }

    private fun decryptResponse(response: Response): JsonObject {
        val encryptedData = json.decodeFromString<JsonObject>(response.body!!.string())["data"]!!.jsonPrimitive.content
        val decryptedData = decrypt(encryptedData)
        return json.decodeFromString(decryptedData)
    }

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        return GmangaFilters.buildSearchPayload(page, query, if (filters.isEmpty()) getFilterList() else filters).let {
            val body = it.toString().toRequestBody(MEDIA_TYPE)
            POST("$baseUrl/api/mangas/search", headers, body)
        }
    }

    override fun getFilterList() = GmangaFilters.getFilterList()

    companion object {
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/81.0.4044.122 Safari/537.36"
        private val MEDIA_TYPE = "application/json; charset=utf-8".toMediaTypeOrNull()
    }
}
