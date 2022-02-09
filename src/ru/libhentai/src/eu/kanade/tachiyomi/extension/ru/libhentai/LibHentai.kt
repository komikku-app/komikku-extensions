package eu.kanade.tachiyomi.extension.ru.libhentai

import android.app.Application
import android.content.SharedPreferences
import android.widget.Toast
import androidx.preference.ListPreference
import androidx.preference.PreferenceScreen
import eu.kanade.tachiyomi.lib.ratelimit.RateLimitInterceptor
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.asObservableSuccess
import eu.kanade.tachiyomi.source.ConfigurableSource
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.util.asJsoup
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Element
import rx.Observable
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class LibHentai : ConfigurableSource, HttpSource() {

    private val json: Json by injectLazy()

    private val preferences: SharedPreferences by lazy {
        Injekt.get<Application>().getSharedPreferences("source_${id}_2", 0x0000)
    }

    override val name: String = "Hentailib"

    override val lang = "ru"

    override val supportsLatest = true

    override val client: OkHttpClient = network.cloudflareClient.newBuilder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addNetworkInterceptor(RateLimitInterceptor(3))
        .build()

    override val baseUrl = "https://hentailib.me"

    override fun headersBuilder() = Headers.Builder().apply {
        add("Accept", "image/webp,*/*;q=0.8")
        add("Referer", baseUrl)
    }

    override fun latestUpdatesRequest(page: Int) = GET(baseUrl, headers)

    private val latestUpdatesSelector = "div.updates__item"

    override fun latestUpdatesParse(response: Response): MangasPage {
        val elements = response.asJsoup().select(latestUpdatesSelector)
        val latestMangas = elements?.map { latestUpdatesFromElement(it) }
        if (latestMangas != null)
            return MangasPage(latestMangas, false) // TODO: use API
        return MangasPage(emptyList(), false)
    }

    private fun latestUpdatesFromElement(element: Element): SManga {
        val manga = SManga.create()
        element.select("div.cover").first().let { img ->
            manga.thumbnail_url = img.attr("data-src").replace("_thumb", "_250x350")
        }

        element.select("a").first().let { link ->
            manga.setUrlWithoutDomain(link.attr("href"))
            manga.title = if (isEng.equals("rus") || element.select(".updates__name_rus").isNullOrEmpty()) { element.select("h4").first().text() } else element.select(".updates__name_rus").first().text()
        }
        return manga
    }

    private var csrfToken: String = ""

    private fun catalogHeaders() = Headers.Builder()
        .apply {
            add("Accept", "application/json, text/plain, */*")
            add("X-Requested-With", "XMLHttpRequest")
            add("x-csrf-token", csrfToken)
        }
        .build()

    override fun popularMangaRequest(page: Int) = GET("$baseUrl/login", headers)

    override fun fetchPopularManga(page: Int): Observable<MangasPage> {
        if (csrfToken.isEmpty()) {
            return client.newCall(popularMangaRequest(page))
                .asObservableSuccess()
                .flatMap { response ->
                    // Obtain token
                    val resBody = response.body!!.string()
                    csrfToken = "_token\" content=\"(.*)\"".toRegex().find(resBody)!!.groups[1]!!.value
                    return@flatMap fetchPopularMangaFromApi(page)
                }
        }
        return fetchPopularMangaFromApi(page)
    }

    private fun fetchPopularMangaFromApi(page: Int): Observable<MangasPage> {
        return client.newCall(POST("$baseUrl/filterlist?dir=desc&sort=views&page=$page", catalogHeaders()))
            .asObservableSuccess()
            .map { response ->
                popularMangaParse(response)
            }
    }

    override fun popularMangaParse(response: Response): MangasPage {
        val resBody = response.body!!.string()
        val result = json.decodeFromString<JsonObject>(resBody)
        val items = result["items"]!!.jsonObject
        val popularMangas = items["data"]?.jsonArray?.map { popularMangaFromElement(it) }

        if (popularMangas != null) {
            val hasNextPage = items["next_page_url"]?.jsonPrimitive?.contentOrNull != null
            return MangasPage(popularMangas, hasNextPage)
        }
        return MangasPage(emptyList(), false)
    }

    private fun popularMangaFromElement(el: JsonElement) = SManga.create().apply {
        val slug = el.jsonObject["slug"]!!.jsonPrimitive.content
        val cover = el.jsonObject["cover"]!!.jsonPrimitive.content
        title = if (isEng.equals("rus")) el.jsonObject["rus_name"]!!.jsonPrimitive.content else el.jsonObject["name"]!!.jsonPrimitive.content
        thumbnail_url = "$COVER_URL/huploads/cover/$slug/cover/${cover}_250x350.jpg"
        url = "/$slug"
    }

    override fun mangaDetailsParse(response: Response): SManga {
        val document = response.asJsoup()

        if (document.select("body[data-page=home]").isNotEmpty())
            throw Exception("Can't open manga. Try log in via WebView")

        val manga = SManga.create()

        val body = document.select("div.media-info-list").first()
        val rawCategory = body.select("div.media-info-list__title:contains(Тип) + div").text()
        val category = when {
            rawCategory == "Комикс западный" -> "Комикс"
            rawCategory.isNotBlank() -> rawCategory
            else -> "Манга"
        }
        var rawAgeStop = body.select("div.media-info-list__title:contains(Возрастной рейтинг) + div").text()
        if (rawAgeStop.isEmpty()) {
            rawAgeStop = "0+"
        }

        val ratingValue = document.select(".media-rating.media-rating_lg div.media-rating__value").text().toFloat() * 2
        val ratingVotes = document.select(".media-rating.media-rating_lg div.media-rating__votes").text()
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
        val genres = document.select(".media-tags > a").map { it.text().capitalize() }
        manga.title = if (isEng.equals("rus")) document.select(".media-name__main").text() else document.select(".media-name__alt").text()
        manga.thumbnail_url = document.select(".media-sidebar__cover > img").attr("src")
        manga.author = body.select("div.media-info-list__title:contains(Автор) + div").text()
        manga.artist = body.select("div.media-info-list__title:contains(Художник) + div").text()
        manga.status = if (document.html().contains("Манга удалена по просьбе правообладателей") ||
            document.html().contains("Данный тайтл лицензирован на территории РФ.")
        ) {
            SManga.LICENSED
        } else
            when (
                body.select("div.media-info-list__title:contains(Статус перевода) + div")
                    .text()
                    .toLowerCase(Locale.ROOT)
            ) {
                "продолжается" -> SManga.ONGOING
                "завершен" -> SManga.COMPLETED
                else -> SManga.UNKNOWN
            }
        manga.genre = category + ", " + rawAgeStop + ", " + genres.joinToString { it.trim() }
        val altSelector = document.select(".media-info-list__item_alt-names .media-info-list__value div")
        var altName = ""
        if (altSelector.isNotEmpty()) {
            altName = "Альтернативные названия:\n" + altSelector.map { it.text() }.joinToString(" / ") + "\n\n"
        }
        val mediaNameLanguage = if (isEng.equals("rus")) document.select(".media-name__alt").text() else document.select(".media-name__main").text()
        manga.description = mediaNameLanguage + "\n" + ratingStar + " " + ratingValue + " (голосов: " + ratingVotes + ")\n" + altName + document.select(".media-description__text").text()
        return manga
    }

    override fun chapterListParse(response: Response): List<SChapter> {
        val document = response.asJsoup()
        if (document.html().contains("Манга удалена по просьбе правообладателей") ||
            document.html().contains("Данный тайтл лицензирован на территории РФ.")
        ) {
            return emptyList()
        }
        val dataStr = document
            .toString()
            .substringAfter("window.__DATA__ = ")
            .substringBefore("window._SITE_COLOR_")
            .substringBeforeLast(";")

        val data = json.decodeFromString<JsonObject>(dataStr)
        val chaptersList = data["chapters"]!!.jsonObject["list"]?.jsonArray
        val slug = data["manga"]!!.jsonObject["slug"]!!.jsonPrimitive.content
        val branches = data["chapters"]!!.jsonObject["branches"]!!.jsonArray.reversed()
        val sortingList = preferences.getString(SORTING_PREF, "ms_mixing")

        val chapters: List<SChapter>? = if (branches.isNotEmpty() && !sortingList.equals("ms_mixing")) {
            sortChaptersByTranslator(sortingList, chaptersList, slug, branches)
        } else {
            chaptersList
                ?.filter { it.jsonObject["status"]?.jsonPrimitive?.intOrNull != 2 }
                ?.map { chapterFromElement(it, sortingList, slug) }
        }

        return chapters ?: emptyList()
    }

    private fun sortChaptersByTranslator
    (sortingList: String?, chaptersList: JsonArray?, slug: String, branches: List<JsonElement>): List<SChapter>? {
        var chapters: List<SChapter>? = null
        when (sortingList) {
            "ms_combining" -> {
                val tempChaptersList = mutableListOf<SChapter>()
                for (currentBranch in branches.withIndex()) {
                    val teamId = branches[currentBranch.index].jsonObject["id"]!!.jsonPrimitive.int
                    chapters = chaptersList
                        ?.filter { it.jsonObject["branch_id"]?.jsonPrimitive?.intOrNull == teamId && it.jsonObject["status"]?.jsonPrimitive?.intOrNull != 2 }
                        ?.map { chapterFromElement(it, sortingList, slug, teamId, branches) }
                    chapters?.let { tempChaptersList.addAll(it) }
                }
                chapters = tempChaptersList
            }
            "ms_largest" -> {
                val sizesChaptersLists = mutableListOf<Int>()
                for (currentBranch in branches.withIndex()) {
                    val teamId = branches[currentBranch.index].jsonObject["id"]!!.jsonPrimitive.int
                    val chapterSize = chaptersList
                        ?.filter { it.jsonObject["branch_id"]?.jsonPrimitive?.intOrNull == teamId }!!.size
                    sizesChaptersLists.add(chapterSize)
                }
                val max = sizesChaptersLists.indexOfFirst { it == sizesChaptersLists.maxOrNull() ?: 0 }
                val teamId = branches[max].jsonObject["id"]!!.jsonPrimitive.int

                chapters = chaptersList
                    ?.filter { it.jsonObject["branch_id"]?.jsonPrimitive?.intOrNull == teamId && it.jsonObject["status"]?.jsonPrimitive?.intOrNull != 2 }
                    ?.map { chapterFromElement(it, sortingList, slug, teamId, branches) }
            }
            "ms_active" -> {
                for (currentBranch in branches.withIndex()) {
                    val teams = branches[currentBranch.index].jsonObject["teams"]!!.jsonArray
                    for (currentTeam in teams.withIndex()) {
                        if (teams[currentTeam.index].jsonObject["is_active"]!!.jsonPrimitive.int == 1) {
                            val teamId = branches[currentBranch.index].jsonObject["id"]!!.jsonPrimitive.int
                            chapters = chaptersList
                                ?.filter { it.jsonObject["branch_id"]?.jsonPrimitive?.intOrNull == teamId && it.jsonObject["status"]?.jsonPrimitive?.intOrNull != 2 }
                                ?.map { chapterFromElement(it, sortingList, slug, teamId, branches) }
                            break
                        }
                    }
                }
                chapters ?: throw Exception("Активный перевод не назначен на сайте")
            }
        }

        return chapters
    }

    private fun chapterFromElement
    (chapterItem: JsonElement, sortingList: String?, slug: String, teamIdParam: Int? = null, branches: List<JsonElement>? = null): SChapter {
        val chapter = SChapter.create()

        val volume = chapterItem.jsonObject["chapter_volume"]!!.jsonPrimitive.int
        val number = chapterItem.jsonObject["chapter_number"]!!.jsonPrimitive.content
        val teamId = if (teamIdParam != null) "?bid=$teamIdParam" else ""

        val url = "$baseUrl/$slug/v$volume/c$number$teamId"

        chapter.setUrlWithoutDomain(url)

        val nameChapter = chapterItem.jsonObject["chapter_name"]?.jsonPrimitive?.contentOrNull
        val fullNameChapter = "Том $volume. Глава $number"

        if (!sortingList.equals("ms_mixing")) {
            chapter.scanlator = branches?.let { getScanlatorTeamName(it, chapterItem) } ?: chapterItem.jsonObject["username"]!!.jsonPrimitive.content
        }
        chapter.name = if (nameChapter.isNullOrBlank()) fullNameChapter else "$fullNameChapter - $nameChapter"
        chapter.date_upload = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            .parse(chapterItem.jsonObject["chapter_created_at"]!!.jsonPrimitive.content.substringBefore(" "))?.time ?: 0L

        return chapter
    }

    private fun getScanlatorTeamName(branches: List<JsonElement>, chapterItem: JsonElement): String? {
        var scanlatorData: String? = null
        for (currentBranch in branches.withIndex()) {
            val branch = branches[currentBranch.index].jsonObject
            val teams = branch["teams"]!!.jsonArray
            if (chapterItem.jsonObject["branch_id"]!!.jsonPrimitive.int == branch["id"]!!.jsonPrimitive.int) {
                for (currentTeam in teams.withIndex()) {
                    val team = teams[currentTeam.index].jsonObject
                    val scanlatorId = chapterItem.jsonObject["chapter_scanlator_id"]!!.jsonPrimitive.int
                    scanlatorData = if ((scanlatorId == team.jsonObject["id"]!!.jsonPrimitive.int) ||
                        (scanlatorId == 0 && team["is_active"]!!.jsonPrimitive.int == 1)
                    ) team["name"]!!.jsonPrimitive.content else branch["teams"]!!.jsonArray[0].jsonObject["name"]!!.jsonPrimitive.content
                }
            }
        }
        return scanlatorData
    }

    override fun prepareNewChapter(chapter: SChapter, manga: SManga) {
        """Глава\s(\d+)""".toRegex().find(chapter.name)?.let {
            val number = it.groups[1]?.value!!
            chapter.chapter_number = number.toFloat()
        }
    }

    override fun pageListParse(response: Response): List<Page> {
        val document = response.asJsoup()

        val redirect = document.html()
        if (!redirect.contains("window.__info")) {
            if (redirect.contains("hold-transition login-page")) {
                throw Exception("Для просмотра 18+ контента необходима авторизация через WebView")
            } else if (redirect.contains("header__logo")) {
                throw Exception("Лицензировано - Главы не доступны")
            }
        }

        val chapInfo = document
            .select("script:containsData(window.__info)")
            .first()
            .html()
            .split("window.__info = ")
            .last()
            .trim()
            .split(";")
            .first()

        val chapInfoJson = json.decodeFromString<JsonObject>(chapInfo)
        val servers = chapInfoJson["servers"]!!.jsonObject.toMap()
        val defaultServer: String = chapInfoJson["img"]!!.jsonObject["server"]!!.jsonPrimitive.content
        val autoServer = setOf("secondary", "fourth", defaultServer, "compress")
        val imgUrl: String = chapInfoJson["img"]!!.jsonObject["url"]!!.jsonPrimitive.content

        val serverToUse = when (this.server) {
            null -> autoServer
            "auto" -> autoServer
            else -> listOf(this.server)
        }

        // Get pages
        val pagesArr = document
            .select("script:containsData(window.__pg)")
            .first()
            .html()
            .trim()
            .removePrefix("window.__pg = ")
            .removeSuffix(";")

        val pagesJson = json.decodeFromString<JsonArray>(pagesArr)
        val pages = mutableListOf<Page>()

        pagesJson.forEach { page ->
            val keys = servers.keys.filter { serverToUse.indexOf(it) >= 0 }.sortedBy { serverToUse.indexOf(it) }
            val serversUrls = keys.map {
                servers[it]?.jsonPrimitive?.contentOrNull + imgUrl + page.jsonObject["u"]!!.jsonPrimitive.content
            }.joinToString(separator = ",,") { it }
            pages.add(Page(page.jsonObject["p"]!!.jsonPrimitive.int, serversUrls))
        }

        return pages
    }

    private fun checkImage(url: String): Boolean {
        val response = client.newCall(Request.Builder().url(url).head().headers(headers).build()).execute()
        return response.isSuccessful && (response.header("content-length", "0")?.toInt()!! > 320)
    }

    override fun fetchImageUrl(page: Page): Observable<String> {
        if (page.imageUrl != null) {
            return Observable.just(page.imageUrl)
        }

        val urls = page.url.split(",,")
        if (urls.size == 1) {
            return Observable.just(urls[0])
        }

        return Observable.from(urls).filter { checkImage(it) }.first()
    }

    override fun imageUrlParse(response: Response): String = ""

    private fun searchMangaByIdRequest(id: String): Request {
        return GET("$baseUrl/$id", headers)
    }

    override fun fetchSearchManga(page: Int, query: String, filters: FilterList): Observable<MangasPage> {
        return if (query.startsWith(PREFIX_SLUG_SEARCH)) {
            val realQuery = query.removePrefix(PREFIX_SLUG_SEARCH)
            client.newCall(searchMangaByIdRequest(realQuery))
                .asObservableSuccess()
                .map { response ->
                    val details = mangaDetailsParse(response)
                    details.url = "/$realQuery"
                    MangasPage(listOf(details), false)
                }
        } else {
            client.newCall(searchMangaRequest(page, query, filters))
                .asObservableSuccess()
                .map { response ->
                    searchMangaParse(response)
                }
        }
    }

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        if (csrfToken.isEmpty()) {
            val tokenResponse = client.newCall(popularMangaRequest(page)).execute()
            val resBody = tokenResponse.body!!.string()
            csrfToken = "_token\" content=\"(.*)\"".toRegex().find(resBody)!!.groups[1]!!.value
        }
        val url = "$baseUrl/filterlist?page=$page".toHttpUrlOrNull()!!.newBuilder()
        if (query.isNotEmpty()) {
            url.addQueryParameter("name", query)
        }
        (if (filters.isEmpty()) getFilterList() else filters).forEach { filter ->
            when (filter) {
                is CategoryList -> filter.state.forEach { category ->
                    if (category.state) {
                        url.addQueryParameter("types[]", category.id)
                    }
                }
                is FormatList -> filter.state.forEach { forma ->
                    if (forma.state != Filter.TriState.STATE_IGNORE) {
                        url.addQueryParameter(if (forma.isIncluded()) "format[include][]" else "format[exclude][]", forma.id)
                    }
                }
                is StatusList -> filter.state.forEach { status ->
                    if (status.state) {
                        url.addQueryParameter("status[]", status.id)
                    }
                }
                is StatusTitleList -> filter.state.forEach { title ->
                    if (title.state) {
                        url.addQueryParameter("manga_status[]", title.id)
                    }
                }
                is GenreList -> filter.state.forEach { genre ->
                    if (genre.state != Filter.TriState.STATE_IGNORE) {
                        url.addQueryParameter(if (genre.isIncluded()) "genres[include][]" else "genres[exclude][]", genre.id)
                    }
                }
                is OrderBy -> {
                    url.addQueryParameter("dir", if (filter.state!!.ascending) "asc" else "desc")
                    url.addQueryParameter("sort", arrayOf("rate", "name", "views", "created_at", "last_chapter_at", "chap_count")[filter.state!!.index])
                }
                is TagList -> filter.state.forEach { tag ->
                    if (tag.state != Filter.TriState.STATE_IGNORE) {
                        url.addQueryParameter(if (tag.isIncluded()) "tags[include][]" else "tags[exclude][]", tag.id)
                    }
                }
                is MyList -> filter.state.forEach { favorite ->
                    if (favorite.state != Filter.TriState.STATE_IGNORE) {
                        url.addQueryParameter(if (favorite.isIncluded()) "bookmarks[include][]" else "bookmarks[exclude][]", favorite.id)
                    }
                }
            }
        }
        return POST(url.toString(), catalogHeaders())
    }

    // Hack search method to add some results from search popup
    override fun searchMangaParse(response: Response): MangasPage {
        val searchRequest = response.request.url.queryParameter("name")
        val mangas = mutableListOf<SManga>()

        if (!searchRequest.isNullOrEmpty()) {
            val popupSearchHeaders = headers
                .newBuilder()
                .add("Accept", "application/json, text/plain, */*")
                .add("X-Requested-With", "XMLHttpRequest")
                .build()

            // +200ms
            val popup = client.newCall(
                GET("$baseUrl/search?query=$searchRequest", popupSearchHeaders)
            )
                .execute().body!!.string()

            val jsonList = json.decodeFromString<JsonArray>(popup)
            jsonList.forEach {
                mangas.add(popularMangaFromElement(it))
            }
        }
        val searchedMangas = popularMangaParse(response)

        // Filtered out what find in popup search
        mangas.addAll(
            searchedMangas.mangas.filter { search ->
                mangas.find { search.title == it.title } == null
            }
        )

        return MangasPage(mangas, searchedMangas.hasNextPage)
    }

    private class SearchFilter(name: String, val id: String) : Filter.TriState(name)
    private class CheckFilter(name: String, val id: String) : Filter.CheckBox(name)

    private class CategoryList(categories: List<CheckFilter>) : Filter.Group<CheckFilter>("Тип", categories)
    private class FormatList(formas: List<SearchFilter>) : Filter.Group<SearchFilter>("Формат выпуска", formas)
    private class StatusList(statuses: List<CheckFilter>) : Filter.Group<CheckFilter>("Статус перевода", statuses)
    private class StatusTitleList(titles: List<CheckFilter>) : Filter.Group<CheckFilter>("Статус тайтла", titles)
    private class GenreList(genres: List<SearchFilter>) : Filter.Group<SearchFilter>("Жанры", genres)
    private class TagList(tags: List<SearchFilter>) : Filter.Group<SearchFilter>("Теги", tags)
    private class MyList(favorites: List<SearchFilter>) : Filter.Group<SearchFilter>("Мои списки", favorites)

    override fun getFilterList() = FilterList(
        OrderBy(),
        CategoryList(getCategoryList()),
        FormatList(getFormatList()),
        GenreList(getGenreList()),
        TagList(getTagList()),
        StatusList(getStatusList()),
        StatusTitleList(getStatusTitleList()),
        MyList(getMyList())
    )

    private class OrderBy : Filter.Sort(
        "Сортировка",
        arrayOf("Рейтинг", "Имя", "Просмотры", "Дате добавления", "Дате обновления", "Кол-во глав"),
        Selection(0, false)
    )

    /*
    * Use console
    * Object.entries(__FILTER_ITEMS__.types).map(([k, v]) => `SearchFilter("${v.label}", "${v.id}")`).join(',\n')
    * on /manga-list
    */
    private fun getCategoryList() = listOf(
        CheckFilter("Манга", "1"),
        CheckFilter("OEL-манга", "4"),
        CheckFilter("Манхва", "5"),
        CheckFilter("Маньхуа", "6"),
        CheckFilter("Руманга", "8"),
        CheckFilter("Комикс западный", "9")
    )

    private fun getFormatList() = listOf(
        SearchFilter("4-кома (Ёнкома)", "1"),
        SearchFilter("Сборник", "2"),
        SearchFilter("Додзинси", "3"),
        SearchFilter("Сингл", "4"),
        SearchFilter("В цвете", "5"),
        SearchFilter("Веб", "6")
    )

    /*
    * Use console
    * Object.entries(__FILTER_ITEMS__.status).map(([k, v]) => `SearchFilter("${v.label}", "${v.id}")`).join(',\n')
    * on /manga-list
    */
    private fun getStatusList() = listOf(
        CheckFilter("Продолжается", "1"),
        CheckFilter("Завершен", "2"),
        CheckFilter("Заморожен", "3"),
        CheckFilter("Заброшен", "4")
    )

    private fun getStatusTitleList() = listOf(
        CheckFilter("Онгоинг", "1"),
        CheckFilter("Завершён", "2"),
        CheckFilter("Анонс", "3"),
        CheckFilter("Приостановлен", "4"),
        CheckFilter("Выпуск прекращён", "5"),
    )

    /*
    * Use console
    * __FILTER_ITEMS__.genres.map(it => `SearchFilter("${it.name}", "${it.id}")`).join(',\n')
    * on /manga-list
    */
    private fun getGenreList() = listOf(
        SearchFilter("арт", "32"),
        SearchFilter("боевик", "34"),
        SearchFilter("боевые искусства", "35"),
        SearchFilter("вампиры", "36"),
        SearchFilter("гарем", "37"),
        SearchFilter("гендерная интрига", "38"),
        SearchFilter("героическое фэнтези", "39"),
        SearchFilter("детектив", "40"),
        SearchFilter("дзёсэй", "41"),
        SearchFilter("драма", "43"),
        SearchFilter("игра", "44"),
        SearchFilter("исекай", "79"),
        SearchFilter("история", "45"),
        SearchFilter("киберпанк", "46"),
        SearchFilter("кодомо", "76"),
        SearchFilter("комедия", "47"),
        SearchFilter("махо-сёдзё", "48"),
        SearchFilter("меха", "49"),
        SearchFilter("мистика", "50"),
        SearchFilter("научная фантастика", "51"),
        SearchFilter("омегаверс", "77"),
        SearchFilter("повседневность", "52"),
        SearchFilter("постапокалиптика", "53"),
        SearchFilter("приключения", "54"),
        SearchFilter("психология", "55"),
        SearchFilter("романтика", "56"),
        SearchFilter("самурайский боевик", "57"),
        SearchFilter("сверхъестественное", "58"),
        SearchFilter("сёдзё", "59"),
        SearchFilter("сёдзё-ай", "60"),
        SearchFilter("сёнэн", "61"),
        SearchFilter("сёнэн-ай", "62"),
        SearchFilter("спорт", "63"),
        SearchFilter("сэйнэн", "64"),
        SearchFilter("трагедия", "65"),
        SearchFilter("триллер", "66"),
        SearchFilter("ужасы", "67"),
        SearchFilter("фантастика", "68"),
        SearchFilter("фэнтези", "69"),
        SearchFilter("школа", "70"),
        SearchFilter("эротика", "71"),
        SearchFilter("этти", "72"),
        SearchFilter("юри", "73"),
        SearchFilter("яой", "74")
    )

    private fun getTagList() = listOf(
        SearchFilter("3D", "1"),
        SearchFilter("Defloration", "287"),
        SearchFilter("FPP(Вид от первого лица)", "289"),
        SearchFilter("Footfuck", "5"),
        SearchFilter("Handjob", "6"),
        SearchFilter("Lactation", "7"),
        SearchFilter("Living clothes", "284"),
        SearchFilter("Mind break", "9"),
        SearchFilter("Scat", "13"),
        SearchFilter("Selfcest", "286"),
        SearchFilter("Shemale", "220"),
        SearchFilter("Tomboy", "14"),
        SearchFilter("Unbirth", "283"),
        SearchFilter("X-Ray", "15"),
        SearchFilter("Алкоголь", "16"),
        SearchFilter("Анал", "17"),
        SearchFilter("Андроид", "18"),
        SearchFilter("Анилингус", "19"),
        SearchFilter("Анимация (GIF)", "350"),
        SearchFilter("Арт", "20"),
        SearchFilter("Ахэгао", "2"),
        SearchFilter("БДСМ", "22"),
        SearchFilter("Бакуню", "21"),
        SearchFilter("Бара", "293"),
        SearchFilter("Без проникновения", "336"),
        SearchFilter("Без текста", "23"),
        SearchFilter("Без трусиков", "24"),
        SearchFilter("Без цензуры", "25"),
        SearchFilter("Беременность", "26"),
        SearchFilter("Бикини", "27"),
        SearchFilter("Близнецы", "28"),
        SearchFilter("Боди-арт", "29"),
        SearchFilter("Больница", "30"),
        SearchFilter("Большая грудь", "31"),
        SearchFilter("Большая попка", "32"),
        SearchFilter("Борьба", "33"),
        SearchFilter("Буккакэ", "34"),
        SearchFilter("В бассейне", "35"),
        SearchFilter("В ванной", "36"),
        SearchFilter("В государственном учреждении", "37"),
        SearchFilter("В общественном месте", "38"),
        SearchFilter("В очках", "8"),
        SearchFilter("В первый раз", "39"),
        SearchFilter("В транспорте", "40"),
        SearchFilter("Вампиры", "41"),
        SearchFilter("Вибратор", "42"),
        SearchFilter("Втроём", "43"),
        SearchFilter("Гипноз", "44"),
        SearchFilter("Глубокий минет", "45"),
        SearchFilter("Горячий источник", "46"),
        SearchFilter("Групповой секс", "47"),
        SearchFilter("Гуро", "307"),
        SearchFilter("Гяру и Гангуро", "48"),
        SearchFilter("Двойное проникновение", "49"),
        SearchFilter("Девочки-волшебницы", "50"),
        SearchFilter("Девушка-туалет", "51"),
        SearchFilter("Демон", "52"),
        SearchFilter("Дилдо", "53"),
        SearchFilter("Домохозяйка", "54"),
        SearchFilter("Дыра в стене", "55"),
        SearchFilter("Жестокость", "56"),
        SearchFilter("Золотой дождь", "57"),
        SearchFilter("Зомби", "58"),
        SearchFilter("Зоофилия", "351"),
        SearchFilter("Зрелые женщины", "59"),
        SearchFilter("Избиение", "223"),
        SearchFilter("Измена", "60"),
        SearchFilter("Изнасилование", "61"),
        SearchFilter("Инопланетяне", "62"),
        SearchFilter("Инцест", "63"),
        SearchFilter("Исполнение желаний", "64"),
        SearchFilter("Историческое", "65"),
        SearchFilter("Камера", "66"),
        SearchFilter("Кляп", "288"),
        SearchFilter("Колготки", "67"),
        SearchFilter("Косплей", "68"),
        SearchFilter("Кримпай", "3"),
        SearchFilter("Куннилингус", "69"),
        SearchFilter("Купальники", "70"),
        SearchFilter("ЛГБТ", "343"),
        SearchFilter("Латекс и кожа", "71"),
        SearchFilter("Магия", "72"),
        SearchFilter("Маленькая грудь", "73"),
        SearchFilter("Мастурбация", "74"),
        SearchFilter("Медсестра", "221"),
        SearchFilter("Мейдочка", "75"),
        SearchFilter("Мерзкий дядька", "76"),
        SearchFilter("Милф", "77"),
        SearchFilter("Много девушек", "78"),
        SearchFilter("Много спермы", "79"),
        SearchFilter("Молоко", "80"),
        SearchFilter("Монашка", "353"),
        SearchFilter("Монстродевушки", "81"),
        SearchFilter("Монстры", "82"),
        SearchFilter("Мочеиспускание", "83"),
        SearchFilter("На природе", "84"),
        SearchFilter("Наблюдение", "85"),
        SearchFilter("Насекомые", "285"),
        SearchFilter("Небритая киска", "86"),
        SearchFilter("Небритые подмышки", "87"),
        SearchFilter("Нетораре", "88"),
        SearchFilter("Нэтори", "11"),
        SearchFilter("Обмен телами", "89"),
        SearchFilter("Обычный секс", "90"),
        SearchFilter("Огромная грудь", "91"),
        SearchFilter("Огромный член", "92"),
        SearchFilter("Омораси", "93"),
        SearchFilter("Оральный секс", "94"),
        SearchFilter("Орки", "95"),
        SearchFilter("Остановка времени", "296"),
        SearchFilter("Пайзури", "96"),
        SearchFilter("Парень пассив", "97"),
        SearchFilter("Переодевание", "98"),
        SearchFilter("Пирсинг", "308"),
        SearchFilter("Пляж", "99"),
        SearchFilter("Повседневность", "100"),
        SearchFilter("Подвязки", "282"),
        SearchFilter("Подглядывание", "101"),
        SearchFilter("Подчинение", "102"),
        SearchFilter("Похищение", "103"),
        SearchFilter("Превозмогание", "104"),
        SearchFilter("Принуждение", "105"),
        SearchFilter("Прозрачная одежда", "106"),
        SearchFilter("Проституция", "107"),
        SearchFilter("Психические отклонения", "108"),
        SearchFilter("Публично", "109"),
        SearchFilter("Пытки", "224"),
        SearchFilter("Пьяные", "110"),
        SearchFilter("Рабы", "356"),
        SearchFilter("Рабыни", "111"),
        SearchFilter("С Сюжетом", "337"),
        SearchFilter("Сuminside", "4"),
        SearchFilter("Секс-игрушки", "112"),
        SearchFilter("Сексуально возбуждённая", "113"),
        SearchFilter("Сибари", "114"),
        SearchFilter("Спортивная форма", "117"),
        SearchFilter("Спортивное тело", "335"),
        SearchFilter("Спящие", "118"),
        SearchFilter("Страпон", "119"),
        SearchFilter("Суккуб", "120"),
        SearchFilter("Темнокожие", "121"),
        SearchFilter("Тентакли", "122"),
        SearchFilter("Толстушки", "123"),
        SearchFilter("Трагедия", "124"),
        SearchFilter("Трап", "125"),
        SearchFilter("Ужасы", "126"),
        SearchFilter("Униформа", "127"),
        SearchFilter("Учитель и ученик", "352"),
        SearchFilter("Ушастые", "128"),
        SearchFilter("Фантазии", "129"),
        SearchFilter("Фемдом", "130"),
        SearchFilter("Фестиваль", "131"),
        SearchFilter("Фетиш", "132"),
        SearchFilter("Фистинг", "133"),
        SearchFilter("Фурри", "134"),
        SearchFilter("Футанари", "136"),
        SearchFilter("Футанари имеет парня", "137"),
        SearchFilter("Цельный купальник", "138"),
        SearchFilter("Цундэрэ", "139"),
        SearchFilter("Чикан", "140"),
        SearchFilter("Чулки", "141"),
        SearchFilter("Шлюха", "142"),
        SearchFilter("Эксгибиционизм", "143"),
        SearchFilter("Эльф", "144"),
        SearchFilter("Юные", "145"),
        SearchFilter("Яндэрэ", "146")
    )

    private fun getMyList() = listOf(
        SearchFilter("Читаю", "1"),
        SearchFilter("В планах", "2"),
        SearchFilter("Брошено", "3"),
        SearchFilter("Прочитано", "4"),
        SearchFilter("Любимые", "5")
    )
    companion object {
        const val PREFIX_SLUG_SEARCH = "slug:"
        private const val SERVER_PREF = "MangaLibImageServer"
        private const val SERVER_PREF_Title = "Сервер изображений"

        private const val SORTING_PREF = "MangaLibSorting"
        private const val SORTING_PREF_Title = "Способ выбора переводчиков"

        private const val LANGUAGE_PREF = "MangaLibTitleLanguage"
        private const val LANGUAGE_PREF_Title = "Выбор языка на обложке"

        private const val COVER_URL = "https://staticlib.me"
    }

    private var server: String? = preferences.getString(SERVER_PREF, null)
    private var isEng: String? = preferences.getString(LANGUAGE_PREF, "eng")
    override fun setupPreferenceScreen(screen: PreferenceScreen) {
        val serverPref = ListPreference(screen.context).apply {
            key = SERVER_PREF
            title = SERVER_PREF_Title
            entries = arrayOf("Основной", "Второй (тестовый)", "Третий (эконом трафика)", "Авто")
            entryValues = arrayOf("secondary", "fourth", "compress", "auto")
            summary = "%s"
            setDefaultValue("auto")
            setOnPreferenceChangeListener { _, newValue ->
                server = newValue.toString()
                true
            }
        }

        val sortingPref = ListPreference(screen.context).apply {
            key = SORTING_PREF
            title = SORTING_PREF_Title
            entries = arrayOf(
                "Полный список (без повторных переводов)", "Все переводы (друг за другом)",
                "Наибольшее число глав", "Активный перевод"
            )
            entryValues = arrayOf("ms_mixing", "ms_combining", "ms_largest", "ms_active")
            summary = "%s"
            setDefaultValue("ms_mixing")
            setOnPreferenceChangeListener { _, newValue ->
                val selected = newValue as String
                preferences.edit().putString(SORTING_PREF, selected).commit()
            }
        }
        val titleLanguagePref = ListPreference(screen.context).apply {
            key = LANGUAGE_PREF
            title = LANGUAGE_PREF_Title
            entries = arrayOf("Английский (транскрипция)", "Русский")
            entryValues = arrayOf("eng", "rus")
            summary = "%s"
            setDefaultValue("eng")
            setOnPreferenceChangeListener { _, newValue ->
                val titleLanguage = preferences.edit().putString(LANGUAGE_PREF, newValue as String).commit()
                val warning = "Если язык обложки не изменился очистите базу данных в приложении (Настройки -> Дополнительно -> Очистить базу данных)"
                Toast.makeText(screen.context, warning, Toast.LENGTH_LONG).show()
                titleLanguage
            }
        }
        screen.addPreference(serverPref)
        screen.addPreference(sortingPref)
        screen.addPreference(titleLanguagePref)
    }
}
