package eu.kanade.tachiyomi.extension.all.projectsuki

import android.app.Application
import android.content.SharedPreferences
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceScreen
import eu.kanade.tachiyomi.lib.randomua.addRandomUAPreferenceToScreen
import eu.kanade.tachiyomi.lib.randomua.getPrefCustomUA
import eu.kanade.tachiyomi.lib.randomua.getPrefUAType
import eu.kanade.tachiyomi.lib.randomua.setRandomUserAgent
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.asObservableSuccess
import eu.kanade.tachiyomi.network.interceptor.rateLimit
import eu.kanade.tachiyomi.source.ConfigurableSource
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.UpdateStrategy
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.util.asJsoup
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import rx.Observable
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.Locale

@Suppress("unused")
class ProjectSuki : HttpSource(), ConfigurableSource {
    override val name: String = "Project Suki"
    override val baseUrl: String = "https://projectsuki.com"
    override val lang: String = "en"

    private val preferences: SharedPreferences by lazy {
        Injekt.get<Application>().getSharedPreferences("source_$id", 0x0000)
    }

    private fun String.processLangPref(): List<String> = split(",").map { it.trim().lowercase(Locale.US) }

    private val SharedPreferences.whitelistedLanguages: List<String>
        get() = getString(PS.PREFERENCE_WHITELIST_LANGUAGES, "")!!
            .processLangPref()

    private val SharedPreferences.blacklistedLanguages: List<String>
        get() = getString(PS.PREFERENCE_BLACKLIST_LANGUAGES, "")!!
            .processLangPref()

    override fun setupPreferenceScreen(screen: PreferenceScreen) {
        addRandomUAPreferenceToScreen(screen)

        screen.addPreference(
            EditTextPreference(screen.context).apply {
                key = PS.PREFERENCE_WHITELIST_LANGUAGES
                title = PS.PREFERENCE_WHITELIST_LANGUAGES_TITLE
                summary = PS.PREFERENCE_WHITELIST_LANGUAGES_SUMMARY
            },
        )

        screen.addPreference(
            EditTextPreference(screen.context).apply {
                key = PS.PREFERENCE_BLACKLIST_LANGUAGES
                title = PS.PREFERENCE_BLACKLIST_LANGUAGES_TITLE
                summary = PS.PREFERENCE_BLACKLIST_LANGUAGES_SUMMARY
            },
        )
    }

    override val client: OkHttpClient = network.cloudflareClient.newBuilder()
        .setRandomUserAgent(
            userAgentType = preferences.getPrefUAType(),
            customUA = preferences.getPrefCustomUA(),
            filterInclude = listOf("chrome"),
        )
        .rateLimit(4)
        .build()

    override fun popularMangaRequest(page: Int) = GET(baseUrl, headers)

    // differentiating between popular and latest manga in the main page is
    // *theoretically possible* but a pain, as such, this is fine "for now"
    override fun popularMangaParse(response: Response): MangasPage {
        val document = response.asJsoup()
        val allBooks = document.getAllBooks()
        return MangasPage(
            mangas = allBooks.mapNotNull mangas@{ (_, psbook) ->
                val (img, _, titleText, _, url) = psbook

                val relativeUrl = url.rawRelative ?: return@mangas null

                SManga.create().apply {
                    this.url = relativeUrl
                    this.title = titleText
                    this.thumbnail_url = img.imgNormalizedURL()?.rawAbsolute
                }
            },
            hasNextPage = false,
        )
    }

    override val supportsLatest: Boolean = false
    override fun latestUpdatesRequest(page: Int): Request = throw UnsupportedOperationException()
    override fun latestUpdatesParse(response: Response): MangasPage = throw UnsupportedOperationException()

    override fun fetchSearchManga(page: Int, query: String, filters: FilterList): Observable<MangasPage> {
        return when {
            /*query.startsWith(PS.SEARCH_INTENT_PREFIX) -> {
                val id = query.substringAfter(PS.SEARCH_INTENT_PREFIX)
                client.newCall(getMangaByIdAsSearchResult(id))
                    .asObservableSuccess()
                    .map { response -> searchMangaParse(response) }
            }*/

            else -> Observable.defer {
                try {
                    client.newCall(searchMangaRequest(page, query, filters))
                        .asObservableSuccess()
                } catch (e: NoClassDefFoundError) {
                    throw RuntimeException(e)
                }
            }.map { response -> searchMangaParse(response) }
        }
    }

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        return GET(
            baseUrl.toHttpUrl().newBuilder().apply {
                addPathSegment("search")
                addQueryParameter("page", (page - 1).toString())
                addQueryParameter("q", query)

                filters.applyFilter<PSFilters.Origin>(this)
                filters.applyFilter<PSFilters.Status>(this)
                filters.applyFilter<PSFilters.Author>(this)
                filters.applyFilter<PSFilters.Artist>(this)
            }.build(),
            headers,
        )
    }

    private inline fun <reified T> FilterList.applyFilter(to: HttpUrl.Builder) where T : Filter<*>, T : PSFilters.AutoFilter {
        firstNotNullOfOrNull { it as? T }?.applyTo(to)
    }

    override fun getFilterList() = FilterList(
        Filter.Header("Filters only take effect when searching for something!"),
        PSFilters.Origin(),
        PSFilters.Status(),
        PSFilters.Author.ownHeader,
        PSFilters.Author(),
        PSFilters.Artist.ownHeader,
        PSFilters.Artist(),
    )

    override fun searchMangaParse(response: Response): MangasPage {
        val document = response.asJsoup()
        val allBooks = document.getAllBooks()

        val mangas = allBooks.mapNotNull mangas@{ (_, psbook) ->
            val (img, _, titleText, _, url) = psbook

            val relativeUrl = url.rawRelative ?: return@mangas null

            SManga.create().apply {
                this.url = relativeUrl
                this.title = titleText
                this.thumbnail_url = img.imgNormalizedURL()?.rawAbsolute
            }
        }

        return MangasPage(
            mangas = mangas,
            hasNextPage = mangas.size >= 30, // observed max number of results in search
        )
    }

    override fun fetchMangaDetails(manga: SManga): Observable<SManga> {
        return client.newCall(mangaDetailsRequest(manga))
            .asObservableSuccess()
            .map { response ->
                mangaDetailsParse(response, incomplete = manga).apply { initialized = true }
            }
    }

    private val displayNoneMatcher = """display: ?none;""".toRegex()
    private val emptyImageURLAbsolute = """https://projectsuki.com/images/gallery/empty.jpg""".toNormalURL()!!.rawAbsolute
    private val emptyImageURLRelative = """https://projectsuki.com/images/gallery/empty.jpg""".toNormalURL()!!.rawRelative!!
    override fun mangaDetailsParse(response: Response): SManga = throw UnsupportedOperationException("not used")
    private fun mangaDetailsParse(response: Response, incomplete: SManga): SManga {
        val document = response.asJsoup()
        val allLinks = document.getAllUrlElements("a", "href") { it.isPSUrl() }

        val thumb: Element? = document.select("img").firstOrNull { img ->
            img.attr("onerror").let {
                it.contains(emptyImageURLAbsolute) ||
                    it.contains(emptyImageURLRelative)
            }
        }

        val authors: Map<Element, NormalizedURL> = allLinks.filter { (_, url) ->
            url.queryParameterNames.contains("author")
        }

        val artists: Map<Element, NormalizedURL> = allLinks.filter { (_, url) ->
            url.queryParameterNames.contains("artist")
        }

        val statuses: Map<Element, NormalizedURL> = allLinks.filter { (_, url) ->
            url.queryParameterNames.contains("status")
        }

        val origins: Map<Element, NormalizedURL> = allLinks.filter { (_, url) ->
            url.queryParameterNames.contains("origin")
        }

        val genres: Map<Element, NormalizedURL> = allLinks.filter { (_, url) ->
            url.pathStartsWith(listOf("genre"))
        }

        val description = document.select("#descriptionCollapse").joinToString("\n-----\n", postfix = "\n") { it.wholeText() }

        val alerts = document.select(".alert, .alert-info")
            .filter(
                predicate = {
                    it.parents().none { parent ->
                        parent.attr("style")
                            .contains(displayNoneMatcher)
                    }
                },
            )

        val userRating = document.select("#ratings")
            .firstOrNull()
            ?.children()
            ?.count { it.hasClass("text-warning") }
            ?.takeIf { it > 0 }

        return SManga.create().apply {
            url = incomplete.url
            title = incomplete.title
            thumbnail_url = thumb?.imgNormalizedURL()?.rawAbsolute ?: incomplete.thumbnail_url

            author = authors.keys.joinToString(", ") { it.text() }
            artist = artists.keys.joinToString(", ") { it.text() }
            status = when (statuses.keys.joinToString("") { it.text().trim() }.lowercase(Locale.US)) {
                "ongoing" -> SManga.ONGOING
                "completed" -> SManga.PUBLISHING_FINISHED
                "hiatus" -> SManga.ON_HIATUS
                "cancelled" -> SManga.CANCELLED
                else -> SManga.UNKNOWN
            }

            this.description = buildString {
                if (alerts.isNotEmpty()) {
                    appendLine("Alerts have been found, refreshing the manga later might help in removing them.")
                    appendLine()

                    alerts.forEach { alert ->
                        var appendedSomething = false
                        alert.select("h4").singleOrNull()?.let {
                            appendLine(it.text())
                            appendedSomething = true
                        }
                        alert.select("p").singleOrNull()?.let {
                            appendLine(it.text())
                            appendedSomething = true
                        }
                        if (!appendedSomething) {
                            appendLine(alert.text())
                        }
                    }

                    appendLine()
                    appendLine()
                }

                appendLine(description)

                fun appendToDescription(by: String, data: String?) {
                    if (data != null) append(by).appendLine(data)
                }

                appendToDescription("User Rating: ", """${userRating ?: "?"}/5""")
                appendToDescription("Authors: ", author)
                appendToDescription("Artists: ", artist)
                appendToDescription("Status: ", statuses.keys.joinToString(", ") { it.text() })
                appendToDescription("Origin: ", origins.keys.joinToString(", ") { it.text() })
                appendToDescription("Genres: ", genres.keys.joinToString(", ") { it.text() })
            }

            this.update_strategy = if (status != SManga.CANCELLED) UpdateStrategy.ALWAYS_UPDATE else UpdateStrategy.ONLY_FETCH_ONCE
            this.genre = buildList {
                addAll(genres.keys.map { it.text() })
                origins.values.forEach { url ->
                    when (url.queryParameter("origin")) {
                        "kr" -> add("Manhwa")
                        "cn" -> add("Manhua")
                        "jp" -> add("Manga")
                    }
                }
            }.joinToString(", ")
        }
    }

    private val chapterHeaderMatcher = """chapters?""".toRegex()
    private val groupHeaderMatcher = """groups?""".toRegex()
    private val dateHeaderMatcher = """added|date""".toRegex()
    private val languageHeaderMatcher = """language""".toRegex()
    private val chapterNumberMatcher = """[Cc][Hh][Aa][Pp][Tt][Ee][Rr]\s*(\d+)(?:\s*[.,-]\s*(\d+))?""".toRegex()
    private val looseNumberMatcher = """(\d+)(?:\s*[.,-]\s*(\d+))?""".toRegex()
    override fun chapterListParse(response: Response): List<SChapter> {
        val document = response.asJsoup()
        val chaptersTable = document.select("table").firstOrNull { it.containsReadLinks() } ?: return emptyList()

        val thead: Element = chaptersTable.select("thead").firstOrNull() ?: return emptyList()
        val tbody: Element = chaptersTable.select("tbody").firstOrNull() ?: return emptyList()

        val columnTypes = thead.select("tr").firstOrNull()?.children()?.select("td") ?: return emptyList()
        val textTypes = columnTypes.map { it.text().lowercase(Locale.US) }
        val normalSize = textTypes.size

        val chaptersIndex: Int = textTypes.indexOfFirst { it.matches(chapterHeaderMatcher) }.takeIf { it >= 0 } ?: return emptyList()
        val dateIndex: Int = textTypes.indexOfFirst { it.matches(dateHeaderMatcher) }.takeIf { it >= 0 } ?: return emptyList()
        val groupIndex: Int? = textTypes.indexOfFirst { it.matches(groupHeaderMatcher) }.takeIf { it >= 0 }
        val languageIndex: Int? = textTypes.indexOfFirst { it.matches(languageHeaderMatcher) }.takeIf { it >= 0 }

        val dataRows = tbody.children().select("tr")

        val blLangs = preferences.blacklistedLanguages
        val wlLangs = preferences.whitelistedLanguages

        return dataRows.mapNotNull chapters@{ tr ->
            val rowData = tr.children().select("td")

            if (rowData.size != normalSize) {
                return@chapters null
            }

            val chapter: Element = rowData[chaptersIndex]
            val date: Element = rowData[dateIndex]
            val group: Element? = groupIndex?.let(rowData::get)
            val language: Element? = languageIndex?.let(rowData::get)

            language?.text()?.lowercase(Locale.US)?.let { lang ->
                if (lang in blLangs && lang !in wlLangs) return@chapters null
            }

            val chapterLink = chapter.select("a").first()!!.attrNormalizedUrl("href")!!

            val relativeURL = chapterLink.rawRelative ?: return@chapters null

            SChapter.create().apply {
                chapter_number = chapter.text()
                    .let { (chapterNumberMatcher.find(it) ?: looseNumberMatcher.find(it)) }
                    ?.let { result ->
                        val integral = result.groupValues[1]
                        val fractional = result.groupValues.getOrNull(2)

                        """${integral}$fractional""".toFloat()
                    } ?: -1f

                url = relativeURL
                scanlator = group?.text() ?: "<UNKNOWN>"
                name = chapter.text()
                date_upload = date.text().parseDate()
            }
        }.toList()
    }

    override fun imageUrlParse(response: Response) = throw UnsupportedOperationException("Not used")

    private val callpageUrl = """https://projectsuki.com/callpage"""
    private val jsonMediaType = "application/json;charset=UTF-8".toMediaType()
    override fun fetchPageList(chapter: SChapter): Observable<List<Page>> {
        // chapter.url is /read/<bookid>/<chapterid>/...
        val url = chapter.url.toNormalURL() ?: return Observable.just(emptyList())

        val bookid = url.pathSegments[1] // <bookid>
        val chapterid = url.pathSegments[2] // <chapterid>

        val callpageHeaders = headersBuilder()
            .add("X-Requested-With", "XMLHttpRequest")
            .add("Content-Type", "application/json;charset=UTF-8")
            .build()

        val callpageBody = Json.encodeToString(
            mapOf(
                "bookid" to bookid,
                "chapterid" to chapterid,
                "first" to "true",
            ),
        ).toRequestBody(jsonMediaType)

        return client.newCall(
            POST(callpageUrl, callpageHeaders, callpageBody),
        ).asObservableSuccess()
            .map { response ->
                callpageParse(chapter, response)
            }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun callpageParse(chapter: SChapter, response: Response): List<Page> {
        // response contains the html src with images
        val src = Json.parseToJsonElement(response.body.string()).jsonObject["src"]?.jsonPrimitive?.content ?: return emptyList()
        val images = Jsoup.parseBodyFragment(src).select("img")
        // images urls are /images/gallery/<bookid>/<uuid>/<pagenum>? (empty query for some reason)
        val urls = images.mapNotNull { it.attrNormalizedUrl("src") }
        if (urls.isEmpty()) return emptyList()

        val anUrl = urls.random()
        val pageNums = urls.mapTo(ArrayList()) { it.pathSegments[4] }
        pageNums += "001"

        fun makeURL(pageNum: String) = anUrl.newBuilder()
            .setPathSegment(anUrl.pathSegments.lastIndex, pageNum)
            .build()

        return pageNums.distinct().sortedBy { it.toInt() }.mapIndexed { index, number ->
            Page(
                index,
                "",
                makeURL(number).rawAbsolute,
            )
        }.distinctBy { it.imageUrl }
    }

    override fun pageListParse(response: Response): List<Page> = throw UnsupportedOperationException("not used")
}
