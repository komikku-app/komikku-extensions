package eu.kanade.tachiyomi.extension.ar.mangaalarab

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale

class MangaAlarab : ParsedHttpSource() {

    override val name = "MangaAlarab"

    override val baseUrl = "https://mangaalarab.com"

    override val lang = "ar"

    override val supportsLatest = true

    // Popular

    override fun popularMangaRequest(page: Int): Request {
        return GET("$baseUrl/manga?page=$page")
    }

    override fun popularMangaSelector() = "article"

    override fun popularMangaFromElement(element: Element): SManga {
        return SManga.create().apply {
            element.select("a").let {
                setUrlWithoutDomain(it.attr("abs:href"))
                title = element.select("h3").text()
                thumbnail_url = element.select("figure img").attr("data-src")
            }
        }
    }

    override fun popularMangaNextPageSelector() = "a[rel=next]"

    // Latest

    override fun latestUpdatesRequest(page: Int): Request {
        return GET(baseUrl)
    }

    override fun latestUpdatesSelector() = "section:nth-child(5) > div.container > div > article"

    override fun latestUpdatesFromElement(element: Element): SManga {
        return SManga.create().apply {
            element.select("figure > a").let {
                setUrlWithoutDomain(it.attr("abs:href"))
                title = element.select("img").attr("title")
                thumbnail_url = element.select("img").attr("data-src")
            }
        }
    }

    override fun latestUpdatesNextPageSelector(): String? = null

    // Search

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        return if (query.isNotBlank()) {
            GET("$baseUrl/search?q=$query&page=$page", headers)
        } else {
            val url = "$baseUrl/manga?page=$page".toHttpUrlOrNull()!!.newBuilder()
            filters.forEach { filter ->
                when (filter) {
                    is SortFilter -> url.addQueryParameter("order", filter.toUriPart())
                    is OTypeFilter -> url.addQueryParameter("order_type", filter.toUriPart())
                    is StatusFilter -> {
                        filter.state
                            .filter { it.state != Filter.TriState.STATE_IGNORE }
                            .forEach { url.addQueryParameter("statuses[]", it.id) }
                    }
                    is TypeFilter -> {
                        filter.state
                            .filter { it.state != Filter.TriState.STATE_IGNORE }
                            .forEach { url.addQueryParameter("types[]", it.id) }
                    }
                    is GenreFilter -> {
                        filter.state
                            .filter { it.state != Filter.TriState.STATE_IGNORE }
                            .forEach { url.addQueryParameter("genres[]", it.id) }
                    }
                    is GenresSelection -> url.addQueryParameter("genresSelection", filter.toUriPart())
                }
            }
            GET(url.build().toString(), headers)
        }
    }

    override fun searchMangaSelector() = popularMangaSelector()

    override fun searchMangaFromElement(element: Element): SManga = popularMangaFromElement(element)

    override fun searchMangaNextPageSelector() = popularMangaNextPageSelector()

    // Details

    override fun mangaDetailsParse(document: Document): SManga {
        return SManga.create().apply {
            document.select("article").first().let { info ->
                description = info.select("p.text-sm").text()
            }

            // add series type(manga/manhwa/manhua/other) thinggy to genre
            genre = document.select("div.text-gray-600 a, div.container > div:nth-child(1) > div:nth-child(1) > div:nth-child(2) > div:nth-child(1) > span:nth-child(2)").joinToString(", ") { it.text() }

            // add series Status to manga description
            document.select("div.container > div:nth-child(1) > div:nth-child(1) > div:nth-child(2) > div:nth-child(6) > span:nth-child(2)")?.first()?.text()?.also { statusText ->
                when {
                    statusText.contains("مستمرة", true) -> status = SManga.ONGOING
                    statusText.contains("مكتملة", true) -> status = SManga.COMPLETED
                    else -> status = SManga.UNKNOWN
                }
            }

            // add alternative name to manga description
            document.select("article span").text()?.let {
                if (it.isEmpty().not()) {
                    description += when {
                        description!!.isEmpty() -> "Alternative Name: $it"
                        else -> "\n\nAlternativ Name: $it"
                    }
                }
            }
        }
    }

    // Chapters

    override fun chapterListSelector() = "div.chapters-container > div > a"

    override fun chapterFromElement(element: Element): SChapter {
        return SChapter.create().apply {
            name = "${element.select("div > span").text()}"
            setUrlWithoutDomain(element.attr("href"))
            date_upload = element.select("div > time").firstOrNull()?.text()
                ?.let { parseChapterDate(it) } ?: 0
        }
    }

    private fun parseChapterDate(date: String): Long {
        var parsedDate = 0L
        try {
            parsedDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(date)?.time ?: 0L
        } catch (e: ParseException) { /*nothing to do, parsedDate is initialized with 0L*/ }
        return parsedDate
    }

    // Pages

    override fun pageListParse(document: Document): List<Page> {
        return document.select("div.container > div > div > img").mapIndexed { i, img ->
            Page(i, "", img.attr("abs:src"))
        }
    }

    override fun imageUrlParse(document: Document): String = throw UnsupportedOperationException("Not used")

    // Filters

    override fun getFilterList() = FilterList(
        Filter.Header("NOTE: Ignored if using text search!"),
        Filter.Separator(),
        SortFilter(getSortFilters()),
        OTypeFilter(getOTypeFilters()),
        GenresSelection(getGenresSelection()),
        Filter.Separator(),
        Filter.Header("exclusion not available for This source"),
        StatusFilter(getStatusFilters()),
        TypeFilter(getTypeFilter()),
        Filter.Separator(),
        Filter.Header("Genre exclusion not available for This source"),
        GenreFilter(getGenreFilters()),
    )

    private class SortFilter(vals: Array<Pair<String?, String>>) : UriPartFilter("Order by", vals)
    private class OTypeFilter(vals: Array<Pair<String?, String>>) : UriPartFilter("Order Type", vals)
    private class GenresSelection(vals: Array<Pair<String?, String>>) : UriPartFilter("Genres Selection", vals)
    class Type(name: String, val id: String = name) : Filter.TriState(name)
    private class TypeFilter(types: List<Type>) : Filter.Group<Type>("Type", types)
    class Status(name: String, val id: String = name) : Filter.TriState(name)
    private class StatusFilter(statuses: List<Status>) : Filter.Group<Status>("Status", statuses)
    class Genre(name: String, val id: String = name) : Filter.TriState(name)
    private class GenreFilter(genres: List<Genre>) : Filter.Group<Genre>("Genre", genres)

    private fun getSortFilters(): Array<Pair<String?, String>> = arrayOf(
        Pair("latest", "التحديثات الاخيرة"),
        Pair("chapters", "عدد الفصول"),
        Pair("release", "تاريخ الاصدار"),
        Pair("followers", "المتابعين"),
        Pair("rating", "التقييم")
    )

    private fun getOTypeFilters(): Array<Pair<String?, String>> = arrayOf(
        Pair("desc", "تنازلي"),
        Pair("asc", "تصاعدي")
    )

    open fun getStatusFilters(): List<Status> = listOf(
        Status("مكتملة", "completed"),
        Status("مستمرة", "ongoing"),
        Status("متوقفة", "cancelled"),
        Status("في الانتظار", "onhold")
    )

    open fun getTypeFilter(): List<Type> = listOf(
        Type("صينية (مانها)", "manhua"),
        Type("كورية (مانهوا)", "manhwa"),
        Type("انجليزية", "english"),
        Type("مانجا (يابانية)", "manga")
    )

    open fun getGenresSelection(): Array<Pair<String?, String>> = arrayOf(
        Pair("and", "ان تحتوى المانجا على كل تصنيف تم تحديده"),
        Pair("or", "ان تحتوي المانجا على تصنيف او اكثر من ما تم تحديده")
    )

    open fun getGenreFilters(): List<Genre> = listOf(
        Genre("اكشن", "1"),
        Genre("مغامرة", "2"),
        Genre("خيالي", "3"),
        Genre("سحر", "4"),
        Genre("من ضعيف لقوي", "5"),
        Genre("زنزانة", "6"),
        Genre("بناء على رواية ويب", "7"),
        Genre("فنون قتالية", "8"),
        Genre("اعادة البعث", "9"),
        Genre("السفر عبر الزمن", "10"),
        Genre("رومانسي", "11"),
        Genre("كوميدي", "12"),
        Genre("الانتقال الى عالم اخر", "13"),
        Genre("تاريخي", "14"),
        Genre("انتقام", "15"),
        Genre("حياة مدرسية", "16"),
        Genre("مصاصي دماء", "17"),
        Genre("غموض", "18"),
        Genre("رعب", "19"),
        Genre("دراما", "20"),
        Genre("نفسي", "21")
    )

    open class UriPartFilter(displayName: String, private val vals: Array<Pair<String?, String>>) :
        Filter.Select<String>(displayName, vals.map { it.second }.toTypedArray()) {
        fun toUriPart() = vals[state].first
    }
}
