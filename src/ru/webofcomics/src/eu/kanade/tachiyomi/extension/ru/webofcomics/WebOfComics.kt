package eu.kanade.tachiyomi.extension.ru.webofcomics

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.interceptor.rateLimit
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
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class WebOfComics : ParsedHttpSource() {

    override val name = "Web of Comics"

    override val baseUrl = "https://webofcomics.ru"

    override val lang = "ru"

    override val supportsLatest = true

    override fun headersBuilder(): Headers.Builder = Headers.Builder()
        .add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/100.0.4896.127 Safari/537.36 Edg/100.0.1185.50")
        .add("Referer", baseUrl)

    override val client: OkHttpClient = network.cloudflareClient.newBuilder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .rateLimit(3)
        .build()

    override fun popularMangaRequest(page: Int): Request {
        return POST(
            "$baseUrl/page/$page",
            body = FormBody.Builder()
                .add("dlenewssortby", "rating")
                .add("dledirection", "desc")
                .add("set_new_sort", "dle_sort_main")
                .add("set_direction_sort", "dle_direction_main")
                .build(),
            headers = headers,
        )
    }

    override fun latestUpdatesRequest(page: Int): Request {
        return POST(
            "$baseUrl/page/$page",
            body = FormBody.Builder()
                .add("dlenewssortby", "date")
                .add("dledirection", "desc")
                .add("set_new_sort", "dle_sort_main")
                .add("set_direction_sort", "dle_direction_main")
                .build(),
            headers = headers,
        )
    }

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        if (query.isNotEmpty()) {
            return POST(
                "$baseUrl/index.php?do=search",
                body = FormBody.Builder()
                    .add("do", "search")
                    .add("subaction", "search")
                    .add("story", query)
                    .add("search_start", page.toString())
                    .build(),
                headers = headers,
            )
        }
        val mutableGenre = mutableListOf<String>()
        val mutableType = mutableListOf<String>()
        val mutableAge = mutableListOf<String>()
        val mutableStatus = mutableListOf<String>()
        var publisherCat = ""
        var orderBy = "desc"
        var ascEnd = "rating"
        var sectionOr = "All"
        (if (filters.isEmpty()) getFilterList() else filters).forEach { select ->
            when (select) {
                is OrderBy -> {
                    orderBy = arrayOf("date", "rating", "news_read", "comm_num", "title", "d.year", "d.multirating")[select.state!!.index]
                    ascEnd = if (select.state!!.ascending) "asc" else "desc"
                }
                is AgeList -> select.state.forEach { age ->
                    if (age.state) {
                        mutableAge += age.name
                    }
                }
                is StatusList -> select.state.forEach { status ->
                    if (status.state) {
                        mutableStatus += status.name
                    }
                }
                is Section -> {
                    if (select.state == 1) {
                        sectionOr = "Comics"
                        filters.forEach { filter ->
                            when (filter) {
                                is GenreComics -> filter.state.forEach { genre ->
                                    if (genre.state) {
                                        mutableGenre += genre.name
                                    }
                                }
                                is TypeComics -> filter.state.forEach { type ->
                                    if (type.state) {
                                        mutableType += type.name
                                    }
                                }
                                is PublishersComics -> {
                                    if (filter.state > 0) {
                                        publisherCat = getPublishersComics()[filter.state].id
                                    }
                                }
                                else -> {}
                            }
                        }
                    }
                    if (select.state == 2) {
                        sectionOr = "Manga"
                        filters.forEach { filter ->
                            when (filter) {
                                is GenreManga -> filter.state.forEach { genre ->
                                    if (genre.state) {
                                        mutableGenre += genre.name
                                    }
                                }
                                is TypeManga -> filter.state.forEach { type ->
                                    if (type.state) {
                                        mutableType += type.name
                                    }
                                }
                                is PublishersManga -> {
                                    if (filter.state > 0) {
                                        publisherCat = getPublishersManga()[filter.state].id
                                    }
                                }
                                else -> {}
                            }
                        }
                    }
                }
                else -> {}
            }
        }

        if (sectionOr == "Comics") {
            return GET("$baseUrl/f/age=${mutableAge.joinToString(",")}/comicsormanga=Comics/o.cat=$publisherCat/translatestatus=${mutableStatus.joinToString(",")}/genre=${mutableGenre.joinToString(",")}/type=${mutableType.joinToString(",")}/sort=$orderBy/order=$ascEnd/page/$page", headers)
        }

        if (sectionOr == "Manga") {
            return GET("$baseUrl/f/age=${mutableAge.joinToString(",")}/comicsormanga=Manga/o.cat=$publisherCat/translatestatus=${mutableStatus.joinToString(",")}/genremanga=${mutableGenre.joinToString(",")}/typemanga=${mutableType.joinToString(",")}/sort=$orderBy/order=$ascEnd/page/$page", headers)
        }

        return POST(
            "$baseUrl/page/$page",
            body = FormBody.Builder()
                .add("dlenewssortby", orderBy)
                .add("dledirection", ascEnd)
                .add("set_new_sort", "dle_sort_main")
                .add("set_direction_sort", "dle_direction_main")
                .build(),
            headers = headers,
        )
    }

    override fun searchMangaParse(response: Response): MangasPage {
        val document = response.asJsoup()

        val mangas = document.select(searchMangaSelector()).map { element ->
            searchMangaFromElement(element)
        }
        return MangasPage(mangas, mangas.isNotEmpty())
    }

    override fun popularMangaSelector() = ".movie-item"

    override fun latestUpdatesSelector() = popularMangaSelector()

    override fun searchMangaSelector() = popularMangaSelector()

    override fun popularMangaFromElement(element: Element): SManga {
        val manga = SManga.create()
        manga.thumbnail_url = baseUrl + element.select(".lazyload").first()!!.attr("data-src").replace("/thumbs", "")
        element.select(".movie-title").first()!!.let {
            manga.setUrlWithoutDomain(it.attr("href"))
            manga.title = it.html().substringBefore("<div>")
        }
        return manga
    }

    override fun latestUpdatesFromElement(element: Element) = popularMangaFromElement(element)

    override fun searchMangaFromElement(element: Element) = popularMangaFromElement(element)

    override fun popularMangaNextPageSelector() = ".pnext a"

    override fun latestUpdatesNextPageSelector() = popularMangaNextPageSelector()

    override fun searchMangaNextPageSelector(): String? = null

    private fun parseStatus(status: String): Int {
        return when (status) {
            "Завершён" -> SManga.COMPLETED
            "Продолжается" -> SManga.ONGOING
            else -> SManga.UNKNOWN
        }
    }

    override fun mangaDetailsParse(document: Document): SManga {
        val ratingValue = document.select(".multirating-itog-rateval").text().toFloat()
        val ratingVotes = document.select(".multirating-itog-votes").text()
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

        val manga = SManga.create()
        val infoElement = document.select(".page-cols").first()!!
        val infoElement2 = document.select(".m-info2 .sliceinfo1")
        manga.title = infoElement.select("h1").first()!!.text()
        manga.thumbnail_url = baseUrl + infoElement.select(".lazyload").first()!!.attr("data-src")
        manga.description = infoElement.select("H2").first()!!.text() + "\n" + ratingStar + " " + ratingValue + " (голосов: " + ratingVotes + ")\n" + Jsoup.parse(document.select(".slice-this").first()!!.html().replace("<br>", "REPLACbR")).text().replace("REPLACbR", "\n").substringAfter("Описание:").trim()
        manga.author = infoElement2.select(":contains(Автор) a").joinToString { it.text() }
        if (manga.author.isNullOrEmpty()) {
            manga.author = infoElement.select(".mi-item:contains(Издательство)").first()!!.text()
        }
        manga.artist = infoElement2.select(":contains(Художник) a").joinToString { it.text() }
        manga.genre = (infoElement.select(".mi-item:contains(Тип) a") + infoElement.select(".mi-item:contains(Возраст) a") + infoElement.select(".mi-item:contains(Формат) a") + infoElement.select(".mi-item:contains(Жанр) a")).joinToString { it.text() }
        manga.status = if (document.toString().contains("Удалено по просьбе правообладателя")) {
            SManga.LICENSED
        } else {
            parseStatus(infoElement.select(".mi-item:contains(Перевод) a").first()!!.text())
        }

        return manga
    }

    override fun chapterListRequest(manga: SManga): Request {
        val typeSeries = if (manga.url.contains("/manga/")) {
            "xsort='tommanga,glavamanga' template='custom-linkstocomics-xfmanga-guest'"
        } else {
            "xsort='number' template='custom-linkstocomics-xfcomics-guest'"
        }

        return POST(
            "$baseUrl/engine/ajax/customajax.php",
            body = FormBody.Builder()
                .add("castom", "custom senxf='fastnavigation|${manga.url.substringAfterLast("/").substringBefore("-")}' $typeSeries limit='3000' sort='asc' cache='yes'")
                .build(),
            headers = headers,
        )
    }

    override fun chapterListSelector() = ".ltcitems:has(a:not(.alttranslatelink))"

    override fun chapterFromElement(element: Element): SChapter {
        val chapter = SChapter.create()
        element.select("a").first()!!.let {
            val numberSection = it.text().substringBefore(" - ")
            chapter.name = it.text().substringAfterLast(":")
            chapter.chapter_number = if (numberSection.contains("#")) {
                numberSection.substringAfter("#").replace("-", ".").toFloatOrNull() ?: -1f
            } else {
                numberSection.substringAfter("Глава").substringAfter("-").toFloatOrNull() ?: -1f
            }
            chapter.setUrlWithoutDomain(it.attr("href"))
        }
        chapter.date_upload = simpleDateFormat.parse(element.select("div").first()!!.text().trim())?.time ?: 0L
        return chapter
    }

    override fun chapterListParse(response: Response): List<SChapter> {
        return super.chapterListParse(response).reversed()
    }

    override fun pageListParse(document: Document): List<Page> {
        var baseImgUrl = document.select("link[rel='image_src']").last()!!.attr("href")

        val publicUrl = "/public_html"
        val uploadUrl =
            with(baseImgUrl) {
                when {
                    contains("/uploads/") -> "/uploads/"
                    contains("/manga/") -> "/manga/"
                    contains("/mangaparser/") -> "/mangaparser/"
                    else -> "errorUploads"
                }
            }
        baseImgUrl = baseImgUrl.substringBefore(uploadUrl)
        if (baseImgUrl.contains(publicUrl)) {
            baseImgUrl =
                baseImgUrl.substringBefore(publicUrl) + "/www/" +
                baseUrl.substringAfter("://") + publicUrl +
                baseImgUrl.substringAfter(publicUrl)
        }

        if (document.select(".readtab .lazyload").isNotEmpty()) {
            return document.select(".readtab .lazyload").mapIndexed { index, element ->
                Page(
                    index,
                    "",
                    baseImgUrl + uploadUrl + element.attr("data-src").substringAfter(uploadUrl),
                )
            }
        } else {
            val counterPageStr = document.select("#comics script").toString()

            val startPageStr = counterPageStr
                .substringAfter("for(var i =")
                .substringBefore("; i <")
                .trim()
            var endPageStr = counterPageStr
                .substringAfter("; i <")
                .substringBefore("; i++)")
                .trim()

            if (endPageStr.contains("=")) {
                endPageStr = (endPageStr.replace("=", "").trim().toInt() + 1).toString()
            }

            if (baseImgUrl.contains("/share.")) {
                baseImgUrl = counterPageStr
                    .substringAfter("data-src=\"")
                    .substringBefore("' + i")
                    .trim().replace("https://feik.domain.ru/", "https://read.webofcomics.ru/webofcomics.ru/www/webofcomics.ru/public_html/") +
                    counterPageStr
                        .substringAfter("i + '")
                        .substringBefore("\">")
                        .trim()
            }

            val countSubPage = counterPageStr.split("document.write").size
            return (startPageStr.toInt() until endPageStr.toInt()).mapIndexed { index, page ->
                val subPage = when (countSubPage) {
                    3 -> {
                        when {
                            page < 10 -> "0"
                            else -> ""
                        }
                    }
                    4 -> {
                        when {
                            page < 10 -> "00"
                            page < 100 -> "0"
                            else -> ""
                        }
                    }
                    else -> ""
                }
                Page(
                    index,
                    "",
                    baseImgUrl.substringBeforeLast("/") + "/$subPage$page." + baseImgUrl.substringAfterLast("."),
                )
            }
        }
    }

    override fun imageUrlParse(document: Document) = ""

    // Filters
    private class CheckFilter(name: String) : Filter.CheckBox(name)
    private data class Publisher(val name: String, val id: String)

    override fun getFilterList() = FilterList(
        Section(),
        OrderBy(),
        Filter.Separator(),
        Filter.Header("ОБЩИЕ ФИЛЬТРЫ♼"),
        Filter.Separator(),
        StatusList(getStatusList()),
        AgeList(getAgeList()),
        Filter.Separator(),
        Filter.Header("КОМИКСЫ♼"),
        Filter.Separator(),
        TypeComics(getTypesComics()),
        GenreComics(getGenresComics()),
        PublishersComics(publishersComics),
        Filter.Separator(),
        Filter.Header("МАНГА♼"),
        Filter.Separator(),
        TypeManga(getTypesManga()),
        GenreManga(getGenresManga()),
        PublishersManga(publishersManga),
    )

    private class OrderBy : Filter.Sort(
        "Сортировать по",
        arrayOf("Дате обновления", "Популярности", "Просмотрам", "Комментариям", "Алфавиту", "Дате выпуска♼", "Рейтингу♼"),
        Selection(1, false),
    )

    private class Section : Filter.Select<String>(
        "ИЛИ",
        arrayOf("Сортировка(без фильтрации♼)", "КОМИКСЫ", "МАНГА"),
    )

    private class StatusList(statuses: List<CheckFilter>) : Filter.Group<CheckFilter>("Статус перевода", statuses)
    private fun getStatusList() = listOf(
        CheckFilter("Продолжается"),
        CheckFilter("Завершён"),
        CheckFilter("Заморожен"),
    )

    private class AgeList(ages: List<CheckFilter>) : Filter.Group<CheckFilter>("Возрастное ограничение", ages)
    private fun getAgeList() = listOf(
        CheckFilter("16+"),
        CheckFilter("18+"),
    )

    // Filters Comics
    private class GenreComics(genres: List<CheckFilter>) : Filter.Group<CheckFilter>("Жанры", genres)
    private class TypeComics(types: List<CheckFilter>) : Filter.Group<CheckFilter>("Тип", types)
    private class PublishersComics(publishers: Array<String>) : Filter.Select<String>("Издательства", publishers)

    private fun getGenresComics() = listOf(
        CheckFilter("Антиутопия"),
        CheckFilter("Биография"),
        CheckFilter("Боевик"),
        CheckFilter("Боевые Искусства"),
        CheckFilter("Вампиры"),
        CheckFilter("Вестерн"),
        CheckFilter("Военный"),
        CheckFilter("Детектив"),
        CheckFilter("Детский"),
        CheckFilter("Драма"),
        CheckFilter("Зомби"),
        CheckFilter("Исторический"),
        CheckFilter("Киберпанк"),
        CheckFilter("Комедия"),
        CheckFilter("Космоопера"),
        CheckFilter("Криминал"),
        CheckFilter("Мелодрама"),
        CheckFilter("Мистика"),
        CheckFilter("Нуар"),
        CheckFilter("Пародия"),
        CheckFilter("Повседневность"),
        CheckFilter("Постапокалиптика"),
        CheckFilter("Приключения"),
        CheckFilter("Путешествия во времени"),
        CheckFilter("Романтика"),
        CheckFilter("Сверхъестественное"),
        CheckFilter("Слэшер"),
        CheckFilter("Стимпанк"),
        CheckFilter("Супергероика"),
        CheckFilter("Триллер"),
        CheckFilter("Ужасы"),
        CheckFilter("Фантасмагория"),
        CheckFilter("Фантастика"),
        CheckFilter("Фэнтези"),
        CheckFilter("Эротика"),
    )

    private fun getTypesComics() = listOf(
        CheckFilter("Ван шот"),
        CheckFilter("Лимитка"),
        CheckFilter("Макси-серия (Онгоинг)"),
        CheckFilter("Графический роман"),
        CheckFilter("Спешл"),
        CheckFilter("Превью"),
        CheckFilter("Ежегодник"),
        CheckFilter("Сборник"),
    )

    private fun getPublishersComics() = listOf(
        Publisher("Все", "Not"),
        Publisher("12 Bis", "221"),
        Publisher("12-Gauge Comics", "59"),
        Publisher("215 INK", "135"),
        Publisher("4Twenty Limited", "236"),
        Publisher("Aardvark", "122"),
        Publisher("Ablaze", "137"),
        Publisher("Abrams ComicArts", "220"),
        Publisher("Abstract Studio", "30"),
        Publisher("Action Lab", "116"),
        Publisher("Aftershock Comics", "95"),
        Publisher("Albin Michel", "43"),
        Publisher("Alias Enterprises", "79"),
        Publisher("Alterna Comics", "114"),
        Publisher("Alterna Comics", "70"),
        Publisher("American Mythology Productions", "101"),
        Publisher("America's Best Comics", "51"),
        Publisher("Ankama", "180"),
        Publisher("Antarctic Press", "46"),
        Publisher("Archaia Studios Press", "45"),
        Publisher("Archie Comics", "39"),
        Publisher("Arh Comix", "139"),
        Publisher("Arrow", "156"),
        Publisher("Atlas Comics", "108"),
        Publisher("Atomeka Press", "84"),
        Publisher("Avatar Press", "22"),
        Publisher("AWA Studios", "162"),
        Publisher("Benitez Productions", "158"),
        Publisher("Best Jackett Press", "237"),
        Publisher("Black Mask Studios", "93"),
        Publisher("Blizzard Entertainment", "100"),
        Publisher("Bluewater Comics", "96"),
        Publisher("Bongo", "78"),
        Publisher("Boom! Studios", "25"),
        Publisher("Boundless Comics", "24"),
        Publisher("Bubble", "94"),
        Publisher("Carlsen Comics", "165"),
        Publisher("Casterman", "54"),
        Publisher("Catalan Communications", "58"),
        Publisher("CD Projekt Red", "163"),
        Publisher("Chaos! Comics", "48"),
        Publisher("Cinebooks", "44"),
        Publisher("Coffin Comics", "103"),
        Publisher("Comico", "73"),
        Publisher("Comics Experience", "160"),
        Publisher("Comics USA", "35"),
        Publisher("Creator Owned Comics", "195"),
        Publisher("Curtis Magazines", "217"),
        Publisher("Dargaud", "41"),
        Publisher("Dark Horse Comics", "18"),
        Publisher("Darkstorm Comics", "26"),
        Publisher("Dayjob Studio", "166"),
        Publisher("DC Comics", "11"),
        Publisher("Dead Dog", "155"),
        Publisher("Delcourt", "71"),
        Publisher("Devil's Due Publishing", "33"),
        Publisher("Devolver Digital", "106"),
        Publisher("Digital Webbing", "143"),
        Publisher("Disney Comics", "109"),
        Publisher("Dreamwave Productions", "66"),
        Publisher("Dupuis", "111"),
        Publisher("Dynamite Entertainment", "19"),
        Publisher("Eclipse Comics", "102"),
        Publisher("Editorial Novaro", "228"),
        Publisher("Egmont Magazines", "38"),
        Publisher("Ehapa Verlag", "151"),
        Publisher("Entity", "129"),
        Publisher("Eternity", "157"),
        Publisher("Europe Comics", "134"),
        Publisher("Evil Twin Comics", "27"),
        Publisher("Exploding Albatross Funnybooks", "64"),
        Publisher("Fantagraphics", "117"),
        Publisher("Fawcett Publications", "34"),
        Publisher("First Comics", "198"),
        Publisher("First Second Books", "90"),
        Publisher("Fleetway", "80"),
        Publisher("Fox Atomic Comics", "119"),
        Publisher("Games Workshop", "133"),
        Publisher("Genesis West", "172"),
        Publisher("GG Studio", "77"),
        Publisher("Ginger Rabbit Studio", "67"),
        Publisher("Glénat", "112"),
        Publisher("H. H. Windsor", "174"),
        Publisher("Harris Comics", "55"),
        Publisher("Heavy Metal", "89"),
        Publisher("Heiro-Graphic", "123"),
        Publisher("Humanoids", "21"),
        Publisher("Icon Comics", "29"),
        Publisher("Id Software", "69"),
        Publisher("IDW Publishing", "16"),
        Publisher("Illusion Studios", "181"),
        Publisher("Image Comics", "15"),
        Publisher("Joe Books", "83"),
        Publisher("Koyama Press", "104"),
        Publisher("Le Lombard", "63"),
        Publisher("Legendary Comics", "144"),
        Publisher("Lion Forge Comics", "85"),
        Publisher("Locust Moon Press", "121"),
        Publisher("Machaon", "107"),
        Publisher("Magic Strip", "190"),
        Publisher("Magnetic Press", "61"),
        Publisher("Malibu", "56"),
        Publisher("Marvel Comics", "13"),
        Publisher("Max", "14"),
        Publisher("Mirage", "40"),
        Publisher("MonkeyBrain Comics", "42"),
        Publisher("Monsterverse", "115"),
        Publisher("Moonstone", "209"),
        Publisher("Mount Olympus Comics", "171"),
        Publisher("Nintendo", "127"),
        Publisher("Northstar", "113"),
        Publisher("Nowa Fantastyka", "141"),
        Publisher("Omnibus Press", "188"),
        Publisher("Oni Press", "28"),
        Publisher("Panelsyndicate", "88"),
        Publisher("Piranha Press", "206"),
        Publisher("Radical Publishing", "86"),
        Publisher("Rebellion", "68"),
        Publisher("Red 5 Comics", "164"),
        Publisher("Revolution Software Ltd", "37"),
        Publisher("Revolver Comics", "72"),
        Publisher("SAF Comics", "170"),
        Publisher("Salleck Publications", "65"),
        Publisher("Scholastic Book Services", "49"),
        Publisher("Sega", "189"),
        Publisher("Self Published", "47"),
        Publisher("Sergio Bonelli Editore", "98"),
        Publisher("Shadowline", "36"),
        Publisher("Sirius Entertainment", "140"),
        Publisher("Skybound", "62"),
        Publisher("Slave Labor Graphics (SLG)", "75"),
        Publisher("Soleil", "17"),
        Publisher("Space Goat Productions", "225"),
        Publisher("Splitter", "87"),
        Publisher("Star Comics", "159"),
        Publisher("Storm King Comics", "175"),
        Publisher("Tell Tale Publications", "167"),
        Publisher("Titan Comics", "60"),
        Publisher("Top Cow", "23"),
        Publisher("Top Shelf", "118"),
        Publisher("Topps", "53"),
        Publisher("Toutain Editor", "203"),
        Publisher("UDON", "169"),
        Publisher("Valiant", "50"),
        Publisher("Vanquish Interactive", "224"),
        Publisher("Vault Comics", "161"),
        Publisher("Vents d'Ouest", "110"),
        Publisher("Vertigo", "20"),
        Publisher("Viper Comics", "154"),
        Publisher("Virgin Comics", "76"),
        Publisher("Vortex", "74"),
        Publisher("Warp Graphics", "31"),
        Publisher("WildStorm", "52"),
        Publisher("Zenescope Entertainment", "32"),
        Publisher("Неизвестное издательство", "57"),
    )

    private val publishersComics = getPublishersComics().map {
        it.name
    }.toTypedArray()

    // Filters Manga
    private class GenreManga(genres: List<CheckFilter>) : Filter.Group<CheckFilter>("Жанры", genres)
    private class TypeManga(types: List<CheckFilter>) : Filter.Group<CheckFilter>("Тип", types)
    private class PublishersManga(publishers: Array<String>) : Filter.Select<String>("Издательства", publishers)

    private fun getGenresManga() = listOf(
        CheckFilter("Арт"),
        CheckFilter("Бара"),
        CheckFilter("Боевик"),
        CheckFilter("Боевые искусства"),
        CheckFilter("Вампиры"),
        CheckFilter("Война"),
        CheckFilter("Гарем"),
        CheckFilter("Гендерная интрига"),
        CheckFilter("Героическое фэнтези"),
        CheckFilter("Гуро"),
        CheckFilter("Детектив"),
        CheckFilter("Дзёсэй"),
        CheckFilter("Додзинси"),
        CheckFilter("Драма"),
        CheckFilter("Ёнкома"),
        CheckFilter("Зомби"),
        CheckFilter("Игра"),
        CheckFilter("Исекай"),
        CheckFilter("История"),
        CheckFilter("Киберпанк"),
        CheckFilter("Кодомо"),
        CheckFilter("Комедия"),
        CheckFilter("Кулинария"),
        CheckFilter("Магия"),
        CheckFilter("Махо-сёдзё"),
        CheckFilter("Меха"),
        CheckFilter("Мистика"),
        CheckFilter("Мужская беременность"),
        CheckFilter("Научная фантастика"),
        CheckFilter("Оборотни"),
        CheckFilter("Образовательная"),
        CheckFilter("Омегаверс"),
        CheckFilter("Повседневность"),
        CheckFilter("Полиция"),
        CheckFilter("Постапокалиптика"),
        CheckFilter("Приключения"),
        CheckFilter("Психология"),
        CheckFilter("Романтика"),
        CheckFilter("Самурайский боевик"),
        CheckFilter("Сборник"),
        CheckFilter("Сверхъестественное"),
        CheckFilter("Сёдзё"),
        CheckFilter("Сёдзё-ай"),
        CheckFilter("Сёнэн"),
        CheckFilter("Сёнэн-ай"),
        CheckFilter("Сказка"),
        CheckFilter("Спорт"),
        CheckFilter("Сэйнэн"),
        CheckFilter("Трагедия"),
        CheckFilter("Триллер"),
        CheckFilter("Ужасы"),
        CheckFilter("Фантастика"),
        CheckFilter("Фэнтези"),
        CheckFilter("Школа"),
        CheckFilter("Экшен"),
        CheckFilter("Элементы юмора"),
        CheckFilter("Эротика"),
        CheckFilter("Этти"),
        CheckFilter("Юри"),
        CheckFilter("Яой"),

    )

    private fun getTypesManga() = listOf(
        CheckFilter("Манга"),
        CheckFilter("Манхва"),
        CheckFilter("Маньхуа"),
        CheckFilter("OEL-Манга"),
        CheckFilter("Руманга"),
        CheckFilter("Индонезийский"),
        CheckFilter("Комикс западный"),
    )

    private fun getPublishersManga() = listOf(
        Publisher("Все", "Not"),
        Publisher("AC.QQ", "128"),
        Publisher("Akita Shoten", "136"),
        Publisher("Alphapolis", "223"),
        Publisher("Asahi Shimbunsha", "204"),
        Publisher("ASCII Media Works", "92"),
        Publisher("Beyond", "201"),
        Publisher("Bilibili", "193"),
        Publisher("Bomtoon", "177"),
        Publisher("BookCube", "186"),
        Publisher("Cherish Media", "227"),
        Publisher("Comico Japan", "210"),
        Publisher("Core Magazine", "194"),
        Publisher("D&C Media", "211"),
        Publisher("Daum", "142"),
        Publisher("DCC", "230"),
        Publisher("eComiX", "202"),
        Publisher("Enterbrain", "150"),
        Publisher("Frontier Works", "214"),
        Publisher("Fujimi Shobo", "146"),
        Publisher("Gentosha", "212"),
        Publisher("Hakusensha", "176"),
        Publisher("Hifumi Shobo", "218"),
        Publisher("iQIYI", "226"),
        Publisher("Kadokawa Shoten", "97"),
        Publisher("Kaiousha", "185"),
        Publisher("Kakao", "138"),
        Publisher("Kidari Studio", "196"),
        Publisher("Kill Time Communication", "148"),
        Publisher("Kodansha", "126"),
        Publisher("KuaiKan Manhua", "147"),
        Publisher("Lezhin", "125"),
        Publisher("Libre", "215"),
        Publisher("Mag Garden", "208"),
        Publisher("Manhuatai", "182"),
        Publisher("Mkzhan", "197"),
        Publisher("MrBlue", "187"),
        Publisher("Naver", "145"),
        Publisher("Nihon Bungeisha", "233"),
        Publisher("PeanuToon", "205"),
        Publisher("Pixiv", "216"),
        Publisher("Printemps Shuppan", "213"),
        Publisher("Ridibooks", "207"),
        Publisher("Screamo", "192"),
        Publisher("Seirin Kogeisha", "168"),
        Publisher("Seoul Media Comics", "191"),
        Publisher("Shinshokan", "235"),
        Publisher("Shogakukan", "124"),
        Publisher("Shonen Gahosha", "173"),
        Publisher("Shueisha", "105"),
        Publisher("Square Enix", "131"),
        Publisher("Suiseisha", "219"),
        Publisher("Takeshobo", "199"),
        Publisher("Tencent", "200"),
        Publisher("Tokuma Shoten", "222"),
        Publisher("Tokyopop", "99"),
        Publisher("Toptoon", "184"),
        Publisher("U17", "130"),
        Publisher("Webtoon", "183"),
        Publisher("Неизвестное издательство", "149"),
    )

    private val publishersManga = getPublishersManga().map {
        it.name
    }.toTypedArray()

    companion object {
        private val simpleDateFormat by lazy { SimpleDateFormat("dd.MM.yyyy", Locale.US) }
    }
}
