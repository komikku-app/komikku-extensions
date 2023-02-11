package eu.kanade.tachiyomi.extension.ru.risensteam

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.asObservableSuccess
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import rx.Observable
import uy.kohesive.injekt.injectLazy
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale

class RisensTeam : HttpSource() {

    override val name = "Risens Team"

    override val baseUrl = "https://risens.team"

    override val lang = "ru"

    override val supportsLatest = false

    override val versionId: Int = 2

    private val json: Json by injectLazy()

    // Popular (source only returns manga sorted by latest)

    override fun popularMangaRequest(page: Int): Request {
        return GET("https://risens.team/api/title/list?type=1", headers)
    }

    private fun mangaFromJson(json: JsonElement): SManga {
        return SManga.create().apply {
            url = "${json.jsonObject["id"]!!.jsonPrimitive.int}/${json.jsonObject["furl"]!!.jsonPrimitive.content}"
            title = json.jsonObject["title"]!!.jsonPrimitive.content
            thumbnail_url = baseUrl + json.jsonObject["poster"]!!.jsonPrimitive.content
            description = json.jsonObject["description"]!!.jsonPrimitive.contentOrNull
            status = try { if (json.jsonObject["active"]!!.jsonPrimitive.int == 1) SManga.ONGOING else SManga.UNKNOWN } catch (_: Exception) { SManga.UNKNOWN }
        }
    }

    override fun popularMangaParse(response: Response): MangasPage {
        val mangas = json.decodeFromString<JsonArray>(response.body.string())
            .map { json -> mangaFromJson(json) }

        return MangasPage(mangas, false)
    }

    // Latest

    override fun latestUpdatesRequest(page: Int): Request = throw UnsupportedOperationException("Not used")
    override fun latestUpdatesParse(response: Response): MangasPage = throw UnsupportedOperationException("Not used")

    // Search

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        val rbody =
            """{"queryString":"$query","limit":3}""".toRequestBody("application/json;charset=utf-8".toMediaTypeOrNull())
        return POST("$baseUrl/api/title/search", headers, rbody)
    }

    override fun searchMangaParse(response: Response): MangasPage = popularMangaParse(response)

    // Details

    override fun fetchMangaDetails(manga: SManga): Observable<SManga> {
        return client.newCall(apiMangaDetailsRequest(manga))
            .asObservableSuccess()
            .map { response ->
                mangaDetailsParse(response).apply { initialized = true }
            }
    }

    private fun apiMangaDetailsRequest(manga: SManga): Request {
        return GET("$baseUrl/api/title/show/${manga.url.substringBefore("/")}")
    }

    override fun mangaDetailsRequest(manga: SManga): Request {
        return GET("$baseUrl/title/${manga.url}")
    }

    override fun mangaDetailsParse(response: Response): SManga {
        return mangaFromJson(json.decodeFromString<JsonObject>(response.body.string()))
    }

    // Chapters

    override fun chapterListRequest(manga: SManga): Request = apiMangaDetailsRequest(manga)

    override fun chapterListParse(response: Response): List<SChapter> {
        return json.decodeFromString<JsonObject>(response.body.string())["entities"]!!.jsonArray.map { json ->
            SChapter.create().apply {
                url = json.jsonObject["id"]!!.jsonPrimitive.int.toString()
                name = listOfNotNull(json.jsonObject["label"]!!.jsonPrimitive.contentOrNull, json.jsonObject["name"]!!.jsonPrimitive.contentOrNull).joinToString(" - ")
                date_upload = json.jsonObject["updated_at"]!!.toDate()
            }
        }
    }

    private val simpleDateFormat by lazy {
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    }

    private fun JsonElement.toDate(): Long {
        val date = this.jsonPrimitive.contentOrNull ?: return 0
        return try {
            simpleDateFormat.parse(date)?.time ?: 0
        } catch (e: ParseException) {
            0
        }
    }

    // Pages

    override fun pageListRequest(chapter: SChapter): Request {
        return GET("$baseUrl/api/yandex/chapter/${chapter.url}", headers)
    }

    override fun pageListParse(response: Response): List<Page> {
        return json.decodeFromString<JsonArray>(response.body.string())
            .mapIndexed { i, json -> Page(i, "", json.jsonPrimitive.content) }
    }

    override fun imageUrlParse(response: Response): String = throw UnsupportedOperationException("Not used")
}
