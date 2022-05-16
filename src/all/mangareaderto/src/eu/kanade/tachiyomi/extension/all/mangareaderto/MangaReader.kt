package eu.kanade.tachiyomi.extension.all.mangareaderto

import android.app.Application
import android.net.Uri
import androidx.preference.ListPreference
import androidx.preference.PreferenceScreen
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.asObservableSuccess
import eu.kanade.tachiyomi.source.ConfigurableSource
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import eu.kanade.tachiyomi.util.asJsoup
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

open class MangaReader(
    override val lang: String
) : ConfigurableSource, ParsedHttpSource() {
    override val name = "MangaReader"

    override val baseUrl = "https://mangareader.to"

    override val supportsLatest = true

    override val client = network.client.newBuilder()
        .addInterceptor(MangaReaderImageInterceptor())
        .build()

    override fun latestUpdatesRequest(page: Int) =
        GET("$baseUrl/filter?sort=latest-updated&language=$lang&page=$page", headers)

    override fun latestUpdatesSelector() = searchMangaSelector()

    override fun latestUpdatesNextPageSelector() = searchMangaNextPageSelector()

    override fun latestUpdatesFromElement(element: Element) =
        searchMangaFromElement(element)

    override fun popularMangaRequest(page: Int) =
        GET("$baseUrl/filter?sort=most-viewed&language=$lang&page=$page", headers)

    override fun popularMangaSelector() = searchMangaSelector()

    override fun popularMangaNextPageSelector() = searchMangaNextPageSelector()

    override fun popularMangaFromElement(element: Element) =
        searchMangaFromElement(element)

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList) =
        if (query.isNotBlank()) {
            Uri.parse("$baseUrl/search").buildUpon().run {
                appendQueryParameter("keyword", query)
                appendQueryParameter("page", page.toString())

                GET(toString(), headers)
            }
        } else {
            Uri.parse("$baseUrl/filter").buildUpon().run {
                appendQueryParameter("language", lang)
                appendQueryParameter("page", page.toString())
                filters.ifEmpty(::getFilterList).forEach { filter ->
                    when (filter) {
                        is Select -> {
                            appendQueryParameter(filter.param, filter.selection)
                        }
                        is DateFilter -> {
                            filter.state.forEach {
                                appendQueryParameter(it.param, it.selection)
                            }
                        }
                        is GenresFilter -> {
                            appendQueryParameter(filter.param, filter.selection)
                        }
                        else -> Unit
                    }
                }
                GET(toString(), headers)
            }
        }

    override fun searchMangaSelector() = ".manga_list-sbs .manga-poster"

    override fun searchMangaNextPageSelector() = ".page-link[title=Next]"

    override fun searchMangaFromElement(element: Element) =
        SManga.create().apply {
            url = element.attr("href")
            element.selectFirst(".manga-poster-img").let {
                title = it.attr("alt")
                thumbnail_url = it.attr("src")
            }
        }

    private val authorSelector = ".item-head:containsOwn(Authors) ~ a"

    private val statusSelector = ".item-head:containsOwn(Status) + .name"

    override fun mangaDetailsParse(document: Document) =
        SManga.create().apply {
            setUrlWithoutDomain(document.location())
            document.getElementById("ani_detail").let { el ->
                title = el.selectFirst(".manga-name").text().trim()
                description = el.selectFirst(".description")?.text()?.trim()
                thumbnail_url = el.selectFirst(".manga-poster-img").attr("src")
                genre = el.select(".genres > a")?.joinToString { it.text() }
                author = el.select(authorSelector)?.joinToString {
                    it.text().replace(",", "")
                }
                artist = author // TODO: separate authors and artists
                status = when (el.selectFirst(statusSelector)?.text()) {
                    "Finished" -> SManga.COMPLETED
                    "Publishing" -> SManga.ONGOING
                    else -> SManga.UNKNOWN
                }
            }
        }

    override fun chapterListSelector() = "#$lang-chapters .item"

    override fun chapterFromElement(element: Element) =
        SChapter.create().apply {
            chapter_number = element.attr("data-number").toFloatOrNull() ?: -1f
            element.selectFirst(".item-link").let {
                url = it.attr("href")
                name = it.attr("title")
            }
        }

    private fun pageListRequest(id: String) =
        GET("$baseUrl/ajax/image/list/chap/$id?quality=$quality", headers)

    override fun fetchPageList(chapter: SChapter) =
        client.newCall(pageListRequest(chapter)).asObservableSuccess().map { res ->
            res.asJsoup().getElementById("wrapper").attr("data-reading-id").let {
                val call = client.newCall(pageListRequest(it))
                val json = JSONObject(call.execute().body!!.string())
                pageListParse(Jsoup.parse(json.getString("html")))
            }
        }!!

    override fun pageListParse(document: Document): List<Page> =
        document.getElementsByClass("iv-card").mapIndexed { idx, img ->
            val url = img.attr("data-url")
            if (img.hasClass("shuffled")) {
                Page(idx, "", "$url&shuffled=true")
            } else {
                Page(idx, "", url)
            }
        }

    override fun imageUrlParse(document: Document) =
        throw UnsupportedOperationException("Not used")

    private val preferences by lazy {
        Injekt.get<Application>().getSharedPreferences("source_$id", 0x0000)!!
    }

    private val quality by lazy {
        preferences.getString("quality", "medium")!!
    }

    override fun setupPreferenceScreen(screen: PreferenceScreen) {
        ListPreference(screen.context).apply {
            key = "quality"
            title = "Quality"
            summary = "%s"
            entries = arrayOf("Low", "Medium", "High")
            entryValues = arrayOf("low", "medium", "high")
            setDefaultValue("medium")

            setOnPreferenceChangeListener { _, newValue ->
                preferences.edit().putString("quality", newValue as String).commit()
            }
        }.let(screen::addPreference)
    }

    override fun getFilterList() =
        FilterList(
            Note,
            TypeFilter(),
            StatusFilter(),
            RatingFilter(),
            ScoreFilter(),
            StartDateFilter(),
            EndDateFilter(),
            SortFilter(),
            GenresFilter()
        )
}
