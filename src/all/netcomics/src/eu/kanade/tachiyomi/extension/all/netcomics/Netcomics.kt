package eu.kanade.tachiyomi.extension.all.netcomics

import android.app.Application
import android.net.Uri
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.PreferenceScreen
import androidx.preference.SwitchPreferenceCompat
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.asObservable
import eu.kanade.tachiyomi.source.ConfigurableSource
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Response
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.Calendar

class Netcomics(
    override val lang: String,
    private val site: String,
) : ConfigurableSource, HttpSource() {
    override val name = "NETCOMICS"

    override val baseUrl = "https://www.netcomics.com"

    override val supportsLatest = true

    private val json by lazy { Injekt.get<Json>() }

    private val preferences by lazy {
        Injekt.get<Application>().getSharedPreferences("source_$id", 0x0000)!!
    }

    private val adult by lazy {
        if (preferences.getBoolean("18+", false)) "Y" else "N"
    }

    private val token by lazy {
        preferences.getString("token", "")!!
    }

    private val quality by lazy {
        preferences.getString("quality", "625")!!
    }

    private val did by lazy {
        System.currentTimeMillis().toString()
    }

    private val apiUri by lazy { Uri.parse(API_URL) }

    private val apiHeaders by lazy {
        headers.newBuilder()
            .set("Origin", baseUrl)
            .set("platform", "android")
            .set("adult", adult)
            .set("token", token)
            .set("site", site)
            .set("did", did)
            .build()
    }

    private val day by lazy {
        when (Calendar.getInstance()[Calendar.DAY_OF_WEEK]) {
            Calendar.MONDAY -> "1"
            Calendar.TUESDAY -> "2"
            Calendar.WEDNESDAY -> "3"
            Calendar.THURSDAY -> "4"
            Calendar.FRIDAY -> "5"
            else -> ""
        }
    }

    // Request the real URL for the webview
    override fun mangaDetailsRequest(manga: SManga) =
        GET("$baseUrl/$site/comic/${manga.slug}", headers)

    override fun searchMangaParse(response: Response) =
        response.data<List<Title>>().ifEmpty {
            error("No more pages")
        }.map {
            SManga.create().apply {
                url = it.slug
                title = it.toString()
                genre = it.genres
                author = it.authors
                artist = it.artists
                description = it.description
                thumbnail_url = it.thumbnail
                status = when {
                    it.isCompleted -> SManga.COMPLETED
                    else -> SManga.ONGOING
                }
            }
        }.run { MangasPage(this, size == 20) }

    override fun chapterListParse(response: Response) =
        response.data<List<Chapter>>().map {
            SChapter.create().apply {
                url = it.path
                name = it.toString()
                date_upload = it.timestamp
                chapter_number = it.number
            }
        }

    override fun pageListParse(response: Response) =
        response.data<PageList>().map {
            Page(it.seq, "", it.toString())
        }

    override fun fetchLatestUpdates(page: Int) =
        apiUri.fetch("title", ::searchMangaParse) {
            appendEncodedPath("new")
            appendQueryParameter("no", page.toString())
            appendQueryParameter("size", "20")
            appendQueryParameter("day", day)
        }

    override fun fetchPopularManga(page: Int) =
        apiUri.fetch("title", ::searchMangaParse) {
            appendEncodedPath("free")
            appendQueryParameter("no", page.toString())
            appendQueryParameter("size", "20")
        }

    override fun fetchSearchManga(page: Int, query: String, filters: FilterList) =
        apiUri.fetch("title", ::searchMangaParse) {
            if (query.isNotBlank()) {
                appendEncodedPath("search/text")
                appendQueryParameter("text", query)
            } else {
                appendEncodedPath("genre")
                appendQueryParameter("genre", filters.genre)
            }
            appendQueryParameter("no", page.toString())
            appendQueryParameter("size", "20")
        }

    override fun fetchMangaDetails(manga: SManga) =
        rx.Observable.just(manga.apply { initialized = true })!!

    override fun fetchChapterList(manga: SManga) =
        apiUri.fetch("chapter", ::chapterListParse) {
            appendEncodedPath("list")
            appendEncodedPath(manga.id)
            appendEncodedPath("rent")
        }

    override fun fetchPageList(chapter: SChapter) =
        apiUri.fetch("chapter", ::pageListParse) {
            appendEncodedPath("viewer")
            appendEncodedPath(quality)
            appendEncodedPath(chapter.url)
        }

    override fun getFilterList() =
        FilterList(GenreFilter.NOTE, GenreFilter())

    override fun setupPreferenceScreen(screen: PreferenceScreen) {
        SwitchPreferenceCompat(screen.context).apply {
            key = "18+"
            title = "Show 18+"
            summaryOff = "18+ OFF"
            summaryOn = "18+ ON"
            setDefaultValue(false)

            setOnPreferenceChangeListener { _, newValue ->
                preferences.edit().putBoolean(key, newValue as Boolean).commit()
            }
        }.let(screen::addPreference)

        // TODO: grab from the webview somehow
        EditTextPreference(screen.context).apply {
            key = "token"
            title = "API key"
            dialogTitle = "localStorage['ncx.user.token']"

            setOnPreferenceChangeListener { _, newValue ->
                preferences.edit().putString(key, newValue as String).commit()
            }
        }.let(screen::addPreference)

        ListPreference(screen.context).apply {
            key = "quality"
            title = "Image quality"
            summary = "%s"
            entries = arrayOf("HD", "Medium")
            entryValues = arrayOf("1024", "625")
            setDefaultValue("625")

            setOnPreferenceChangeListener { _, newValue ->
                preferences.edit().putString(key, newValue as String).commit()
            }
        }.let(screen::addPreference)
    }

    override fun latestUpdatesRequest(page: Int) =
        throw UnsupportedOperationException("Not used")

    override fun popularMangaRequest(page: Int) =
        throw UnsupportedOperationException("Not used")

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList) =
        throw UnsupportedOperationException("Not used")

    override fun chapterListRequest(manga: SManga) =
        throw UnsupportedOperationException("Not used")

    override fun pageListRequest(chapter: SChapter) =
        throw UnsupportedOperationException("Not used")

    override fun latestUpdatesParse(response: Response) =
        throw UnsupportedOperationException("Not used")

    override fun popularMangaParse(response: Response) =
        throw UnsupportedOperationException("Not used")

    override fun mangaDetailsParse(response: Response) =
        throw UnsupportedOperationException("Not used")

    override fun imageUrlParse(response: Response) =
        throw UnsupportedOperationException("Not used")

    private inline val SManga.slug: String
        get() = url.substringBefore('|')

    private inline val SManga.id: String
        get() = url.substringAfter('|')

    private inline val FilterList.genre: String
        get() = find { it is GenreFilter }?.toString() ?: ""

    private inline fun <reified T> Response.data() =
        json.decodeFromJsonElement<T>(
            json.parseToJsonElement(body.string()).run {
                jsonObject["data"] ?: throw Error(
                    jsonObject["message"]!!.jsonPrimitive.content,
                )
            },
        )

    private inline fun <R> Uri.fetch(
        path: String,
        noinline parse: (Response) -> R,
        block: Uri.Builder.() -> Uri.Builder,
    ) = buildUpon().appendEncodedPath(path).let(block).toString().run {
        client.newCall(GET(this, apiHeaders)).asObservable().map(parse)!!
    }
}
