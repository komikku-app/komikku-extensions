package eu.kanade.tachiyomi.extension.all.hentaihand

import android.app.Application
import android.content.SharedPreferences
import android.text.InputType
import android.widget.Toast
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.POST
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
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import rx.Observable
import rx.schedulers.Schedulers
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy
import java.text.SimpleDateFormat
import java.util.Locale

abstract class HentaiHand(
    override val lang: String,
    private val hhLangId: Int? = null,
    extraName: String = ""
) : ConfigurableSource, HttpSource() {

    override val baseUrl: String = "https://hentaihand.com"
    override val name: String = "HentaiHand$extraName"
    override val supportsLatest = true

    override val client: OkHttpClient = network.cloudflareClient.newBuilder()
        .addInterceptor { authIntercept(it) }
        .build()

    private val json: Json by injectLazy()

    private fun slugToUrl(json: JsonObject) = json["slug"]!!.jsonPrimitive.content.prependIndent("/en/comic/")

    private fun jsonArrayToString(arrayKey: String, obj: JsonObject): String? {
        val array = obj[arrayKey]!!.jsonArray
        if (array.isEmpty()) return null
        return array.joinToString(", ") {
            it.jsonObject["name"]!!.jsonPrimitive.content
        }
    }

    // Popular

    override fun popularMangaParse(response: Response): MangasPage {
        val jsonResponse = json.parseToJsonElement(response.body!!.string())
        val mangaList = jsonResponse.jsonObject["data"]!!.jsonArray.map {
            val obj = it.jsonObject
            SManga.create().apply {
                url = slugToUrl(obj)
                title = obj["title"]!!.jsonPrimitive.content
                thumbnail_url = obj["thumb_url"]!!.jsonPrimitive.content
            }
        }
        val hasNextPage = jsonResponse.jsonObject["next_page_url"]!!.jsonPrimitive.content.isNotEmpty()
        return MangasPage(mangaList, hasNextPage)
    }

    override fun popularMangaRequest(page: Int): Request {
        val url = "$baseUrl/api/comics?page=$page&sort=popularity&order=desc&duration=all"
        return GET(if (hhLangId == null) url else ("$url&languages=$hhLangId"))
    }

    // Latest

    override fun latestUpdatesParse(response: Response): MangasPage = popularMangaParse(response)

    override fun latestUpdatesRequest(page: Int): Request {
        val url = "$baseUrl/api/comics?page=$page&sort=uploaded_at&order=desc&duration=week"
        return GET(if (hhLangId == null) url else ("$url&languages=$hhLangId"))
    }

    // Search

    override fun searchMangaParse(response: Response): MangasPage = popularMangaParse(response)

    private fun lookupFilterId(query: String, uri: String): Int? {
        // filter query needs to be resolved to an ID
        return client.newCall(GET("$baseUrl/api/$uri?q=$query"))
            .asObservableSuccess()
            .subscribeOn(Schedulers.io())
            .map { response ->
                // Returns the first matched id, or null if there are no results
                val idList = json.parseToJsonElement(response.body!!.string()).jsonObject["data"]!!.jsonArray.map {
                    it.jsonObject["id"]!!.jsonPrimitive.content
                }
                if (idList.isEmpty()) return@map null
                else idList.first().toInt()
            }.toBlocking().first()
    }

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {

        val url = "$baseUrl/api/comics".toHttpUrlOrNull()!!.newBuilder()
            .addQueryParameter("page", page.toString())
            .addQueryParameter("q", query)

        if (hhLangId != null)
            url.addQueryParameter("languages", hhLangId.toString())

        (if (filters.isEmpty()) getFilterList() else filters).forEach { filter ->
            when (filter) {
                is SortFilter -> url.addQueryParameter("sort", getSortPairs()[filter.state].second)
                is OrderFilter -> url.addQueryParameter("order", getOrderPairs()[filter.state].second)
                is DurationFilter -> url.addQueryParameter("duration", getDurationPairs()[filter.state].second)
                is AttributesGroupFilter -> filter.state.forEach {
                    if (it.state) url.addQueryParameter("attributes", it.value)
                }
                is LookupFilter -> {
                    filter.state.split(",").map { it.trim() }.filter { it.isNotBlank() }.map {
                        lookupFilterId(it, filter.uri) ?: throw Exception("No ${filter.singularName} \"$it\" was found")
                    }.forEach {
                        if (!(filter.uri == "languages" && it == hhLangId))
                            url.addQueryParameter(filter.uri, it.toString())
                    }
                }
                else -> {}
            }
        }

        return GET(url.toString())
    }

    // Details

    private fun mangaDetailsApiRequest(manga: SManga): Request {
        val slug = manga.url.removePrefix("/en/comic/")
        return GET("$baseUrl/api/comics/$slug")
    }

    override fun fetchMangaDetails(manga: SManga): Observable<SManga> {
        return client.newCall(mangaDetailsApiRequest(manga))
            .asObservableSuccess()
            .map { mangaDetailsParse(it).apply { initialized = true } }
    }

    override fun mangaDetailsParse(response: Response): SManga {
        val obj = json.parseToJsonElement(response.body!!.string()).jsonObject
        return SManga.create().apply {
            url = slugToUrl(obj)
            title = obj["title"]!!.jsonPrimitive.content
            thumbnail_url = obj["thumb_url"]!!.jsonPrimitive.content
            artist = jsonArrayToString("artists", obj)
            author = jsonArrayToString("authors", obj) ?: artist
            genre = listOfNotNull(jsonArrayToString("tags", obj), jsonArrayToString("relationships", obj)).joinToString(", ")
            status = SManga.COMPLETED

            description = listOf(
                Pair("Alternative Title", obj["alternative_title"]!!.jsonPrimitive.content),
                Pair("Groups", jsonArrayToString("groups", obj)),
                Pair("Description", obj["description"]!!.jsonPrimitive.content),
                Pair("Pages", obj["pages"]!!.jsonPrimitive.content),
                Pair("Category", obj["category"]!!.jsonObject["name"]!!.jsonPrimitive.content),
                Pair("Language", obj["language"]!!.jsonObject["name"]!!.jsonPrimitive.content),
                Pair("Parodies", jsonArrayToString("parodies", obj)),
                Pair("Characters", jsonArrayToString("characters", obj))
            ).filter { !it.second.isNullOrEmpty() }.joinToString("\n\n") { "${it.first}: ${it.second}" }
        }
    }

    // Chapters

    override fun chapterListRequest(manga: SManga): Request = mangaDetailsApiRequest(manga)

    override fun chapterListParse(response: Response): List<SChapter> {
        val obj = json.parseToJsonElement(response.body!!.string()).jsonObject
        return listOf(
            SChapter.create().apply {
                url = "/en/comic/${obj["slug"]!!.jsonPrimitive.content}/reader/1"
                name = "Chapter"
                date_upload = DATE_FORMAT.parse(obj["uploaded_at"]!!.jsonPrimitive.content)?.time ?: 0
                chapter_number = 1f
            }
        )
    }

    // Pages

    override fun pageListRequest(chapter: SChapter): Request {
        val slug = chapter.url.removePrefix("/en/comic/").removeSuffix("/reader/1")
        return GET("$baseUrl/api/comics/$slug/images")
    }

    override fun pageListParse(response: Response): List<Page> =
        json.parseToJsonElement(response.body!!.string()).jsonObject["images"]!!.jsonArray.map {
            val imgObj = it.jsonObject
            val index = imgObj["page"]!!.jsonPrimitive.int
            val imgUrl = imgObj["source_url"]!!.jsonPrimitive.content
            Page(index, "", imgUrl)
        }

    override fun imageUrlParse(response: Response): String = throw UnsupportedOperationException("Not used")

    // Authorization

    private fun authIntercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (username.isEmpty() or password.isEmpty()) {
            return chain.proceed(request)
        }

        if (token.isEmpty()) {
            token = this.login(chain, username, password)
        }
        val authRequest = request.newBuilder()
            .addHeader("Authorization", "Bearer $token")
            .build()
        return chain.proceed(authRequest)
    }

    private fun login(chain: Interceptor.Chain, username: String, password: String): String {
        val jsonObject = buildJsonObject {
            put("username", username)
            put("password", password)
            put("remember_me", true)
        }
        val body = jsonObject.toString().toRequestBody(MEDIA_TYPE)
        val response = chain.proceed(POST("$baseUrl/api/login", headers, body))
        if (response.code == 401) {
            throw Exception("Failed to login, check if username and password are correct")
        }

        if (response.body == null)
            throw Exception("Login response body is empty")
        try {
            // Returns access token as a string, unless unparseable
            return json.parseToJsonElement(response.body!!.string()).jsonObject["auth"]!!.jsonObject["access-token"]!!.jsonPrimitive.content
        } catch (e: IllegalArgumentException) {
            throw Exception("Cannot parse login response body")
        }
    }

    private var token: String = ""
    private val username by lazy { getPrefUsername() }
    private val password by lazy { getPrefPassword() }

    // Preferences

    private val preferences: SharedPreferences by lazy {
        Injekt.get<Application>().getSharedPreferences("source_$id", 0x0000)
    }

    override fun setupPreferenceScreen(screen: androidx.preference.PreferenceScreen) {
        screen.addPreference(screen.editTextPreference(USERNAME_TITLE, USERNAME_DEFAULT, username))
        screen.addPreference(screen.editTextPreference(PASSWORD_TITLE, PASSWORD_DEFAULT, password, true))
    }

    private fun androidx.preference.PreferenceScreen.editTextPreference(title: String, default: String, value: String, isPassword: Boolean = false): androidx.preference.EditTextPreference {
        return androidx.preference.EditTextPreference(context).apply {
            key = title
            this.title = title
            summary = value
            this.setDefaultValue(default)
            dialogTitle = title

            if (isPassword) {
                setOnBindEditTextListener {
                    it.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                }
            }
            setOnPreferenceChangeListener { _, newValue ->
                try {
                    val res = preferences.edit().putString(title, newValue as String).commit()
                    Toast.makeText(context, "Restart Tachiyomi to apply new setting.", Toast.LENGTH_LONG).show()
                    res
                } catch (e: Exception) {
                    e.printStackTrace()
                    false
                }
            }
        }
    }

    private fun getPrefUsername(): String = preferences.getString(USERNAME_TITLE, USERNAME_DEFAULT)!!
    private fun getPrefPassword(): String = preferences.getString(PASSWORD_TITLE, PASSWORD_DEFAULT)!!

    // Filters

    private class SortFilter(sortPairs: List<Pair<String, String>>) : Filter.Select<String>("Sort By", sortPairs.map { it.first }.toTypedArray())
    private class OrderFilter(orderPairs: List<Pair<String, String>>) : Filter.Select<String>("Order By", orderPairs.map { it.first }.toTypedArray())
    private class DurationFilter(durationPairs: List<Pair<String, String>>) : Filter.Select<String>("Duration", durationPairs.map { it.first }.toTypedArray())
    private class AttributeFilter(name: String, val value: String) : Filter.CheckBox(name)
    private class AttributesGroupFilter(attributePairs: List<Pair<String, String>>) : Filter.Group<AttributeFilter>("Attributes", attributePairs.map { AttributeFilter(it.first, it.second) })

    private class CategoriesFilter : LookupFilter("Categories", "categories", "category")
    private class TagsFilter : LookupFilter("Tags", "tags", "tag")
    private class ArtistsFilter : LookupFilter("Artists", "artists", "artist")
    private class GroupsFilter : LookupFilter("Groups", "groups", "group")
    private class CharactersFilter : LookupFilter("Characters", "characters", "character")
    private class ParodiesFilter : LookupFilter("Parodies", "parodies", "parody")
    private class LanguagesFilter : LookupFilter("Other Languages", "languages", "language")
    open class LookupFilter(name: String, val uri: String, val singularName: String) : Filter.Text(name)

    override fun getFilterList() = FilterList(
        SortFilter(getSortPairs()),
        OrderFilter(getOrderPairs()),
        DurationFilter(getDurationPairs()),
        Filter.Header("Separate terms with commas (,)"),
        CategoriesFilter(),
        TagsFilter(),
        ArtistsFilter(),
        GroupsFilter(),
        CharactersFilter(),
        ParodiesFilter(),
        LanguagesFilter(),
        AttributesGroupFilter(getAttributePairs())
    )

    private fun getSortPairs() = listOf(
        Pair("Upload Date", "uploaded_at"),
        Pair("Title", "title"),
        Pair("Pages", "pages"),
        Pair("Favorites", "favorites"),
        Pair("Popularity", "popularity")
    )

    private fun getOrderPairs() = listOf(
        Pair("Descending", "desc"),
        Pair("Ascending", "asc")
    )

    private fun getDurationPairs() = listOf(
        Pair("Today", "day"),
        Pair("This Week", "week"),
        Pair("This Month", "month"),
        Pair("This Year", "year"),
        Pair("All Time", "all")
    )

    private fun getAttributePairs() = listOf(
        Pair("Translated", "translated"),
        Pair("Speechless", "speechless"),
        Pair("Rewritten", "rewritten")
    )

    companion object {
        private val DATE_FORMAT = SimpleDateFormat("yyyy-dd-MM", Locale.US)
        private val MEDIA_TYPE = "application/json; charset=utf-8".toMediaTypeOrNull()
        private const val USERNAME_TITLE = "Username"
        private const val USERNAME_DEFAULT = ""
        private const val PASSWORD_TITLE = "Password"
        private const val PASSWORD_DEFAULT = ""
    }
}
