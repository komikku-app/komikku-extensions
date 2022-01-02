package eu.kanade.tachiyomi.extension.ru.comx

import android.webkit.CookieManager
import eu.kanade.tachiyomi.lib.ratelimit.RateLimitInterceptor
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import eu.kanade.tachiyomi.util.asJsoup
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.FormBody
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import uy.kohesive.injekt.injectLazy
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class ComX : ParsedHttpSource() {

    private val json: Json by injectLazy()

    override val name = "Com-x"

    override val baseUrl = "https://com-x.life"

    override val lang = "ru"

    override val supportsLatest = true
    private val cookieManager by lazy { CookieManager.getInstance() }
    override val client: OkHttpClient = network.cloudflareClient.newBuilder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addNetworkInterceptor(RateLimitInterceptor(3))
        .cookieJar(object : CookieJar {
            override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) =
                cookies.filter { it.matches(url) }.forEach {
                    cookieManager.setCookie(url.toString(), it.toString())
                }

            override fun loadForRequest(url: HttpUrl) =
                cookieManager.getCookie(url.toString())?.split("; ")
                    ?.mapNotNull { Cookie.parse(url, it) } ?: emptyList()
        })
        .build()

    override fun headersBuilder(): Headers.Builder = Headers.Builder()
        .add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:77.0) Gecko/20100101 Firefox/78.0")
        .add("Referer", baseUrl + "/comix-read/")

    override fun popularMangaSelector() = "div.short"

    override fun latestUpdatesSelector() = "ul#content-load li.latest"

    override fun popularMangaRequest(page: Int): Request = GET("$baseUrl/comix-read/page/$page/", headers)

    override fun latestUpdatesRequest(page: Int): Request = GET(baseUrl, headers)

    override fun popularMangaParse(response: Response): MangasPage {
        val document = response.asJsoup()

        val mangas = document.select(popularMangaSelector()).map { element ->
            popularMangaFromElement(element)
        }
        if (document.html().contains("Sorry, your request has been denied.")) throw UnsupportedOperationException("Error: Open in WebView and solve the Antirobot!")

        return MangasPage(mangas, document.select(".pagination__pages span").first().text().toInt() <= document.select(".pagination__pages a:last-child").first().text().toInt())
    }

    override fun popularMangaFromElement(element: Element): SManga {
        val manga = SManga.create()
        manga.thumbnail_url = baseUrl + element.select("img").first().attr("src")
        element.select(".readed__title a").first().let {
            manga.setUrlWithoutDomain(it.attr("href"))
            manga.title = it.text()
        }
        return manga
    }
    override fun latestUpdatesParse(response: Response): MangasPage {
        val document = response.asJsoup()

        val mangas = document.select(latestUpdatesSelector()).map { element ->
            latestUpdatesFromElement(element)
        }
        if (document.html().contains("Sorry, your request has been denied.")) throw UnsupportedOperationException("Error: Open in WebView and solve the Antirobot!")

        return MangasPage(mangas, false)
    }

    override fun latestUpdatesFromElement(element: Element): SManga {
        val manga = SManga.create()
        manga.thumbnail_url = baseUrl + element.select("img").first().attr("src")
        element.select("a.latest__title").first().let {
            manga.setUrlWithoutDomain(it.attr("href"))
            manga.title = it.text()
        }
        return manga
    }

    override fun popularMangaNextPageSelector(): Nothing? = null

    override fun latestUpdatesNextPageSelector(): Nothing? = null

    override fun searchMangaNextPageSelector(): Nothing? = null

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        /** val url = "$baseUrl/index.php?do=xsearch&searchCat=comix-read&page=$page".toHttpUrlOrNull()!!.newBuilder()
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
         }**/
        if (query.isNotEmpty()) {
            return POST(
                "$baseUrl/index.php?do=search&search_start=$page",
                body = FormBody.Builder()
                    .add("do", "search")
                    .add("subaction", "search")
                    .add("story", query)
                    .build(),
                headers = headers
            )
        }
        return GET("$baseUrl/comix-read/", headers)
    }

    override fun searchMangaSelector() = popularMangaSelector()

    override fun searchMangaFromElement(element: Element): SManga = popularMangaFromElement(element)

    override fun mangaDetailsParse(document: Document): SManga {
        if (document.html().contains("Sorry, your request has been denied.")) throw UnsupportedOperationException("Error: Open in WebView and solve the Antirobot!")
        val infoElement = document.select("div.page__grid").first()

        val manga = SManga.create()
        manga.author = infoElement.select(".page__list li:eq(1)").text()
        manga.genre = infoElement.select(".page__tags a").joinToString { it.text() }
        manga.status = parseStatus(infoElement.select(".page__list li:eq(2)").text())

        manga.description = infoElement.select(".page__text ").text()

        val src = infoElement.select(".img-wide img").attr("src")
        if (src.contains(baseUrl.substringAfter("://"))) {
            manga.thumbnail_url = "http:" + src
        } else {
            manga.thumbnail_url = baseUrl + src
        }
        return manga
    }

    private fun parseStatus(element: String): Int = when {
        element.contains("Продолжается") ||
            element.contains(" из ") ||
            element.contains("Онгоинг") -> SManga.ONGOING
        element.contains("Заверш") ||
            element.contains("Лимитка") ||
            element.contains("Ван шот") ||
            element.contains("Графический роман") -> SManga.COMPLETED

        else -> SManga.UNKNOWN
    }

    override fun chapterListSelector() = throw NotImplementedError("Unused")

    override fun chapterListParse(response: Response): List<SChapter> {
        val document = response.asJsoup()
        if (document.html().contains("Sorry, your request has been denied.")) throw UnsupportedOperationException("Error: Open in WebView and solve the Antirobot!")
        val dataStr = document
            .toString()
            .substringAfter("window.__DATA__ = ")
            .substringBefore("</script>")
            .substringBeforeLast(";")

        val data = json.decodeFromString<JsonObject>(dataStr)
        val chaptersList = data["chapters"]?.jsonArray
        val chapters: List<SChapter>? = chaptersList?.map {
            val chapter = SChapter.create()
            chapter.name = it.jsonObject["title"]!!.jsonPrimitive.content
            chapter.date_upload = parseDate(it.jsonObject["date"]!!.jsonPrimitive.content)
            chapter.setUrlWithoutDomain("/readcomix/" + data["news_id"] + "/" + it.jsonObject["id"]!!.jsonPrimitive.content + ".html")
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
    /**
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

     )**/
}
