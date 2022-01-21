package eu.kanade.tachiyomi.extension.ar.mangalink

import android.app.Application
import android.content.SharedPreferences
import androidx.preference.ListPreference
import androidx.preference.PreferenceScreen
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.ConfigurableSource
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class MangaLink : ConfigurableSource, ParsedHttpSource() {

    private val preferences: SharedPreferences by lazy {
        Injekt.get<Application>().getSharedPreferences("source_$id", 0x0000)
    }

    override val name = "MangaLink"

    override val baseUrl by lazy { preferences.getString("preferred_domain", "http://135.181.209.99")!! }

    override val lang = "ar"

    override val supportsLatest = true

    override val client: OkHttpClient = network.cloudflareClient

    // Popular

    override fun popularMangaRequest(page: Int): Request {
        return GET("$baseUrl/mangas?page=$page")
    }

    override fun popularMangaSelector() = "div.card"

    override fun popularMangaFromElement(element: Element): SManga {
        return SManga.create().apply {
            element.select("a:has(h6)").let {
                setUrlWithoutDomain(it.attr("href"))
                title = it.text()
            }
            // thumbnail_url = "http://135.181.209.99" + element.select("img").attr("abs:data-src")
        }
    }

    override fun popularMangaNextPageSelector() = "a[rel=next]"

    // Latest

    override fun latestUpdatesRequest(page: Int): Request {
        return GET(baseUrl)
    }

    override fun latestUpdatesSelector() = popularMangaSelector()

    override fun latestUpdatesFromElement(element: Element): SManga = popularMangaFromElement(element)

    override fun latestUpdatesNextPageSelector(): String? = null

    // Search

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        val url = "$baseUrl/mangas?page=$page&query=$query".toHttpUrlOrNull()!!.newBuilder()
        filters.forEach { filter ->
            when (filter) {
                is TypeFilter -> {
                    filter.state
                        .filter { it.state != Filter.TriState.STATE_IGNORE }
                        .forEach { url.addQueryParameter("story[]", it.id) }
                }
            }
        }
        return GET(url.build().toString(), headers)
    }

    override fun searchMangaSelector() = popularMangaSelector()

    override fun searchMangaFromElement(element: Element): SManga = popularMangaFromElement(element)

    override fun searchMangaNextPageSelector() = popularMangaNextPageSelector()

    // Details

    override fun mangaDetailsParse(document: Document): SManga {
        return SManga.create().apply {
            document.select("div.card").first().let { info ->
                title = info.select("h1").text()
                // add series type(manga/manhwa/manhua/other) thinggy to genre
                genre = info.select("span.d-flex a.btn, span:nth-child(1) > label:nth-child(2)").joinToString { it.text() }
                description = info.select("p.card-text").text()
                thumbnail_url = info.select("img").attr("abs:src")
            }
        }
    }

    // Chapters

    override fun chapterListSelector() = "div.card-body > a.btn"

    override fun chapterFromElement(element: Element): SChapter {
        return SChapter.create().apply {
            name = "# ${element.text()}"
            setUrlWithoutDomain(element.attr("href"))
        }
    }

    // Pages

    override fun pageListParse(document: Document): List<Page> {
        return document.select("div#content img").mapIndexed { i, img ->
            Page(i, "", img.attr("abs:data-src").replace("https://mangalink.cc", "http://135.181.209.99"))
        }
    }

    override fun imageUrlParse(document: Document): String = throw UnsupportedOperationException("Not used")

    // Filters

    override fun getFilterList() = FilterList(
        Filter.Header("Type exclusion not available for this source"),
        Filter.Separator(),
        TypeFilter(getTypeList()),
    )

    class Type(name: String, val id: String = name) : Filter.TriState(name)
    class TypeFilter(types: List<Type>) : Filter.Group<Type>("Type", types)

    open fun getTypeList(): List<Type> = listOf(
        Type("<--->", ""),
        Type("مانجا(يابانية)", "1"),
        Type("مانهوا(كورية)", "2"),
        Type("مانها(صينية)", "3"),
        Type("ويب تون", "4"),
        Type("كوميك", "5"),
        Type("غير معروف", "6"),
    )

    // Preferences

    override fun setupPreferenceScreen(screen: PreferenceScreen) {
        val domainPref = ListPreference(screen.context).apply {
            key = "preferred_domain"
            title = "Preferred domain"
            entries = arrayOf("135.181.209.99", "mangalink.org")
            entryValues = arrayOf("http://135.181.209.99", "https://mangalink.org")
            setDefaultValue("http://135.181.209.99")
            summary = "%s"

            setOnPreferenceChangeListener { _, newValue ->
                val selected = newValue as String
                val index = findIndexOfValue(selected)
                val entry = entryValues[index] as String
                preferences.edit().putString(key, entry).commit()
            }
        }
        screen.addPreference(domainPref)
    }
}
