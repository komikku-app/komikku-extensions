package eu.kanade.tachiyomi.extension.ru.newbie

import BookDto
import LibraryDto
import MangaDetDto
import PageDto
import PageWrapperDto
import SeriesWrapperDto
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.os.Build
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.asObservableSuccess
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.jsoup.Jsoup
import rx.Observable
import uy.kohesive.injekt.injectLazy
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
class Newbie : HttpSource() {
    override val name = "Newbie"

    override val baseUrl = "https://newmanga.org"

    override val lang = "ru"

    override val supportsLatest = true

    override fun headersBuilder(): Headers.Builder = Headers.Builder()
        .add("User-Agent", "Tachiyomi")
        .add("Referer", baseUrl)

    private fun imageContentTypeIntercept(chain: Interceptor.Chain): Response {
        if (chain.request().url.queryParameter("slice").isNullOrEmpty()) {
            return chain.proceed(chain.request())
        }

        val response = chain.proceed(chain.request())
        val image = response.body?.byteString()?.toResponseBody("image/webp".toMediaType())
        return response.newBuilder().body(image).build()
    }

    override val client: OkHttpClient =
        network.client.newBuilder()
            .addInterceptor { imageContentTypeIntercept(it) }
            .build()

    private val count = 30

    override fun popularMangaRequest(page: Int) = GET("$API_URL/projects/popular?scale=month&size=$count&page=$page", headers)

    override fun popularMangaParse(response: Response): MangasPage = searchMangaParse(response)

    override fun latestUpdatesRequest(page: Int): Request = GET("$API_URL/projects/updates?only_bookmarks=false&size=$count&page=$page", headers)

    override fun latestUpdatesParse(response: Response): MangasPage = searchMangaParse(response)

    override fun searchMangaParse(response: Response): MangasPage {
        val page = json.decodeFromString<PageWrapperDto<LibraryDto>>(response.body!!.string())
        val mangas = page.items.map {
            it.toSManga()
        }
        return MangasPage(mangas, mangas.size == count)
    }

    private fun LibraryDto.toSManga(): SManga {
        val o = this
        return SManga.create().apply {
            // Do not change the title name to ensure work with a multilingual catalog!
            title = o.title.en
            url = "$id"
            thumbnail_url = if (image.srcset.large.isNotEmpty()) {
                "$IMAGE_URL/${image.srcset.large}"
            } else "" +
                "$IMAGE_URL/${image.srcset.small}"
        }
    }

    private val simpleDateFormat by lazy { SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US) }

    private fun parseDate(date: String?): Long {
        date ?: return 0L
        return try {
            simpleDateFormat.parse(date)!!.time
        } catch (_: Exception) {
            Date().time
        }
    }

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        var url = "$API_URL/projects/catalog?size=$count&page=$page".toHttpUrlOrNull()!!.newBuilder()
        if (query.isNotEmpty()) {
            url = "$API_URL/projects/search?size=$count&page=$page".toHttpUrlOrNull()!!.newBuilder()
            url.addQueryParameter("query", query)
        }
        (if (filters.isEmpty()) getFilterList() else filters).forEach { filter ->
            when (filter) {
                is OrderBy -> {
                    val ord = arrayOf("rating", "fresh")[filter.state!!.index]
                    url.addQueryParameter("sorting", ord)
                }
                is TypeList -> filter.state.forEach { type ->
                    if (type.state) {
                        url.addQueryParameter("types", type.id)
                    }
                }
                is StatusList -> filter.state.forEach { status ->
                    if (status.state) {
                        url.addQueryParameter("statuses", status.id)
                    }
                }
                is GenreList -> filter.state.forEach { genre ->
                    if (genre.state) {
                        url.addQueryParameter("genres", genre.id)
                    }
                }
            }
        }
        return GET(url.toString(), headers)
    }

    private fun parseStatus(status: String): Int {
        return when (status) {
            "completed" -> SManga.COMPLETED
            "on_going" -> SManga.ONGOING
            else -> SManga.UNKNOWN
        }
    }

    private fun parseType(type: String): String {
        return when (type) {
            "manga" -> "Манга"
            "manhwa" -> "Манхва"
            "manhya" -> "Маньхуа"
            "single" -> "Сингл"
            "comics" -> "Комикс"
            "russian" -> "Руманга"
            else -> type
        }
    }

    private fun MangaDetDto.toSManga(): SManga {
        val ratingValue = DecimalFormat("#,###.##").format(rating * 2).replace(",", ".").toFloat()
        val ratingStar = when {
            ratingValue > 9.5 -> "★★★★★"
            ratingValue > 8.5 -> "★★★★✬"
            ratingValue > 7.5 -> "★★★★☆"
            ratingValue > 6.5 -> "★★★✬☆"
            ratingValue > 5.5 -> "★★★☆☆"
            ratingValue > 4.5 -> "★★✬☆☆"
            ratingValue > 3.5 -> "★★☆☆☆"
            ratingValue > 2.5 -> "★✬☆☆☆"
            ratingValue > 1.5 -> "★☆☆☆☆"
            ratingValue > 0.5 -> "✬☆☆☆☆"
            else -> "☆☆☆☆☆"
        }
        val o = this
        return SManga.create().apply {
            // Do not change the title name to ensure work with a multilingual catalog!
            title = o.title.en
            url = "$id"
            thumbnail_url = "$IMAGE_URL/${image.srcset.large}"
            author = o.author?.name
            artist = o.artist?.name
            description = o.title.ru + "\n" + ratingStar + " " + ratingValue + "\n" + Jsoup.parse(o.description).text()
            genre = genres.joinToString { it.title.ru.capitalize() } + ", " + parseType(type) + ", " + "$adult+"
            status = parseStatus(o.status)
        }
    }

    private fun titleDetailsRequest(manga: SManga): Request {
        return GET(API_URL + "/projects/" + manga.url, headers)
    }
    // Workaround to allow "Open in browser" use the real URL.
    override fun fetchMangaDetails(manga: SManga): Observable<SManga> {
        return client.newCall(titleDetailsRequest(manga))
            .asObservableSuccess()
            .map { response ->
                mangaDetailsParse(response).apply { initialized = true }
            }
    }
    override fun mangaDetailsRequest(manga: SManga): Request {
        return GET(baseUrl + "/p/" + manga.url, headers)
    }

    override fun mangaDetailsParse(response: Response): SManga {
        val series = json.decodeFromString<MangaDetDto>(response.body!!.string())
        return series.toSManga()
    }

    @SuppressLint("DefaultLocale")
    private fun chapterName(book: BookDto): String {
        var chapterName = "${book.tom}. Глава ${DecimalFormat("#,###.##").format(book.number).replace(",", ".")}"
        if (book.name?.isNotBlank() == true) {
            chapterName += " ${book.name.capitalize()}"
        }
        return chapterName
    }

    override fun chapterListParse(response: Response): List<SChapter> {
        val chapters = json.decodeFromString<SeriesWrapperDto<List<BookDto>>>(response.body!!.string())
        return chapters.items.filter { it.is_available == true }.map { chapter ->
            SChapter.create().apply {
                chapter_number = chapter.number
                name = chapterName(chapter)
                url = "/chapters/${chapter.id}/pages"
                date_upload = parseDate(chapter.created_at)
                scanlator = chapter.translator
            }
        }
    }
    override fun chapterListRequest(manga: SManga): Request {
        return GET(API_URL + "/branches/" + manga.url + "/chapters?reverse=true&size=1000000", headers)
    }

    @TargetApi(Build.VERSION_CODES.N)
    override fun pageListRequest(chapter: SChapter): Request {
        return GET(API_URL + chapter.url, headers)
    }

    private fun pageListParse(response: Response, chapter: SChapter): List<Page> {
        val body = response.body?.string()!!
        val pages = json.decodeFromString<List<PageDto>>(body)
        val result = mutableListOf<Page>()
        pages.forEach { page ->
            (1..page.slices!!).map { i ->
                result.add(Page(result.size, "", API_URL + chapter.url + "/${page.id}?slice=$i"))
            }
        }
        return result
    }
    override fun pageListParse(response: Response): List<Page> = throw Exception("Not used")
    override fun fetchPageList(chapter: SChapter): Observable<List<Page>> {
        return client.newCall(pageListRequest(chapter))
            .asObservableSuccess()
            .map { response ->
                pageListParse(response, chapter)
            }
    }
    override fun fetchImageUrl(page: Page): Observable<String> = Observable.just(page.imageUrl!!)

    override fun imageUrlRequest(page: Page): Request = throw NotImplementedError("Unused")

    override fun imageUrlParse(response: Response): String = throw NotImplementedError("Unused")

    override fun imageRequest(page: Page): Request {
        val refererHeaders = headersBuilder().build()
        return GET(page.imageUrl!!, refererHeaders)
    }

    private class CheckFilter(name: String, val id: String) : Filter.CheckBox(name)

    private class TypeList(types: List<CheckFilter>) : Filter.Group<CheckFilter>("Типы", types)
    private class StatusList(statuses: List<CheckFilter>) : Filter.Group<CheckFilter>("Статус", statuses)
    private class GenreList(genres: List<CheckFilter>) : Filter.Group<CheckFilter>("Жанры", genres)

    override fun getFilterList() = FilterList(
        OrderBy(),
        GenreList(getGenreList()),
        TypeList(getTypeList()),
        StatusList(getStatusList())
    )

    private class OrderBy : Filter.Sort(
        "Сортировка",
        arrayOf("По рейтенгу", "По новизне"),
        Selection(0, false)
    )

    private fun getTypeList() = listOf(
        CheckFilter("Манга", "manga"),
        CheckFilter("Манхва", "manhwa"),
        CheckFilter("Маньхуа", "manhya"),
        CheckFilter("Сингл", "single"),
        CheckFilter("OEL-манга", "oel"),
        CheckFilter("Комикс", "comics"),
        CheckFilter("Руманга", "russian")
    )

    private fun getStatusList() = listOf(
        CheckFilter("Выпускается", "on_going"),
        CheckFilter("Заброшен", "abandoned"),
        CheckFilter("Завершён", "completed"),
        CheckFilter("Приостановлен", "suspended")
    )

    private fun getGenreList() = listOf(
        CheckFilter("cёнэн-ай", "28"),
        CheckFilter("боевик", "17"),
        CheckFilter("боевые искусства", "33"),
        CheckFilter("гарем", "34"),
        CheckFilter("гендерная интрига", "3"),
        CheckFilter("героическое фэнтези", "19"),
        CheckFilter("детектив", "35"),
        CheckFilter("дзёсэй", "4"),
        CheckFilter("додзинси", "20"),
        CheckFilter("драма", "36"),
        CheckFilter("ёнкома", "5"),
        CheckFilter("игра", "21"),
        CheckFilter("драма", "36"),
        CheckFilter("ёнкома", "5"),
        CheckFilter("игра", "21"),
        CheckFilter("исекай", "37"),
        CheckFilter("история", "6"),
        CheckFilter("киберпанк", "22"),
        CheckFilter("кодомо", "38"),
        CheckFilter("комедия", "7"),
        CheckFilter("махо-сёдзё", "23"),
        CheckFilter("меха", "39"),
        CheckFilter("мистика", "8"),
        CheckFilter("научная фантастика", "24"),
        CheckFilter("омегаверс", "40"),
        CheckFilter("повседневность", "9"),
        CheckFilter("постапокалиптика", "25"),
        CheckFilter("приключения", "41"),
        CheckFilter("психология", "10"),
        CheckFilter("романтика", "26"),
        CheckFilter("самурайский боевик", "42"),
        CheckFilter("сверхъестественное", "11"),
        CheckFilter("сёдзё", "27"),
        CheckFilter("сёдзё-ай", "43"),
        CheckFilter("сёнэн", "13"),
        CheckFilter("спорт", "44"),
        CheckFilter("сэйнэн", "12"),
        CheckFilter("трагедия", "29"),
        CheckFilter("триллер", "45"),
        CheckFilter("ужасы", "14"),
        CheckFilter("фантастика", "30"),
        CheckFilter("фэнтези", "46"),
        CheckFilter("школа", "15"),
        CheckFilter("элементы юмора", "1"),
        CheckFilter("эротика", "31"),
        CheckFilter("этти", "47"),
        CheckFilter("юри", "16"),
        CheckFilter("яой", "32"),
    )
    companion object {
        private const val API_URL = "https://api.newmanga.org/v2"
        private const val IMAGE_URL = "https://storage.newmanga.org"
    }
    private val json: Json by injectLazy()
}
