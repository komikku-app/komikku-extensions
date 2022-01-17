package eu.kanade.tachiyomi.extension.ru.mangaonlinebiz

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.float
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Headers
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import uy.kohesive.injekt.injectLazy
import java.text.SimpleDateFormat
import java.util.Locale

class MangaOnlineBiz : ParsedHttpSource() {

    private val json: Json by injectLazy()

    override val name = "MangaOnlineBiz"

    override val baseUrl = "https://manga-online.biz"

    override val lang = "ru"

    override val supportsLatest = true

    private val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/80.0.3987.163 Safari/537.36"

    override fun headersBuilder(): Headers.Builder = Headers.Builder()
        .add("User-Agent", userAgent)
        .add("Referer", baseUrl)

    override fun popularMangaRequest(page: Int): Request =
        GET("$baseUrl/genre/all/page/$page", headers)

    override fun latestUpdatesRequest(page: Int): Request =
        GET("$baseUrl/genre/all/order/new/page/$page")

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        val url = if (query.isNotBlank()) {
            "$baseUrl/search-ajax/?query=$query"
        } else {
            var ret = String()
            (if (filters.isEmpty()) getFilterList() else filters).forEach { filter ->
                when (filter) {
                    is GenreList -> {
                        ret = "$baseUrl/genre/${filter.values[filter.state].id}/page/$page"
                    }
                }
            }
            ret
        }
        return GET(url, headers)
    }

    override fun popularMangaSelector() = "a.genre"

    override fun latestUpdatesSelector() = popularMangaSelector()

    override fun searchMangaParse(response: Response): MangasPage {
        if (!response.request.url.toString().contains("search-ajax")) {
            return popularMangaParse(response)
        }
        val jsonData = response.body!!.string()
        val results = json.decodeFromString<JsonObject>(jsonData)["results"]!!.jsonArray
        val mangas = mutableListOf<SManga>()
        results.forEach {
            val element = it.jsonObject
            val manga = SManga.create()
            manga.setUrlWithoutDomain(element["url"]!!.jsonPrimitive.content)
            manga.title = element["title"]!!.jsonPrimitive.content.split("/").first()
            val image = element["image"]!!.jsonPrimitive.content
            if (image.startsWith("http")) {
                manga.thumbnail_url = image
            } else {
                manga.thumbnail_url = baseUrl + image
            }

            mangas.add(manga)
        }

        return MangasPage(mangas, false)
    }

    override fun popularMangaFromElement(element: Element): SManga {
        val manga = SManga.create()
        manga.thumbnail_url = element.select("img").first().attr("src")
        manga.setUrlWithoutDomain(element.attr("href"))
        element.select("div.content").first().let {
            manga.title = it.text().split("/").first()
        }
        return manga
    }

    override fun latestUpdatesFromElement(element: Element): SManga =
        popularMangaFromElement(element)

    override fun searchMangaFromElement(element: Element): SManga = throw Exception("Not Used")

    override fun popularMangaNextPageSelector() = "a.button.next"

    override fun latestUpdatesNextPageSelector() = popularMangaNextPageSelector()

    override fun searchMangaNextPageSelector() = throw Exception("Not Used")

    override fun mangaDetailsParse(document: Document): SManga {
        val infoElement = document.select(".items .item").first()
        val manga = SManga.create()
        manga.genre = infoElement.select("a.label").joinToString { it.text() }
        manga.description = infoElement.select(".description").text()
        manga.thumbnail_url = infoElement.select("img").first().attr("src")
        if (infoElement.text().contains("Перевод: закончен")) {
            manga.status = SManga.COMPLETED
        } else if (infoElement.text().contains("Перевод: продолжается")) {
            manga.status = SManga.ONGOING
        }

        return manga
    }

    override fun chapterListParse(response: Response): List<SChapter> {
        val html = response.body!!.string()

        val jsonData = html.split("App.Collection.MangaChapter(").last().split("]);").first() + "]"
        val mangaName = html.split("mangaName: '").last().split("' });").first()
        val chapterList = mutableListOf<SChapter>()
        json.decodeFromString<JsonArray>(jsonData).forEach {
            chapterList.add(chapterFromElement(mangaName, it.jsonObject))
        }
        return chapterList
    }

    override fun chapterListSelector(): String = throw Exception("Not Used")

    private fun chapterFromElement(mangaName: String, element: JsonObject): SChapter {
        val chapter = SChapter.create()
        chapter.setUrlWithoutDomain("/$mangaName/${element["volume"]!!.jsonPrimitive.content}/${element["number"]!!.jsonPrimitive.content})/1")
        chapter.name = "Том ${element["volume"]!!.jsonPrimitive.content} - Глава ${element["number"]!!.jsonPrimitive.content} ${element["title"]!!.jsonPrimitive.content}"
        chapter.chapter_number = element["number"]!!.jsonPrimitive.float
        chapter.date_upload = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(element["date"]!!.jsonPrimitive.content)?.time ?: 0L
        return chapter
    }

    override fun pageListParse(response: Response): List<Page> {
        val html = response.body!!.string()
        val rawPages = html.split("'pages': ").last().split(",\n").first()
        val jsonPages = json.decodeFromString<JsonObject>(rawPages)
        val pages = jsonPages.jsonObject

        val rawCdnUrl = html.split("'srcBaseUrl': ").last().split(",\n").first()
        val cdnUrl = rawCdnUrl.replace("'", "")

        val resPages = mutableListOf<Page>()
        pages.entries.forEach { (page, jsonElement) ->
            resPages.add(Page(page.toInt(), imageUrl = "$cdnUrl/${jsonElement.jsonObject["src"]!!.jsonPrimitive.content}"))
        }
        return resPages
    }

    private class Genre(name: String, val id: String) : Filter.CheckBox(name) {
        override fun toString(): String {
            return name
        }
    }

    private class GenreList(genres: Array<Genre>) : Filter.Select<Genre>("Genres", genres, 0)

    override fun getFilterList() = FilterList(
        GenreList(getGenreList())
    )

    /*  [...document.querySelectorAll(".categories .item")]
    *     .map(el => `Genre("${el.textContent.trim()}", "${el.getAttribute('href')}")`).join(',\n')
    *   on https://manga-online.biz/genre/all/
    */
    private fun getGenreList() = arrayOf(
        Genre("Все", "all"),
        Genre("Боевик", "boevik"),
        Genre("Боевые искусства", "boevye_iskusstva"),
        Genre("Вампиры", "vampiry"),
        Genre("Гарем", "garem"),
        Genre("Гендерная интрига", "gendernaya_intriga"),
        Genre("Героическое фэнтези", "geroicheskoe_fehntezi"),
        Genre("Детектив", "detektiv"),
        Genre("Дзёсэй", "dzyosehj"),
        Genre("Додзинси", "dodzinsi"),
        Genre("Драма", "drama"),
        Genre("Игра", "igra"),
        Genre("История", "istoriya"),
        Genre("Меха", "mekha"),
        Genre("Мистика", "mistika"),
        Genre("Научная фантастика", "nauchnaya_fantastika"),
        Genre("Повседневность", "povsednevnost"),
        Genre("Постапокалиптика", "postapokaliptika"),
        Genre("Приключения", "priklyucheniya"),
        Genre("Психология", "psihologiya"),
        Genre("Романтика", "romantika"),
        Genre("Самурайский боевик", "samurajskij_boevik"),
        Genre("Сверхъестественное", "sverhestestvennoe"),
        Genre("Сёдзё", "syodzyo"),
        Genre("Сёдзё-ай", "syodzyo-aj"),
        Genre("Сёнэн", "syonen"),
        Genre("Спорт", "sport"),
        Genre("Сэйнэн", "sejnen"),
        Genre("Трагедия", "tragediya"),
        Genre("Триллер", "triller"),
        Genre("Ужасы", "uzhasy"),
        Genre("Фантастика", "fantastika"),
        Genre("Фэнтези", "fentezi"),
        Genre("Школа", "shkola"),
        Genre("Этти", "etti"),
        Genre("Юри", "yuri"),
        Genre("Военный", "voennyj"),
        Genre("Жосей", "zhosej"),
        Genre("Магия", "magiya"),
        Genre("Полиция", "policiya"),
        Genre("Смена пола", "smena-pola"),
        Genre("Супер сила", "super-sila"),
        Genre("Эччи", "echchi"),
        Genre("Яой", "yaoj"),
        Genre("Сёнэн-ай", "syonen-aj")
    )

    override fun imageUrlParse(document: Document) = throw Exception("Not Used")

    override fun searchMangaSelector(): String = throw Exception("Not Used")

    override fun chapterFromElement(element: Element): SChapter = throw Exception("Not Used")

    override fun pageListParse(document: Document): List<Page> = throw Exception("Not Used")
}
