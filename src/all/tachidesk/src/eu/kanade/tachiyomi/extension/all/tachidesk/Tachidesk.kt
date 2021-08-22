package eu.kanade.tachiyomi.extension.all.tachidesk

import android.app.Application
import android.content.SharedPreferences
import android.text.InputType
import android.util.Log
import android.widget.Toast
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceScreen
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
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.Request
import okhttp3.Response
import rx.Observable
import rx.Single
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy
import java.lang.RuntimeException

class Tachidesk : ConfigurableSource, HttpSource() {
    override val name = "Tachidesk"
    override val baseUrl by lazy { getPrefBaseUrl() }
    override val lang = "en"
    override val supportsLatest = false

    private val json: Json by injectLazy()

    // ------------- Popular Manga -------------

    override fun popularMangaRequest(page: Int): Request =
        GET("$checkedBaseUrl/api/v1/category/$defaultCategoryId")

    override fun popularMangaParse(response: Response): MangasPage =
        MangasPage(
            json.decodeFromString<List<MangaDataClass>>(response.body!!.string()).map {
                it.toSManga()
            },
            false
        )
    // ------------- Manga Details -------------

    override fun mangaDetailsRequest(manga: SManga) =
        GET("$checkedBaseUrl/api/v1/manga/${manga.url}/?onlineFetch=true")

    override fun mangaDetailsParse(response: Response): SManga =
        json.decodeFromString<MangaDataClass>(response.body!!.string()).let { it.toSManga() }

    // ------------- Chapter -------------

    override fun chapterListRequest(manga: SManga): Request =
        GET("$checkedBaseUrl/api/v1/manga/${manga.url}/chapters?onlineFetch=true", headers)

    override fun chapterListParse(response: Response): List<SChapter> =
        json.decodeFromString<List<ChapterDataClass>>(response.body!!.string()).map {
            it.toSChapter()
        }

    // ------------- Page List -------------

    override fun fetchPageList(chapter: SChapter): Observable<List<Page>> {
        return client.newCall(pageListRequest(chapter))
            .asObservableSuccess()
            .map { response ->
                pageListParse(response, chapter)
            }
    }

    override fun pageListRequest(chapter: SChapter): Request {
        val mangaId = chapter.url.split(" ").first()
        val chapterIndex = chapter.url.split(" ").last()

        return GET("$checkedBaseUrl/api/v1/manga/$mangaId/chapter/$chapterIndex/?onlineFetch=True", headers)
    }

    fun pageListParse(response: Response, sChapter: SChapter): List<Page> {
        val mangaId = sChapter.url.split(" ").first()
        val chapterIndex = sChapter.url.split(" ").last()

        val chapter = json.decodeFromString<ChapterDataClass>(response.body!!.string())

        return List(chapter.pageCount) {
            Page(it + 1, "", "$checkedBaseUrl/api/v1/manga/$mangaId/chapter/$chapterIndex/page/$it/")
        }
    }

    // ------------- Filters & Search -------------

    override fun getFilterList(): FilterList =
        FilterList(
            CategorySelect(refreshCategoryList(baseUrl).let { categoryList }),
            Filter.Header("Press reset to attempt to fetch categories")
        )

    private var categoryList: List<CategoryDataClass> = emptyList()

    private fun refreshCategoryList(baseUrl: String) {
        Single.fromCallable {
            client.newCall(GET("$baseUrl/api/v1/category", headers)).execute()
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { response ->
                    categoryList = try {
                        json.decodeFromString<List<CategoryDataClass>>(response.body!!.string())
                    } catch (e: Exception) {
                        emptyList()
                    }
                },
                {}
            )
    }

    init {
        val initBaseUrl = Injekt.get<Application>().getSharedPreferences("source_$id", 0x0000).getString(ADDRESS_TITLE, ADDRESS_DEFAULT)!!

        if (initBaseUrl.isNotBlank()) {
            refreshCategoryList(initBaseUrl)
        }
    }

    private val defaultCategoryId: Int
        get() = categoryList.firstOrNull()?.id ?: 0

    class CategorySelect(categoryList: List<CategoryDataClass>) :
        Filter.Select<String>("Category", categoryList.map { it.name }.toTypedArray())

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        if (query.isNotEmpty()) {
            throw RuntimeException("Only Empty search is supported!")
        } else {
            var selectedFilter = defaultCategoryId

            filters.forEach { filter ->
                when (filter) {
                    is CategorySelect -> {
                        selectedFilter = categoryList[filter.state].id
                    }
                    else -> {
                    }
                }
            }

            return GET("$checkedBaseUrl/api/v1/category/$selectedFilter")
        }
    }

    override fun searchMangaParse(response: Response): MangasPage = popularMangaParse(response)

    // ------------- Preferences -------------
    override fun setupPreferenceScreen(screen: PreferenceScreen) {
        screen.addPreference(screen.editTextPreference(ADDRESS_TITLE, ADDRESS_DEFAULT, baseUrl))
    }

    /** boilerplate for [EditTextPreference] */
    private fun PreferenceScreen.editTextPreference(title: String, default: String, value: String, isPassword: Boolean = false): EditTextPreference {
        return EditTextPreference(context).apply {
            key = title
            this.title = title
            summary = if (value.isEmpty()) "i.e. http://192.168.1.115:4567" else value
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
                    Log.e("Tachidesk", "Exception while setting text preference", e)
                    false
                }
            }
        }
    }

    private val preferences: SharedPreferences by lazy {
        Injekt.get<Application>().getSharedPreferences("source_$id", 0x0000)
    }

    private fun getPrefBaseUrl(): String = preferences.getString(ADDRESS_TITLE, ADDRESS_DEFAULT)!!

    companion object {
        private const val ADDRESS_TITLE = "Server URL Address"
        private const val ADDRESS_DEFAULT = ""
    }

    // ------------- Not Used -------------

    override fun latestUpdatesRequest(page: Int): Request = throw Exception("Not used")

    override fun latestUpdatesParse(response: Response): MangasPage = throw Exception("Not used")

    override fun pageListParse(response: Response): List<Page> = throw Exception("Not used")

    override fun imageUrlParse(response: Response): String = throw Exception("Not used")

    // ------------- Util -------------

    private fun MangaDataClass.toSManga() = SManga.create().also {
        it.title = title
        it.url = id.toString()
        it.thumbnail_url = "$baseUrl$thumbnailUrl"
        it.artist = artist
        it.author = author
        it.description = description
        it.status = when (status) {
            "ONGOING" -> SManga.ONGOING
            "COMPLETED" -> SManga.COMPLETED
            "LICENSED" -> SManga.LICENSED
            else -> SManga.UNKNOWN // covers "UNKNOWN" and other Impossible cases
        }
    }

    private fun ChapterDataClass.toSChapter() = SChapter.create().also {
        it.url = "$mangaId $index"
        it.name = name
        it.date_upload = uploadDate
        it.scanlator = scanlator
    }

    private val checkedBaseUrl: String
        get(): String = if (baseUrl.isNotEmpty()) baseUrl
        else throw RuntimeException("Set Tachidesk server url in extension settings")
}
