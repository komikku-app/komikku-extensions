package eu.kanade.tachiyomi.extension.id.doujindesu

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.text.SimpleDateFormat
import java.util.Locale

class DoujinDesu : ParsedHttpSource() {
    // Information : DoujinDesu use EastManga WordPress Theme
    override val name = "Doujindesu"
    override val baseUrl = "https://doujindesu.xxx"
    override val lang = "id"
    override val supportsLatest = true
    override val client: OkHttpClient = network.cloudflareClient

    // Private stuff

    private val DATE_FORMAT by lazy {
        SimpleDateFormat("MMMM d, yyyy", Locale("id"))
    }

    private fun parseStatus(status: String) = when {
        status.toLowerCase(Locale.US).contains("finished") -> SManga.ONGOING
        status.toLowerCase(Locale.US).contains("publishing") -> SManga.COMPLETED
        else -> SManga.UNKNOWN
    }

    private class Category(title: String, val key: String) : Filter.TriState(title) {
        override fun toString(): String {
            return name
        }
    }

    private class Genre(title: String, val key: String) : Filter.TriState(title) {
        override fun toString(): String {
            return name
        }
    }

    private class Order(title: String, val key: String) : Filter.TriState(title) {
        override fun toString(): String {
            return name
        }
    }

    private class Status(title: String, val key: String) : Filter.TriState(title) {
        override fun toString(): String {
            return name
        }
    }

    private val orderBy = arrayOf(
        Order("All", ""),
        Order("A-Z", "title"),
        Order("Z-A", "titlereverse"),
        Order("Latest Update", "update"),
        Order("Latest Added", "latest"),
        Order("Popular", "popular")
    )

    private val statusList = arrayOf(
        Status("All", ""),
        Status("Publishing", "Publishing"),
        Status("Finished", "Finished")
    )

    private val genreList = arrayOf(
        Genre("All", ""),
        Genre("Age Regression", "age-regression"),
        Genre("Ahegao", "ahegao"),
        Genre("All The Way Through", "all-the-way-through"),
        Genre("Amputee", "amputee"),
        Genre("Anal", "anal"),
        Genre("Anorexia", "anorexia"),
        Genre("Apron", "apron"),
        Genre("Artist CG", "artist-cg"),
        Genre("Aunt", "aunt"),
        Genre("Bald", "bald"),
        Genre("Bestiality", "bestiality"),
        Genre("Big As", "big-as"),
        Genre("Big Ass", "big-ass"),
        Genre("Big Breast", "big-breast"),
        Genre("Big Penis", "big-penis"),
        Genre("Bike Shorts", "bike-shorts"),
        Genre("Bikini", "bikini"),
        Genre("Birth", "birth"),
        Genre("Bisexual", "bisexual"),
        Genre("Blackmail", "blackmail"),
        Genre("Blindfold", "blindfold"),
        Genre("Bloomers", "bloomers"),
        Genre("Blowjob", "blowjob"),
        Genre("Body Swap", "body-swap"),
        Genre("Bodysuit", "bodysuit"),
        Genre("Bondage", "bondage"),
        Genre("Business Suit", "business-suit"),
        Genre("Cheating", "cheating"),
        Genre("Collar", "collar"),
        Genre("Condom", "condom"),
        Genre("Cousin", "cousin"),
        Genre("Crossdressing", "crossdressing"),
        Genre("Cunnilingus", "cunnilingus"),
        Genre("Dark Skin", "dark-skin"),
        Genre("Daughter", "daughter"),
        Genre("Defloartion", "defloartion"),
        Genre("Defloration", "defloration"),
        Genre("Demon", "demon"),
        Genre("Demon Girl", "demon-girl"),
        Genre("Dick Growth", "dick-growth"),
        Genre("DILF", "dilf"),
        Genre("Double Penetration", "double-penetration"),
        Genre("Drugs", "drugs"),
        Genre("Drunk", "drunk"),
        Genre("Elf", "elf"),
        Genre("Emotionless Sex", "emotionless-sex"),
        Genre("Exhibitionism", "exhibitionism"),
        Genre("Eyepatch", "eyepatch"),
        Genre("Fantasy", "fantasy"),
        Genre("Females Only", "females-only"),
        Genre("Femdom", "femdom"),
        Genre("Filming", "filming"),
        Genre("Fingering", "fingering"),
        Genre("Footjob", "footjob"),
        Genre("Full Color", "full-color"),
        Genre("Furry", "furry"),
        Genre("Futanari", "futanari"),
        Genre("Garter Belt", "garter-belt"),
        Genre("Gender Bender", "gender-bender"),
        Genre("Ghost", "ghost"),
        Genre("Glasses", "glasses"),
        Genre("Gore", "gore"),
        Genre("Group", "group"),
        Genre("Guro", "guro"),
        Genre("Gyaru", "gyaru"),
        Genre("Hairy", "hairy"),
        Genre("Handjob", "handjob"),
        Genre("Harem", "harem"),
        Genre("Horns", "horns"),
        Genre("Huge Breast", "huge-breast"),
        Genre("Humiliation", "humiliation"),
        Genre("Impregnation", "impregnation"),
        Genre("Incest", "incest"),
        Genre("Inflation", "inflation"),
        Genre("Insect", "insect"),
        Genre("Inseki", "inseki"),
        Genre("Inverted Nipples", "inverted-nipples"),
        Genre("Invisible", "invisible"),
        Genre("Kemomimi", "kemomimi"),
        Genre("Kimono", "kimono"),
        Genre("Lactation", "lactation"),
        Genre("Leotard", "leotard"),
        Genre("Lingerie", "lingerie"),
        Genre("Loli", "loli"),
        Genre("Lolipai", "lolipai"),
        Genre("Maid", "maid"),
        Genre("Males Only", "males-only"),
        Genre("Masturbation", "masturbation"),
        Genre("Miko", "miko"),
        Genre("MILF", "milf"),
        Genre("Mind Break", "mind-break"),
        Genre("Mind Control", "mind-control"),
        Genre("Minigirl", "minigirl"),
        Genre("Miniguy", "miniguy"),
        Genre("Monster", "monster"),
        Genre("Monster Girl", "monster-girl"),
        Genre("Mother", "mother"),
        Genre("Multi-work Series", "multi-work-series"),
        Genre("Muscle", "muscle")
    )

    private val categoryNames = arrayOf(
        Category("All", ""),
        Category("Manga", "Manga"),
        Category("Manhua", "Manhua"),
        Category("Doujinshi", "Doujinshi"),
        Category("Manhwa", "Manhwa")
    )

    private class CategoryNames(categories: Array<Category>) : Filter.Select<Category>("Category", categories, 0)
    private class OrderBy(orders: Array<Order>) : Filter.Select<Order>("Order", orders, 0)
    private class GenreList(genres: Array<Genre>) : Filter.Select<Genre>("Genre", genres, 0)
    private class StatusList(statuses: Array<Status>) : Filter.Select<Status>("Status", statuses, 0)

    private fun basicInformationFromElement(element: Element): SManga {
        val manga = SManga.create()

        manga.title = element.select("div > div > a").attr("alt")
        manga.setUrlWithoutDomain(element.select("div > div > a").attr("href"))
        manga.thumbnail_url = element.select("div > div > a > div > img").attr("src")

        return manga
    }

    private fun getNumberFromString(epsStr: String): String {
        return epsStr.filter { it.isDigit() }
    }

    private fun reconstructDate(dateStr: String): Long {
        return runCatching { DATE_FORMAT.parse(dateStr)?.time }
            .getOrNull() ?: 0L
    }

    private fun getImage(element: Element): Page {
        return Page(getNumberFromString(element.attr("img-id")).toInt(), "", element.attr("src"))
    }

    // Popular

    override fun popularMangaFromElement(element: Element): SManga = basicInformationFromElement(element)

    override fun popularMangaRequest(page: Int): Request {
        return GET("$baseUrl/komik-list/page/$page/?&order=popular")
    }

    // Latest

    override fun latestUpdatesFromElement(element: Element): SManga = basicInformationFromElement(element)

    override fun latestUpdatesRequest(page: Int): Request {
        return GET("$baseUrl/komik-list/page/$page/?order=update")
    }

    // Element Selectors

    override fun latestUpdatesSelector(): String = "#main > div.relat > article"
    override fun popularMangaSelector(): String = "#main > div.relat > article"
    override fun searchMangaSelector(): String = "#main > div.relat > article"

    override fun popularMangaNextPageSelector(): String = "#nextpagination"
    override fun latestUpdatesNextPageSelector() = popularMangaNextPageSelector()
    override fun searchMangaNextPageSelector() = popularMangaNextPageSelector()

    // Search & FIlter

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        var url = "$baseUrl/komik-list/page/$page/".toHttpUrlOrNull()?.newBuilder()!!.addQueryParameter("title", query)
        (if (filters.isEmpty()) getFilterList() else filters).forEach { filter ->
            when (filter) {
                is CategoryNames -> {
                    val category = filter.values[filter.state]
                    url.addQueryParameter("type", category.key)
                }
                is OrderBy -> {
                    val order = filter.values[filter.state]
                    url.addQueryParameter("order", order.key)
                }
                is GenreList -> {
                    val genre = filter.values[filter.state]
                    url.addQueryParameter("genre", genre.key)
                }
                is StatusList -> {
                    val status = filter.values[filter.state]
                    url.addQueryParameter("status", status.key)
                }
            }
        }
        return GET(url.toString(), headers)
    }

    override fun searchMangaFromElement(element: Element): SManga = basicInformationFromElement(element)

    override fun getFilterList() = FilterList(
        CategoryNames(categoryNames),
        OrderBy(orderBy),
        GenreList(genreList),
        StatusList(statusList)
    )

    // Detail Parse

    override fun mangaDetailsParse(document: Document): SManga {
        val manga = SManga.create()
        manga.description = when {
            document.select("div.infox > div.entry-content.entry-content-single > p").isEmpty() -> "No description specified"
            else -> document.select("div.infox > div.entry-content.entry-content-single > p").first().text()
        }
        manga.author = document.select("div.infox > div.spe > span:nth-child(5)").text()
        manga.genre = document.select("div.genre-info > a[itemprop=genre]").joinToString { it.text() }
        manga.status = parseStatus(document.select("div.infox > div.spe > span:nth-child(1)").text())
        manga.thumbnail_url = document.select("div.thumb > img").attr("src")
        manga.artist = document.select("div.infox > div.spe > span:nth-child(6)").text()

        return manga
    }

    // Chapter Stuff

    override fun chapterFromElement(element: Element): SChapter {
        val chapter = SChapter.create()
        val number = getNumberFromString(element.select("div.epsright > span > a > chapter").text())
        chapter.chapter_number = when {
            (number.isNotEmpty()) -> number.toFloat()
            else -> 1F
        }
        chapter.date_upload = reconstructDate(element.select("div.epsleft > span.date").text())
        chapter.name = element.select("div.epsleft > span.lchx > a").text()
        chapter.setUrlWithoutDomain(element.select("div.epsleft > span.lchx > a").attr("href"))

        return chapter
    }

    override fun chapterListSelector(): String = "#chapter_list li"

    // More parser stuff
    override fun imageUrlParse(document: Document): String = throw UnsupportedOperationException("Not Used")

    override fun pageListParse(document: Document): List<Page> {
        return document.select("div.reader-area > img").mapIndexed { i, element ->
            Page(i, "", element.attr("src"))
        }
    }
}
