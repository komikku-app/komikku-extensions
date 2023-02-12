package eu.kanade.tachiyomi.extension.fr.japscan

import android.app.Application
import android.content.SharedPreferences
import android.net.Uri
import android.util.Base64
import android.util.Log
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.interceptor.rateLimit
import eu.kanade.tachiyomi.source.ConfigurableSource
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import eu.kanade.tachiyomi.util.asJsoup
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale

class Japscan : ConfigurableSource, ParsedHttpSource() {

    override val id: Long = 11

    override val name = "Japscan"

    override val baseUrl = "https://www.japscan.me"

    override val lang = "fr"

    override val supportsLatest = true

    private val json: Json by injectLazy()

    private val preferences: SharedPreferences by lazy {
        Injekt.get<Application>().getSharedPreferences("source_$id", 0x0000)
    }

    override val client: OkHttpClient = network.cloudflareClient.newBuilder()
        .rateLimit(1, 2)
        .build()

    companion object {
        val dateFormat by lazy {
            SimpleDateFormat("dd MMM yyyy", Locale.US)
        }
        private const val SHOW_SPOILER_CHAPTERS_Title = "Les chapitres en Anglais ou non traduit sont upload en tant que \" Spoilers \" sur Japscan"
        private const val SHOW_SPOILER_CHAPTERS = "JAPSCAN_SPOILER_CHAPTERS"
        private val prefsEntries = arrayOf("Montrer uniquement les chapitres traduit en Français", "Montrer les chapitres spoiler")
        private val prefsEntryValues = arrayOf("hide", "show")
    }

    private fun chapterListPref() = preferences.getString(SHOW_SPOILER_CHAPTERS, "hide")

    // Popular
    override fun popularMangaRequest(page: Int): Request {
        return GET("$baseUrl/mangas/", headers)
    }

    override fun popularMangaParse(response: Response): MangasPage {
        val document = response.asJsoup()
        pageNumberDoc = document

        val mangas = document.select(popularMangaSelector()).map { element ->
            popularMangaFromElement(element)
        }
        val hasNextPage = false
        return MangasPage(mangas, hasNextPage)
    }

    override fun popularMangaNextPageSelector(): String? = null

    override fun popularMangaSelector() = "#top_mangas_week li"

    override fun popularMangaFromElement(element: Element): SManga {
        val manga = SManga.create()
        element.select("a").first()!!.let {
            manga.setUrlWithoutDomain(it.attr("href"))
            manga.title = it.text()
            manga.thumbnail_url = "$baseUrl/imgs/${it.attr("href").replace(Regex("/$"),".jpg").replace("manga","mangas")}".lowercase(Locale.ROOT)
        }
        return manga
    }

    // Latest
    override fun latestUpdatesRequest(page: Int): Request {
        return GET(baseUrl, headers)
    }

    override fun latestUpdatesParse(response: Response): MangasPage {
        val document = response.asJsoup()
        val mangas = document.select(latestUpdatesSelector())
            .distinctBy { element -> element.select("a").attr("href") }
            .map { element ->
                latestUpdatesFromElement(element)
            }
        val hasNextPage = false
        return MangasPage(mangas, hasNextPage)
    }

    override fun latestUpdatesNextPageSelector(): String? = null

    override fun latestUpdatesSelector() = "#chapters h3.text-truncate, #chapters_list h3.text-truncate"

    override fun latestUpdatesFromElement(element: Element): SManga = popularMangaFromElement(element)

    // Search
    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        if (query.isEmpty()) {
            val uri = Uri.parse(baseUrl).buildUpon()
                .appendPath("mangas")
            filters.forEach { filter ->
                when (filter) {
                    is TextField -> uri.appendPath(((page - 1) + filter.state.toInt()).toString())
                    is PageList -> uri.appendPath(((page - 1) + filter.values[filter.state]).toString())
                    else -> {}
                }
            }
            return GET(uri.toString(), headers)
        } else {
            val formBody = FormBody.Builder()
                .add("search", query)
                .build()
            val searchHeaders = headers.newBuilder()
                .add("X-Requested-With", "XMLHttpRequest")
                .build()

            try {
                val searchRequest = POST("$baseUrl/live-search/", searchHeaders, formBody)
                val searchResponse = client.newCall(searchRequest).execute()

                if (!searchResponse.isSuccessful) {
                    throw Exception("Code ${searchResponse.code} inattendu")
                }

                val jsonResult = json.parseToJsonElement(searchResponse.body.string()).jsonArray

                if (jsonResult.isEmpty()) {
                    Log.d("japscan", "Search not returning anything, using duckduckgo")
                    throw Exception("Pas de données")
                }

                return searchRequest
            } catch (e: Exception) {
                // Fallback to duckduckgo if the search does not return any result
                val uri = Uri.parse("https://duckduckgo.com/lite/").buildUpon()
                    .appendQueryParameter("q", "$query site:$baseUrl/manga/")
                    .appendQueryParameter("kd", "-1")
                return GET(uri.toString(), headers)
            }
        }
    }

    override fun searchMangaNextPageSelector(): String = "li.page-item:last-child:not(li.active),.next_form .navbutton"

    override fun searchMangaSelector(): String = "div.card div.p-2, a.result-link"

    override fun searchMangaParse(response: Response): MangasPage {
        if ("live-search" in response.request.url.toString()) {
            val jsonResult = json.parseToJsonElement(response.body.string()).jsonArray

            val mangaList = jsonResult.map { jsonEl -> searchMangaFromJson(jsonEl.jsonObject) }

            return MangasPage(mangaList, hasNextPage = false)
        }

        return super.searchMangaParse(response)
    }

    override fun searchMangaFromElement(element: Element): SManga {
        return if (element.attr("class") == "result-link") {
            SManga.create().apply {
                title = element.text().substringAfter(" ").substringBefore(" | JapScan")
                setUrlWithoutDomain(element.attr("abs:href"))
            }
        } else {
            SManga.create().apply {
                thumbnail_url = element.select("img").attr("abs:src")
                element.select("p a").let {
                    title = it.text()
                    url = it.attr("href")
                }
            }
        }
    }

    private fun searchMangaFromJson(jsonObj: JsonObject): SManga = SManga.create().apply {
        title = jsonObj["name"]!!.jsonPrimitive.content
        url = jsonObj["url"]!!.jsonPrimitive.content
    }

    override fun mangaDetailsParse(document: Document): SManga {
        val infoElement = document.selectFirst("#main .card-body")!!

        val manga = SManga.create()
        manga.thumbnail_url = infoElement.select("img").attr("abs:src")

        val infoRows = infoElement.select(".row, .d-flex")
        infoRows.select("p").forEach { el ->
            when (el.select("span").text().trim()) {
                "Auteur(s):" -> manga.author = el.text().replace("Auteur(s):", "").trim()
                "Artiste(s):" -> manga.artist = el.text().replace("Artiste(s):", "").trim()
                "Genre(s):" -> manga.genre = el.text().replace("Genre(s):", "").trim()
                "Statut:" -> manga.status = el.text().replace("Statut:", "").trim().let {
                    parseStatus(it)
                }
            }
        }
        manga.description = infoElement.select("div:contains(Synopsis) + p").text().orEmpty()

        return manga
    }

    private fun parseStatus(status: String) = when {
        status.contains("En Cours") -> SManga.ONGOING
        status.contains("Terminé") -> SManga.COMPLETED
        else -> SManga.UNKNOWN
    }

    override fun chapterListSelector() = "#chapters_list > div.collapse > div.chapters_list" +
        if (chapterListPref() == "hide") { ":not(:has(.badge:contains(SPOILER),.badge:contains(RAW),.badge:contains(VUS)))" } else { "" }
    // JapScan sometimes uploads some "spoiler preview" chapters, containing 2 or 3 untranslated pictures taken from a raw. Sometimes they also upload full RAWs/US versions and replace them with a translation as soon as available.
    // Those have a span.badge "SPOILER" or "RAW". The additional pseudo selector makes sure to exclude these from the chapter list.

    override fun chapterFromElement(element: Element): SChapter {
        val urlElement = element.selectFirst("a")!!

        val chapter = SChapter.create()
        chapter.setUrlWithoutDomain(urlElement.attr("href"))
        chapter.name = urlElement.ownText()
        // Using ownText() doesn't include childs' text, like "VUS" or "RAW" badges, in the chapter name.
        chapter.date_upload = element.selectFirst("span")!!.text().trim().let { parseChapterDate(it) }
        return chapter
    }

    private fun parseChapterDate(date: String): Long {
        return try {
            dateFormat.parse(date)?.time ?: 0
        } catch (e: ParseException) {
            0L
        }
    }

    private val decodingStringsRe: Regex = Regex("""'([\dA-Z]{62})'""", RegexOption.IGNORE_CASE)

    private val sortedLookupString: List<Char> = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray().toList()

    override fun pageListParse(document: Document): List<Page> {
        /*
            JapScan stores chapter metadata in a `#data` element, and in the `data-data` attribute.

            This data is scrambled base64, and to unscramble it this code searches in the ZJS for
            two strings of length 62 (base64 minus `+` and `/`), creating a character map.

            Since figuring out how to properly map characters would be more effort than I want to
            put in, this just flips around the charsets if the first attempt didn't succeed.
         */
        val zjsurl = document.getElementsByTag("script").first {
            it.attr("src").contains("zjs", ignoreCase = true)
        }.attr("src")
        Log.d("japscan", "ZJS at $zjsurl")
        val zjs = client.newCall(GET(baseUrl + zjsurl, headers)).execute().body.string()

        val stringLookupTables = decodingStringsRe.findAll(zjs).mapNotNull {
            it.groupValues[1].takeIf {
                it.toCharArray().sorted() == sortedLookupString
            }
        }.toList()

        if (stringLookupTables.size != 2) {
            throw Exception("Attendait 2 chaînes de recherche dans ZJS, a trouvé ${stringLookupTables.size}")
        }
        Log.d("japscan", "lookup tables: $stringLookupTables")

        val scrambledData = document.getElementById("data")!!.attr("data-data")

        for (i in 0..1) {
            Log.d("japscan", "descramble attempt $i")
            val otherIndice = if (i == 0) 1 else 0
            val lookupTable = stringLookupTables[i].zip(stringLookupTables[otherIndice]).toMap()
            try {
                val unscrambledData = scrambledData.map { lookupTable[it] ?: it }.joinToString("")
                if (!unscrambledData.startsWith("ey")) {
                    // `ey` is the Base64 representation of a curly bracket. Since we're expecting a
                    // JSON object, we're counting this attempt as failed if it doesn't start with a
                    // curly bracket.
                    continue
                }
                val decoded = Base64.decode(unscrambledData, Base64.DEFAULT).toString(Charsets.UTF_8)

                val data = json.parseToJsonElement(decoded).jsonObject

                return data["imagesLink"]!!.jsonArray.mapIndexed { idx, it ->
                    Page(idx, imageUrl = it.jsonPrimitive.content)
                }
            } catch (_: Throwable) {}
        }

        throw Exception("Les deux tentatives de désembrouillage ont échoué")
    }

    override fun imageUrlParse(document: Document): String = ""

    // Filters
    private class TextField(name: String) : Filter.Text(name)

    private class PageList(pages: Array<Int>) : Filter.Select<Int>("Page #", arrayOf(0, *pages))

    override fun getFilterList(): FilterList {
        val totalPages = pageNumberDoc?.select("li.page-item:last-child a")?.text()
        val pagelist = mutableListOf<Int>()
        return if (!totalPages.isNullOrEmpty()) {
            for (i in 0 until totalPages.toInt()) {
                pagelist.add(i + 1)
            }
            FilterList(
                Filter.Header("Page alphabétique"),
                PageList(pagelist.toTypedArray()),
            )
        } else {
            FilterList(
                Filter.Header("Page alphabétique"),
                TextField("Page #"),
                Filter.Header("Appuyez sur reset pour la liste"),
            )
        }
    }

    private var pageNumberDoc: Document? = null

    // Prefs
    override fun setupPreferenceScreen(screen: androidx.preference.PreferenceScreen) {
        val chapterListPref = androidx.preference.ListPreference(screen.context).apply {
            key = SHOW_SPOILER_CHAPTERS_Title
            title = SHOW_SPOILER_CHAPTERS_Title
            entries = prefsEntries
            entryValues = prefsEntryValues
            summary = "%s"

            setOnPreferenceChangeListener { _, newValue ->
                val selected = newValue as String
                val index = this.findIndexOfValue(selected)
                val entry = entryValues[index] as String
                preferences.edit().putString(SHOW_SPOILER_CHAPTERS, entry).commit()
            }
        }
        screen.addPreference(chapterListPref)
    }
}
