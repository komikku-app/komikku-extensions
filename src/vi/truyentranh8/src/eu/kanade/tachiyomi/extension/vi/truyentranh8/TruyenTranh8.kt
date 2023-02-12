package eu.kanade.tachiyomi.extension.vi.truyentranh8

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.interceptor.rateLimit
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Evaluator
import rx.Observable
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

class TruyenTranh8 : ParsedHttpSource() {

    override val name = "Truyện Tranh 8"

    override val baseUrl = "http://truyentranh86.com"

    override val lang = "vi"

    override val supportsLatest = true

    override val client = network.cloudflareClient.newBuilder()
        .rateLimit(1, 2, TimeUnit.SECONDS)
        .build()

    override fun headersBuilder() = Headers.Builder()
        .add("Referer", "$baseUrl/")
        .add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:103.0) Gecko/20100101 Firefox/103.0")

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("Asia/Ho_Chi_Minh")
    }

    private val floatingNumberRegex = Regex("""([+-]?(?:[0-9]*[.])?[0-9]+)""")

    override fun popularMangaRequest(page: Int) = GET(
        baseUrl.toHttpUrl().newBuilder().apply {
            addPathSegment("search.php")
            addQueryParameter("act", "search")
            addQueryParameter("sort", "xem")
            addQueryParameter("view", "thumb")
            addQueryParameter("page", page.toString())
        }.build().toString(),
        headers,
    )

    override fun popularMangaNextPageSelector(): String = "div#tblChap p.page a:contains(Cuối)"

    override fun popularMangaSelector(): String = "div#tblChap figure.col"

    override fun popularMangaFromElement(element: Element): SManga = SManga.create().apply {
        setUrlWithoutDomain(element.select("figcaption h3 a").first()!!.attr("href"))
        title = element.select("figcaption h3 a").first()!!.text().replace("[TT8] ", "")
        thumbnail_url = element.select("img").first()!!.attr("abs:src")
    }

    override fun latestUpdatesRequest(page: Int) = GET(
        baseUrl.toHttpUrl().newBuilder().apply {
            addPathSegment("search.php")
            addQueryParameter("act", "search")
            addQueryParameter("sort", "chap")
            addQueryParameter("view", "thumb")
            addQueryParameter("page", page.toString())
        }.build().toString(),
        headers,
    )

    override fun latestUpdatesNextPageSelector(): String = popularMangaNextPageSelector()

    override fun latestUpdatesSelector(): String = popularMangaSelector()

    override fun latestUpdatesFromElement(element: Element): SManga = popularMangaFromElement(element)

    override fun fetchSearchManga(page: Int, query: String, filters: FilterList): Observable<MangasPage> {
        return when {
            query.startsWith(PREFIX_ID_SEARCH) -> {
                val id = query.removePrefix(PREFIX_ID_SEARCH).trim()
                if (id.isEmpty()) {
                    throw Exception("ID tìm kiếm không hợp lệ.")
                }
                fetchMangaDetails(SManga.create().apply { url = "/truyen-tranh/$id/" })
                    .map { MangasPage(listOf(it), false) }
            }
            else -> super.fetchSearchManga(page, query, filters)
        }
    }

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList) = GET(
        baseUrl.toHttpUrl().newBuilder().apply {
            addPathSegment("search.php")
            addQueryParameter("act", "timnangcao")
            addQueryParameter("view", "thumb")
            addQueryParameter("page", page.toString())

            if (query.isNotEmpty()) {
                addQueryParameter("q", query)
            }

            (if (filters.isEmpty()) getFilterList() else filters).forEach { filter ->
                when (filter) {
                    is SortByFilter -> addQueryParameter("sort", filter.toUriPart())
                    is SearchTypeFilter -> addQueryParameter("andor", filter.toUriPart())
                    is ForFilter -> if (filter.state != 0) {
                        addQueryParameter("danhcho", filter.toUriPart())
                    }
                    is AgeFilter -> if (filter.state != 0) {
                        addQueryParameter("DoTuoi", filter.toUriPart())
                    }
                    is StatusFilter -> if (filter.state != 0) {
                        addQueryParameter("TinhTrang", filter.toUriPart())
                    }
                    is OriginFilter -> if (filter.state != 0) {
                        addQueryParameter("quocgia", filter.toUriPart())
                    }
                    is ReadingModeFilter -> if (filter.state != 0) {
                        addQueryParameter("KieuDoc", filter.toUriPart())
                    }
                    is YearFilter -> if (filter.state.isNotEmpty()) {
                        addQueryParameter("NamPhaHanh", filter.state)
                    }
                    is UserFilter -> if (filter.state.isNotEmpty()) {
                        addQueryParameter("u", filter.state)
                    }
                    is AuthorFilter -> if (filter.state.isNotEmpty()) {
                        addQueryParameter("TacGia", filter.state)
                    }
                    is SourceFilter -> if (filter.state.isNotEmpty()) {
                        addQueryParameter("Nguon", filter.state)
                    }
                    is GenreList -> {
                        addQueryParameter(
                            "baogom",
                            filter.state
                                .filter { it.state == Filter.TriState.STATE_INCLUDE }
                                .joinToString(",") { it.id },
                        )
                        addQueryParameter(
                            "khonggom",
                            filter.state
                                .filter { it.state == Filter.TriState.STATE_EXCLUDE }
                                .joinToString(",") { it.id },
                        )
                    }
                    else -> {}
                }
            }
        }.build().toString(),
        headers,
    )

    override fun searchMangaNextPageSelector(): String = popularMangaNextPageSelector()

    override fun searchMangaSelector(): String = popularMangaSelector()

    override fun searchMangaFromElement(element: Element): SManga = popularMangaFromElement(element)

    override fun mangaDetailsParse(document: Document) = SManga.create().apply {
        title = document.select("h1.fs-5").first()!!.text().replace("Truyện Tranh ", "")

        author = document.select("span[itemprop=author]").toList()
            .filter { it.text().isNotEmpty() }
            .joinToString(", ") { it.text() }

        thumbnail_url = document.select("img.thumbnail").first()!!.attr("abs:src")

        genre = document.select("a[itemprop=genre]").toList()
            .filter { it.text().isNotEmpty() }
            .joinToString(", ") { it.text() }

        status = when (document.select("ul.mangainfo b:contains(Tình Trạng) + a").first()!!.text().trim()) {
            "Đang tiến hành" -> SManga.ONGOING
            "Đã hoàn thành" -> SManga.COMPLETED
            "Tạm ngưng" -> SManga.ON_HIATUS
            else -> SManga.UNKNOWN
        }

        val descnode = document.select("div.card-body.border-start.border-info.border-3").first()!!
        descnode.select(Evaluator.Tag("br")).prepend("\\n")

        description = if (descnode.select("p").any()) {
            descnode.select("p").joinToString("\n") {
                it.text().replace("\\n", "\n").replace("\n ", "\n")
            }.trim()
        } else {
            descnode.text().replace("\\n", "\n").replace("\n ", "\n").trim()
        }
    }

    override fun chapterListSelector() = "ul#ChapList li"

    override fun chapterFromElement(element: Element) = SChapter.create().apply {
        setUrlWithoutDomain(element.select("a").first()!!.attr("abs:href"))
        name = element.text().replace(element.select("time").first()!!.text(), "")
        date_upload = runCatching {
            dateFormatter.parse(element.select("time").first()!!.attr("datetime"))?.time
        }.getOrNull() ?: 0L

        val match = floatingNumberRegex.find(name)
        chapter_number = if (name.lowercase().startsWith("vol")) {
            match?.groups?.get(2)
        } else {
            match?.groups?.get(1)
        }?.value?.toFloat() ?: -1f
    }

    override fun pageListParse(document: Document) = document.select("div.page-chapter")
        .mapIndexed { i, elem ->
            Page(i, "", elem.select("img").first()!!.attr("abs:src"))
        }

    override fun imageUrlParse(document: Document): String = throw Exception("Not used")

    open class UriPartFilter(displayName: String, private val vals: Array<Pair<String, String>>, state: Int = 0) :
        Filter.Select<String>(displayName, vals.map { it.first }.toTypedArray(), state) {
        fun toUriPart() = vals[state].second
    }

    private class YearFilter : Filter.Text("Năm phát hành")
    private class UserFilter : Filter.Text("Đăng bởi thành viên")
    private class AuthorFilter : Filter.Text("Tên tác giả")
    private class SourceFilter : Filter.Text("Nguồn/Nhóm dịch")
    private class SearchTypeFilter : UriPartFilter(
        "Kiểu tìm",
        arrayOf(
            Pair("AND/và", "and"),
            Pair("OR/hoặc", "or"),
        ),
    )
    private class ForFilter : UriPartFilter(
        "Dành cho",
        arrayOf(
            Pair("Bất kì", ""),
            Pair("Con gái", "gai"),
            Pair("Con trai", "trai"),
            Pair("Con nít", "nit"),
        ),
    )
    private class AgeFilter : UriPartFilter(
        "Bất kỳ",
        arrayOf(
            Pair("Bất kì", ""),
            Pair("= 13", "13"),
            Pair("= 14", "14"),
            Pair("= 15", "15"),
            Pair("= 16", "16"),
            Pair("= 17", "17"),
            Pair("= 18", "18"),
        ),
    )
    private class StatusFilter : UriPartFilter(
        "Tình trạng",
        arrayOf(
            Pair("Bất kì", ""),
            Pair("Đang dịch", "Ongoing"),
            Pair("Hoàn thành", "Complete"),
            Pair("Tạm ngưng", "Drop"),
        ),
    )
    private class OriginFilter : UriPartFilter(
        "Quốc gia",
        arrayOf(
            Pair("Bất kì", ""),
            Pair("Nhật Bản", "nhat"),
            Pair("Trung Quốc", "trung"),
            Pair("Hàn Quốc", "han"),
            Pair("Việt Nam", "vietnam"),
        ),
    )
    private class ReadingModeFilter : UriPartFilter(
        "Kiểu đọc",
        arrayOf(
            Pair("Bất kì", ""),
            Pair("Chưa xác định", "chưa xác định"),
            Pair("Phải qua trái", "xem từ phải qua trái"),
            Pair("Trái qua phải", "xem từ trái qua phải"),
        ),
    )
    private class SortByFilter : UriPartFilter(
        "Sắp xếp theo",
        arrayOf(
            Pair("Chap mới", "chap"),
            Pair("Truyện mới", "truyen"),
            Pair("Xem nhiều", "xem"),
            Pair("Theo ABC", "ten"),
            Pair("Số Chương", "sochap"),
        ),
        2,
    )
    open class Genre(name: String, val id: String) : Filter.TriState(name)
    private class GenreList(genres: List<Genre>) : Filter.Group<Genre>("Thể loại", genres)
    override fun getFilterList() = FilterList(
        GenreList(getGenreList()),
        SortByFilter(),
        SearchTypeFilter(),
        ForFilter(),
        AgeFilter(),
        StatusFilter(),
        OriginFilter(),
        ReadingModeFilter(),
        YearFilter(),
        UserFilter(),
        AuthorFilter(),
        SourceFilter(),
    )

    private fun getGenreList() = listOf(
        Genre("Phát Hành Tại TT8", "106"),
        Genre("Truyện Màu", "113"),
        Genre("Webtoons", "112"),
        Genre("Manga - Truyện Nhật", "141"),
        Genre("Action - Hành động", "52"),
        Genre("Adult - Người lớn", "53"),
        Genre("Adventure - Phiêu lưu", "65"),
        Genre("Anime", "107"),
        Genre("Biseinen", "123"),
        Genre("Bishounen", "122"),
        Genre("Comedy - Hài hước", "50"),
        Genre("Doujinshi", "72"),
        Genre("Drama", "73"),
        Genre("Ecchi", "74"),
        Genre("Fantasy", "75"),
        Genre("Gender Bender - Đổi giới tính", "76"),
        Genre("Harem", "77"),
        Genre("Historical - Lịch sử", "78"),
        Genre("Horror - Kinh dị", "79"),
        Genre("Isekai - Xuyên không", "139"),
        Genre("Josei", "80"),
        Genre("Live-action - Live Action", "81"),
        Genre("Macgic", "138"),
        Genre("Magic - Phép thuật", "116"),
        Genre("Martial Arts - Martial-Arts", "84"),
        Genre("Mature - Trưởng thành", "85"),
        Genre("Mecha - Robot", "86"),
        Genre("Mystery - Bí ẩn", "87"),
        Genre("One-shot", "88"),
        Genre("Psychological - Tâm lý", "89"),
        Genre("Romance - Tình cảm", "90"),
        Genre("School Life - Học đường", "91"),
        Genre("Sci fi - Khoa học viễn tưởng", "92"),
        Genre("Seinen", "93"),
        Genre("Shoujo", "94"),
        Genre("Shoujo Ai", "66"),
        Genre("Shounen", "96"),
        Genre("Shounen Ai", "97"),
        Genre("Slash", "121"),
        Genre("Slice-of-Life - Đời sống", "98"),
        Genre("Smut", "99"),
        Genre("Soft Yaoi - Soft-Yaoi", "100"),
        Genre("Sports - Thể thao", "101"),
        Genre("Supernatural - Siêu nhiên", "102"),
        Genre("Tạp chí truyện tranh", "103"),
        Genre("Tragedy - Bi kịch", "104"),
        Genre("Trap - Crossdressing", "115"),
        Genre("Yaoi", "114"),
        Genre("Yaoi Hardcore", "120"),
        Genre("Yuri", "111"),
        Genre("Manhua - Truyện Trung", "82"),
        Genre("Bách Hợp", "128"),
        Genre("Chuyển sinh", "134"),
        Genre("Cổ đại", "135"),
        Genre("Cung đình", "144"),
        Genre("Giới giải trí", "146"),
        Genre("Hậu cung", "145"),
        Genre("Huyền Huyễn", "132"),
        Genre("Khoa Huyễn", "130"),
        Genre("Lịch Sử", "131"),
        Genre("Ngôn tình", "127"),
        Genre("Ngọt sủng", "148"),
        Genre("Ngược", "143"),
        Genre("Người đóng góp", "147"),
        Genre("Nữ Cường", "136"),
        Genre("Tổng tài", "137"),
        Genre("Trọng Sinh", "126"),
        Genre("Trường học", "142"),
        Genre("Tu chân - tu tiên", "140"),
        Genre("Võng Du", "125"),
        Genre("Xuyên không", "124"),
        Genre("Đam Mỹ", "108"),
        Genre("Đô thị", "129"),
        Genre("Manhwa - Truyện Hàn", "83"),
        Genre("Boy love", "133"),
        Genre("Thriller - Giết người, sát nhân, máu me", "149"),
        Genre("Truyện Tranh Việt", "51"),
        Genre("Cướp bồ  - NTR, Netorare", "118"),
        Genre("Hướng dẫn vẽ!", "109"),
        Genre("Truyện scan", "105"),
        Genre("Comic - truyện Âu Mĩ", "71"),
    )

    companion object {
        const val PREFIX_ID_SEARCH = "id:"
    }
}
