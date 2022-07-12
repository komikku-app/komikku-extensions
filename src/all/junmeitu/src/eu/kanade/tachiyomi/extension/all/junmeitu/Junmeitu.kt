package eu.kanade.tachiyomi.extension.all.junmeitu

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
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.Request
import okhttp3.Response
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Evaluator
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

class Junmeitu : ConfigurableSource, ParsedHttpSource() {
    override val lang = "all"
    override val name = "Junmeitu"
    override val supportsLatest = true
    override val id = 4721197766605490540

    private val json: Json by injectLazy()

    // Preference
    private val preferences: SharedPreferences by lazy {
        Injekt.get<Application>().getSharedPreferences("source_$id", 0x0000)
    }

    override val baseUrl: String = getMirrorPref()!!

    override fun setupPreferenceScreen(screen: PreferenceScreen) {
        val mirrorPref = ListPreference(screen.context).apply {
            key = "${MIRROR_PREF_KEY}_$lang"
            title = MIRROR_PREF_TITLE
            entries = MIRROR_PREF_ENTRIES
            entryValues = MIRROR_PREF_ENTRY_VALUES
            setDefaultValue(MIRROR_PREF_DEFAULT_VALUE)
            summary = "%s"
        }
        screen.addPreference(mirrorPref)
    }

    private fun getMirrorPref(): String? = preferences.getString("${MIRROR_PREF_KEY}_$lang", MIRROR_PREF_DEFAULT_VALUE)

    companion object {
        private const val MIRROR_PREF_KEY = "MIRROR"
        private const val MIRROR_PREF_TITLE = "Mirror"
        private val MIRROR_PREF_ENTRIES = arrayOf("Junmeitu.com", "Meijuntu.com")
        private val MIRROR_PREF_ENTRY_VALUES = arrayOf("https://junmeitu.com", "https://meijuntu.com")
        private val MIRROR_PREF_DEFAULT_VALUE = MIRROR_PREF_ENTRY_VALUES[0]
    }

    // Latest
    override fun latestUpdatesFromElement(element: Element): SManga {
        val manga = SManga.create()
        manga.thumbnail_url = element.select("img").attr("abs:src")
        manga.title = element.select("p").text()
        manga.setUrlWithoutDomain(element.select("a").attr("abs:href"))
        return manga
    }

    override fun latestUpdatesNextPageSelector() = "span + a  + a"

    override fun latestUpdatesRequest(page: Int): Request {
        return GET("$baseUrl/beauty/index-$page.html")
    }

    override fun latestUpdatesSelector() = ".pic-list > ul > li"

    // Popular
    override fun popularMangaFromElement(element: Element) = latestUpdatesFromElement(element)
    override fun popularMangaNextPageSelector() = latestUpdatesNextPageSelector()
    override fun popularMangaRequest(page: Int): Request {
        return GET("$baseUrl/beauty/hot-$page.html")
    }
    override fun popularMangaSelector() = latestUpdatesSelector()

    // Search
    override fun searchMangaFromElement(element: Element) = latestUpdatesFromElement(element)
    override fun searchMangaNextPageSelector() = latestUpdatesNextPageSelector()
    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        val tagFilter = filters.findInstance<TagFilter>()
        val modelFilter = filters.findInstance<ModelFilter>()
        val groupFilter = filters.findInstance<GroupFilter>()
        val categoryFilter = filters.findInstance<CategoryFilter>()
        val sortFilter = filters.findInstance<SortFilter>()
        return when {
            query.isNotEmpty() -> GET("$baseUrl/search/$query-$page.html")
            tagFilter!!.state.isNotEmpty() -> GET("$baseUrl/tags/${tagFilter.state}-${categoryFilter!!.selected}-$page.html")
            modelFilter!!.state.isNotEmpty() -> GET("$baseUrl/model/${modelFilter.state}-$page.html")
            groupFilter!!.state.isNotEmpty() -> GET("$baseUrl/xzjg/${groupFilter.state}-$page.html")
            categoryFilter!!.state != 0 -> GET("$baseUrl/${categoryFilter.slug}/${sortFilter!!.selected}-$page.html")
            sortFilter!!.state != 0 -> GET("$baseUrl/${categoryFilter.slug}/${sortFilter.selected}-$page.html")
            else -> latestUpdatesRequest(page)
        }
    }
    override fun searchMangaSelector() = latestUpdatesSelector()

    // Details
    override fun mangaDetailsParse(document: Document): SManga {
        val manga = SManga.create()
        manga.title = document.selectFirst(".news-title,.title").text()
        manga.description = document.select(".news-info,.picture-details").text() + "\n" + document.select(".introduce").text()
        manga.genre = document.select(".relation_tags > a").joinToString { it.text() }
        manga.status = SManga.COMPLETED
        return manga
    }

    override fun chapterFromElement(element: Element): SChapter {
        val chapter = SChapter.create()
        chapter.setUrlWithoutDomain(element.select(".position a:last-child").first().attr("abs:href"))
        chapter.chapter_number = 0F
        chapter.name = element.select(".news-title,.title").text()
        return chapter
    }

    override fun chapterListSelector() = "html"

    // Pages
    override fun pageListParse(document: Document): List<Page> {
        val numPages = document.select(".pages > a:nth-last-of-type(2)").text().toIntOrNull()
        val newsBody = document.selectFirst(Evaluator.Class("news-body"))
        if (newsBody == null) {
            val baseUrl = this.baseUrl
            val prefix = document.location().run {
                val index = lastIndexOf('.') // .html
                baseUrl + "/ajax_" + substring(baseUrl.length + 1, index) + '-'
            }
            val postfix = document.selectFirst("body > script").data().run {
                val script = substringAfterLast("pc_cid = ")
                val categoryId = script.substringBefore(';')
                val contentId = script.substringAfter("pc_id = ").substringBeforeLast(';')
                ".html?ajax=1&catid=$categoryId&conid=$contentId"
            }
            return (1..numPages!!).map { Page(it - 1, "$prefix$it$postfix") }
        } else {
            return newsBody.select(Evaluator.Tag("img")).mapIndexed { index, it ->
                val imgUrl = when {
                    it.hasAttr("data-original") -> it.attr("abs:data-original")
                    it.hasAttr("data-src") -> it.attr("abs:data-src")
                    it.hasAttr("data-lazy-src") -> it.attr("abs:data-lazy-src")
                    else -> it.attr("abs:src")
                }
                Page(index, imageUrl = imgUrl)
            }
        }
    }

    override fun imageUrlParse(response: Response): String {
        val page: PageDto = json.decodeFromString(response.body!!.string())
        val img = Jsoup.parseBodyFragment(page.pic).body().child(0)
        return img.attr("src")
    }

    override fun imageUrlParse(document: Document): String = throw UnsupportedOperationException("Not used")

    // Filters
    override fun getFilterList(): FilterList = FilterList(
        Filter.Header("NOTE: Ignored if using text search!"),
        Filter.Header("NOTE: Filter are weird for this extension!"),
        Filter.Separator(),
        TagFilter(),
        ModelFilter(),
        GroupFilter(),
        CategoryFilter(getCategoryFilter(), 0),
        SortFilter(getSortFilter(), 0)
    )

    class SelectFilterOption(val name: String, val value: String = name)
    abstract class SelectFilter(name: String, private val options: List<SelectFilterOption>, default: Int = 0) : Filter.Select<String>(name, options.map { it.name }.toTypedArray(), default) {
        val selected: String
            get() = options[state].value
        val slug: String
            get() = options[state].name
    }

    class TagFilter : Filter.Text("Tag")
    class ModelFilter : Filter.Text("Model")
    class GroupFilter : Filter.Text("Group")
    class CategoryFilter(options: List<SelectFilterOption>, default: Int) : SelectFilter("Category", options, default)
    class SortFilter(options: List<SelectFilterOption>, default: Int) : SelectFilter("Sort", options, default)

    private fun getCategoryFilter() = listOf(
        SelectFilterOption("beauty", "6"),
        SelectFilterOption("handsome", "5"),
        SelectFilterOption("news", "30"),
        SelectFilterOption("street", "32"),
    )

    private fun getSortFilter() = listOf(
        SelectFilterOption("default", "index"),
        SelectFilterOption("hot"),
    )

    private inline fun <reified T> Iterable<*>.findInstance() = find { it is T } as? T
}
