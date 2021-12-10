package eu.kanade.tachiyomi.extension.ru.comx

import com.github.salomonbrys.kotson.get
import com.github.salomonbrys.kotson.nullArray
import com.github.salomonbrys.kotson.obj
import com.google.gson.JsonParser
import eu.kanade.tachiyomi.lib.ratelimit.RateLimitInterceptor
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import eu.kanade.tachiyomi.util.asJsoup
import okhttp3.FormBody
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class ComX : ParsedHttpSource() {
    override val name = "Com-x"

    override val baseUrl = "https://com-x.life"

    override val lang = "ru"

    override val supportsLatest = true

    private val userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:72.0) Gecko/20100101 Firefox/72.0"

    override val client: OkHttpClient = network.cloudflareClient.newBuilder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addNetworkInterceptor(RateLimitInterceptor(3))
        .build()

    override fun headersBuilder(): Headers.Builder = Headers.Builder()
        .add("User-Agent", userAgent)
        .add("Referer", baseUrl)

    override fun popularMangaSelector() = "div.shortstory1"

    override fun latestUpdatesSelector() = "ul.last-comix li"

    override fun popularMangaRequest(page: Int): Request = GET("$baseUrl/comix-read/page/$page/", headers)

    override fun latestUpdatesRequest(page: Int): Request = GET(baseUrl, headers)

    override fun popularMangaParse(response: Response): MangasPage {
        val document = response.asJsoup()

        val mangas = document.select(popularMangaSelector()).map { element ->
            popularMangaFromElement(element)
        }
        if (mangas.isEmpty()) throw UnsupportedOperationException("Error: Open in WebView and solve the Antirobot!")

        val hasNextPage = popularMangaNextPageSelector().let { selector ->
            document.select(selector).first()
        } != null

        return MangasPage(mangas, hasNextPage)
    }

    override fun popularMangaFromElement(element: Element): SManga {
        val manga = SManga.create()
        manga.thumbnail_url = baseUrl + element.select("img").first().attr("src")
        element.select("div.info-poster1 a").first().let {
            manga.setUrlWithoutDomain(it.attr("href"))
            manga.title = it.text()
        }
        return manga
    }

    override fun latestUpdatesFromElement(element: Element): SManga {
        val manga = SManga.create()
        manga.thumbnail_url = baseUrl + element.select("img").first().attr("src")
        element.select("a.comix-last-title").first().let {
            manga.setUrlWithoutDomain(it.attr("href"))
            manga.title = it.text()
        }
        return manga
    }

    override fun popularMangaNextPageSelector() = ".pnext:last-child"

    override fun latestUpdatesNextPageSelector(): Nothing? = null

    override fun searchMangaNextPageSelector() = popularMangaNextPageSelector()

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        val url = "$baseUrl/index.php?do=xsearch&searchCat=comix-read&page=$page".toHttpUrlOrNull()!!.newBuilder()
        (if (filters.isEmpty()) getFilterList() else filters).forEach { filter ->
            when (filter) {
                is TypeList -> filter.state.forEach { type ->
                    if (type.state) {
                        url.addQueryParameter("field[type][${type.id}]", 1.toString())
                    }
                }
                is PubList -> filter.state.forEach { publisher ->
                    if (publisher.state) {
                        url.addQueryParameter("subCat[]", publisher.id)
                    }
                }
                is GenreList -> filter.state.forEach { genre ->
                    if (genre.state) {
                        url.addQueryParameter("field[genre][${genre.id}]", 1.toString())
                    }
                }
            }
        }
        if (query.isNotEmpty()) {
            return POST(
                "$baseUrl/comix-read/",
                body = FormBody.Builder()
                    .add("do", "search")
                    .add("story", query)
                    .add("subaction", "search")
                    .build(),
                headers = headers
            )
        }
        return GET(url.toString(), headers)
    }

    override fun searchMangaSelector() = popularMangaSelector()

    override fun searchMangaFromElement(element: Element): SManga = popularMangaFromElement(element)

    override fun mangaDetailsParse(document: Document): SManga {
        val infoElement = document.select("div.maincont").first()

        val manga = SManga.create()
        manga.author = infoElement.select(".fullstory__infoSection:eq(1) > .fullstory__infoSectionContent").text()
        manga.genre = infoElement.select(".fullstory__infoSection:eq(2) > .fullstory__infoSectionContent").text()
            .split(",").plusElement("Комикс").joinToString { it.trim() }

        manga.status = parseStatus(infoElement.select(".fullstory__infoSection:eq(3) > .fullstory__infoSectionContent").text())

        val text = infoElement.select("*").text()
        if (!text.contains("Добавить описание на комикс")) {
            val fromRemove = "Отслеживать"
            val toRemove = "Читать комикс"
            val desc = text.removeRange(0, text.indexOf(fromRemove) + fromRemove.length)
            manga.description = desc.removeRange(desc.indexOf(toRemove) + toRemove.length, desc.length)
        }

        val src = infoElement.select("img").attr("src")
        if (src.contains(baseUrl)) {
            manga.thumbnail_url = src
        } else {
            manga.thumbnail_url = baseUrl + src
        }
        return manga
    }

    private fun parseStatus(element: String): Int = when {
        element.contains("Продолжается") ||
            element.contains(" из ") -> SManga.ONGOING
        element.contains("Заверш") ||
            element.contains("Лимитка") ||
            element.contains("Ван шот") ||
            element.contains("Графический роман") -> SManga.COMPLETED

        else -> SManga.UNKNOWN
    }

    override fun chapterListSelector() = throw NotImplementedError("Unused")

    override fun chapterListParse(response: Response): List<SChapter> {

        val document = response.asJsoup()
        val dataStr = document
            .toString()
            .substringAfter("window.__DATA__ = ")
            .substringBefore("</script>")
            .substringBeforeLast(";")

        val data = JsonParser.parseString(dataStr).obj
        val chaptersList = data["chapters"].nullArray
        val chapters: List<SChapter>? = chaptersList?.map {
            val chapter = SChapter.create()
            chapter.name = it["title"].asString
            chapter.date_upload = parseDate(it["date"].asString)
            chapter.setUrlWithoutDomain("/readcomix/" + data["news_id"] + "/" + it["id"] + ".html")
            chapter
        }
        return chapters ?: emptyList()
    }

    private val simpleDateFormat by lazy { SimpleDateFormat("dd.MM.yyyy", Locale.US) }
    private fun parseDate(date: String?): Long {
        date ?: return 0L
        return try {
            simpleDateFormat.parse(date)!!.time
        } catch (_: Exception) {
            Date().time
        }
    }

    override fun chapterFromElement(element: Element): SChapter =
        throw NotImplementedError("Unused")

    override fun pageListParse(response: Response): List<Page> {
        val html = response.body!!.string()
        val baseImgUrl = "https://img.com-x.life/comix/"

        val beginTag = "\"images\":["
        val beginIndex = html.indexOf(beginTag)
        val endIndex = html.indexOf("]", beginIndex)

        val urls: List<String> = html.substring(beginIndex + beginTag.length, endIndex)
            .split(',').map {
                val img = it.replace("\\", "").replace("\"", "")
                baseImgUrl + img
            }

        val pages = mutableListOf<Page>()
        for (i in urls.indices) {
            pages.add(Page(i, "", urls[i]))
        }

        return pages
    }

    override fun pageListParse(document: Document): List<Page> {
        throw Exception("Not used")
    }

    override fun imageUrlParse(document: Document) = ""

    override fun imageRequest(page: Page): Request {
        return GET(page.imageUrl!!, headers)
    }

    private class CheckFilter(name: String, val id: String) : Filter.CheckBox(name)

    private class TypeList(types: List<CheckFilter>) : Filter.Group<CheckFilter>("Тип выпуска", types)
    private class PubList(publishers: List<CheckFilter>) : Filter.Group<CheckFilter>("Разделы", publishers)
    private class GenreList(genres: List<CheckFilter>) : Filter.Group<CheckFilter>("Жанры", genres)

    override fun getFilterList() = FilterList(
        TypeList(getTypeList()),
        PubList(getPubList()),
        GenreList(getGenreList()),
    )

    private fun getTypeList() = listOf(
        CheckFilter("Лимитка", "1"),
        CheckFilter("Ван шот", "2"),
        CheckFilter("Графический Роман", "3"),
        CheckFilter("Онгоинг", "4"),
    )

    private fun getPubList() = listOf(
        CheckFilter("Marvel", "2"),
        CheckFilter("DC Comics", "14"),
        CheckFilter("Dark Horse", "7"),
        CheckFilter("IDW Publishing", "6"),
        CheckFilter("Image", "4"),
        CheckFilter("Vertigo", "8"),
        CheckFilter("Dynamite Entertainment", "10"),
        CheckFilter("Wildstorm", "5"),
        CheckFilter("Avatar Press", "11"),
        CheckFilter("Boom! Studios", "12"),
        CheckFilter("Top Cow", "9"),
        CheckFilter("Oni Press", "13"),
        CheckFilter("Valiant", "15"),
        CheckFilter("Icon Comics", "16"),
        CheckFilter("Manga", "3"),
        CheckFilter("Manhua", "45"),
        CheckFilter("Manhwa", "44"),
        CheckFilter("Разные комиксы", "18")
    )

    private fun getGenreList() = listOf(
        CheckFilter("Sci-Fi", "2"),
        CheckFilter("Антиутопия", "3"),
        CheckFilter("Апокалипсис", "4"),
        CheckFilter("Боевик", "5"),
        CheckFilter("Боевые искусства", "6"),
        CheckFilter("Вампиры", "7"),
        CheckFilter("Вестерн", "8"),
        CheckFilter("Военный", "9"),
        CheckFilter("Детектив", "10"),
        CheckFilter("Драма", "11"),
        CheckFilter("Зомби", "12"),
        CheckFilter("Игры", "13"),
        CheckFilter("Исекай", "14"),
        CheckFilter("Исторический", "15"),
        CheckFilter("Киберпанк", "16"),
        CheckFilter("Комедия", "17"),
        CheckFilter("Космоопера", "18"),
        CheckFilter("Космос", "19"),
        CheckFilter("Криминал", "20"),
        CheckFilter("МелоДрама", "21"),
        CheckFilter("Мистика", "22"),
        CheckFilter("Научная Фантастика", "23"),
        CheckFilter("Неотвратимость", "24"),
        CheckFilter("Нуар", "25"),
        CheckFilter("Паника", "26"),
        CheckFilter("Пародия", "27"),
        CheckFilter("Повседневность", "28"),
        CheckFilter("Постапокалиптика", "29"),
        CheckFilter("ПредательСредиНас", "30"),
        CheckFilter("Приключения", "31"),
        CheckFilter("Путешествия во времени", "32"),
        CheckFilter("Сверхъестественное", "33"),
        CheckFilter("Слэшер", "34"),
        CheckFilter("Смерть", "35"),
        CheckFilter("Супергерои", "36"),
        CheckFilter("Супергероика", "37"),
        CheckFilter("Сёнен", "38"),
        CheckFilter("Тревога", "39"),
        CheckFilter("Триллер", "40"),
        CheckFilter("Ужасы", "41"),
        CheckFilter("Фантасмагория", "42"),
        CheckFilter("Фантастика", "43"),
        CheckFilter("Фэнтези", "44"),
        CheckFilter("Экшен", "45"),
        CheckFilter("Экшн", "46"),
        CheckFilter("Эротика", "47"),
        CheckFilter("сэйнэн", "66"),
        CheckFilter("сёдзё", "67"),
        CheckFilter("сёнэн", "68"),
        CheckFilter("сёнэн-ай", "69"),
        CheckFilter("трагедия", "70"),
        CheckFilter("фэнтези", "73"),
        CheckFilter("школа", "74"),
        CheckFilter("этти", "76"),
        CheckFilter("яой", "77"),

    )
}
