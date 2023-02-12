package eu.kanade.tachiyomi.multisrc.madara

import android.app.Application
import android.content.SharedPreferences
import android.util.Log
import android.widget.Toast
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceScreen
import androidx.preference.SwitchPreferenceCompat
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.asObservable
import eu.kanade.tachiyomi.source.ConfigurableSource
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import eu.kanade.tachiyomi.util.asJsoup
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.CacheControl
import okhttp3.FormBody
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import rx.Observable
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy
import java.io.IOException
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

abstract class Madara(
    override val name: String,
    override val baseUrl: String,
    final override val lang: String,
    private val dateFormat: SimpleDateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.US),
) : ParsedHttpSource(), ConfigurableSource {

    private val preferences: SharedPreferences by lazy {
        Injekt.get<Application>().getSharedPreferences("source_$id", 0x0000)
    }

    override val supportsLatest = true

    // override with true if you want useRandomUserAgentByDefault to be on by default for some source
    protected open val useRandomUserAgentByDefault: Boolean = false

    /**
     * override include/exclude user-agent string if needed
     *   some example:
     *      listOf("chrome")
     *      listOf("linux", "windows")
     *      listOf("108")
     */
    protected open val filterIncludeUserAgent: List<String> = listOf()
    protected open val filterExcludeUserAgent: List<String> = listOf()

    private var userAgent: String? = null
    private var checkedUa = false

    private val hasUaIntercept by lazy {
        client.interceptors.toString().contains("uaIntercept")
    }

    protected val uaIntercept = object : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val useRandomUa = preferences.getBoolean(PREF_KEY_RANDOM_UA, false)
            val customUa = preferences.getString(PREF_KEY_CUSTOM_UA, "")

            try {
                if (hasUaIntercept && (useRandomUa || customUa!!.isNotBlank())) {
                    Log.i("Extension_setting", "$TITLE_RANDOM_UA or $TITLE_CUSTOM_UA option is ENABLED")

                    if (customUa!!.isNotBlank() && useRandomUa.not()) {
                        userAgent = customUa
                    }

                    if (userAgent.isNullOrBlank() && !checkedUa) {
                        val uaResponse = chain.proceed(GET(UA_DB_URL))

                        if (uaResponse.isSuccessful) {
                            var listUserAgentString =
                                json.decodeFromString<Map<String, List<String>>>(uaResponse.body.string())["desktop"]

                            if (filterIncludeUserAgent.isNotEmpty()) {
                                listUserAgentString = listUserAgentString!!.filter {
                                    filterIncludeUserAgent.any { filter ->
                                        it.contains(filter, ignoreCase = true)
                                    }
                                }
                            }
                            if (filterExcludeUserAgent.isNotEmpty()) {
                                listUserAgentString = listUserAgentString!!.filterNot {
                                    filterExcludeUserAgent.any { filter ->
                                        it.contains(filter, ignoreCase = true)
                                    }
                                }
                            }
                            userAgent = listUserAgentString!!.random()
                            checkedUa = true
                        }

                        uaResponse.close()
                    }

                    if (userAgent.isNullOrBlank().not()) {
                        val newRequest = chain.request().newBuilder()
                            .header("User-Agent", userAgent!!.trim())
                            .build()

                        return chain.proceed(newRequest)
                    }
                }

                return chain.proceed(chain.request())
            } catch (e: Exception) {
                throw IOException(e.message)
            }
        }
    }

    override val client: OkHttpClient = network.cloudflareClient.newBuilder()
        .addInterceptor(uaIntercept)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    protected open val json: Json by injectLazy()

    /**
     * If enabled, will remove non-manga items in search.
     * Can be disabled if the source incorrectly sets the entry types.
     */
    protected open val filterNonMangaItems = true

    /**
     * Automatically fetched genres from the source to be used in the filters.
     */
    private var genresList: List<Genre> = emptyList()

    /**
     * Inner variable to control the genre fetching failed state.
     */
    private var fetchGenresFailed: Boolean = false

    /**
     * Inner variable to control how much tries the genres request was called.
     */
    private var fetchGenresAttempts: Int = 0

    /**
     * Disable it if you don't want the genres to be fetched.
     */
    protected open val fetchGenres: Boolean = true

    override fun headersBuilder(): Headers.Builder = Headers.Builder()
        .add("Referer", "$baseUrl/")

    // Popular Manga

    override fun popularMangaParse(response: Response): MangasPage {
        runCatching { fetchGenres() }
        return super.popularMangaParse(response)
    }

    // exclude/filter bilibili manga from list
    override fun popularMangaSelector() = "div.page-item-detail:not(:has(a[href*='bilibilicomics.com']))"

    open val popularMangaUrlSelector = "div.post-title a"

    override fun popularMangaFromElement(element: Element): SManga {
        val manga = SManga.create()

        with(element) {
            select(popularMangaUrlSelector).first()?.let {
                manga.setUrlWithoutDomain(it.attr("abs:href"))
                manga.title = it.ownText()
            }

            select("img").first()?.let {
                manga.thumbnail_url = imageFromElement(it)
            }
        }

        return manga
    }

    open fun formBuilder(page: Int, popular: Boolean) = FormBody.Builder().apply {
        add("action", "madara_load_more")
        add("page", (page - 1).toString())
        add("template", "madara-core/content/content-archive")
        add("vars[orderby]", "meta_value_num")
        add("vars[paged]", "1")
        add("vars[posts_per_page]", "20")
        add("vars[post_type]", "wp-manga")
        add("vars[post_status]", "publish")
        add("vars[meta_key]", if (popular) "_wp_manga_views" else "_latest_update")
        add("vars[order]", "desc")
        add("vars[sidebar]", if (popular) "full" else "right")
        add("vars[manga_archives_item_layout]", "big_thumbnail")

        if (filterNonMangaItems) {
            add("vars[meta_query][0][key]", "_wp_manga_chapter_type")
            add("vars[meta_query][0][value]", "manga")
        }
    }

    open val formHeaders: Headers by lazy { headersBuilder().build() }

    override fun popularMangaRequest(page: Int): Request {
        return POST(
            "$baseUrl/wp-admin/admin-ajax.php",
            formHeaders,
            formBuilder(page, true).build(),
            CacheControl.FORCE_NETWORK,
        )
    }

    override fun popularMangaNextPageSelector(): String? = "body:not(:has(.no-posts))"

    // Latest Updates

    override fun latestUpdatesSelector() = popularMangaSelector()

    override fun latestUpdatesFromElement(element: Element): SManga {
        // Even if it's different from the popular manga's list, the relevant classes are the same
        return popularMangaFromElement(element)
    }

    override fun latestUpdatesRequest(page: Int): Request {
        return POST("$baseUrl/wp-admin/admin-ajax.php", formHeaders, formBuilder(page, false).build(), CacheControl.FORCE_NETWORK)
    }

    override fun latestUpdatesNextPageSelector(): String? = popularMangaNextPageSelector()

    override fun latestUpdatesParse(response: Response): MangasPage {
        val mp = super.latestUpdatesParse(response)
        val mangas = mp.mangas.distinctBy { it.url }
        return MangasPage(mangas, mp.hasNextPage)
    }

    // Search Manga

    open val mangaSubString = "manga"

    /**
     * If enabled, the search will use the madara_load_more action instead of
     * the normal page. This allows more control over the query and will permit
     * the filtering of non-manga items such as novels or videos.
     */
    open val useLoadMoreSearch = true

    open fun searchFormBuilder(page: Int, showOnlyManga: Boolean): FormBody.Builder = FormBody.Builder().apply {
        add("action", "madara_load_more")
        add("page", (page - 1).toString())
        add("template", "madara-core/content/content-search")
        add("vars[paged]", "1")
        add("vars[template]", "archive")
        add("vars[sidebar]", "right")
        add("vars[post_type]", "wp-manga")
        add("vars[post_status]", "publish")
        add("vars[manga_archives_item_layout]", "big_thumbnail")
        add("vars[posts_per_page]", "20")

        if (filterNonMangaItems && showOnlyManga) {
            add("vars[meta_query][0][key]", "_wp_manga_chapter_type")
            add("vars[meta_query][0][value]", "manga")
        }
    }

    override fun fetchSearchManga(page: Int, query: String, filters: FilterList): Observable<MangasPage> {
        if (query.startsWith(URL_SEARCH_PREFIX) && !useLoadMoreSearch) {
            val mangaUrl = "$baseUrl/$mangaSubString/${query.substringAfter(URL_SEARCH_PREFIX)}"
            return client.newCall(GET(mangaUrl, headers))
                .asObservable().map { response ->
                    MangasPage(listOf(mangaDetailsParse(response.asJsoup()).apply { url = "/$mangaSubString/${query.substringAfter(URL_SEARCH_PREFIX)}/" }), false)
                }
        }
        return client.newCall(searchMangaRequest(page, query, filters))
            .asObservable().doOnNext { response ->
                if (!response.isSuccessful) {
                    response.close()
                    // Error message for exceeding last page
                    if (response.code == 404) {
                        error("Already on the Last Page!")
                    } else {
                        throw Exception("HTTP error ${response.code}")
                    }
                }
            }
            .map { response ->
                searchMangaParse(response)
            }
    }

    protected open fun searchPage(page: Int): String = "page/$page/"

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        if (useLoadMoreSearch) {
            return searchLoadMoreRequest(page, query, filters)
        }

        val url = "$baseUrl/${searchPage(page)}".toHttpUrlOrNull()!!.newBuilder()
        url.addQueryParameter("s", query)
        url.addQueryParameter("post_type", "wp-manga")
        filters.forEach { filter ->
            when (filter) {
                is AuthorFilter -> {
                    if (filter.state.isNotBlank()) {
                        url.addQueryParameter("author", filter.state)
                    }
                }
                is ArtistFilter -> {
                    if (filter.state.isNotBlank()) {
                        url.addQueryParameter("artist", filter.state)
                    }
                }
                is YearFilter -> {
                    if (filter.state.isNotBlank()) {
                        url.addQueryParameter("release", filter.state)
                    }
                }
                is StatusFilter -> {
                    filter.state.forEach {
                        if (it.state) {
                            url.addQueryParameter("status[]", it.id)
                        }
                    }
                }
                is OrderByFilter -> {
                    if (filter.state != 0) {
                        url.addQueryParameter("m_orderby", filter.toUriPart())
                    }
                }
                is AdultContentFilter -> {
                    url.addQueryParameter("adult", filter.toUriPart())
                }
                is GenreConditionFilter -> {
                    url.addQueryParameter("op", filter.toUriPart())
                }
                is GenreList -> {
                    filter.state
                        .filter { it.state }
                        .let { list ->
                            if (list.isNotEmpty()) { list.forEach { genre -> url.addQueryParameter("genre[]", genre.id) } }
                        }
                }
                else -> {}
            }
        }
        return GET(url.toString(), headers)
    }

    protected open fun searchLoadMoreRequest(page: Int, query: String, filters: FilterList): Request {
        val showOnlyManga = filters.filterIsInstance<ShowOnlyMangaFilter>()
            .firstOrNull()?.state ?: true

        val formBodyBuilder = searchFormBuilder(page, showOnlyManga).apply {
            if (query.startsWith(URL_SEARCH_PREFIX)) {
                add("vars[name]", query.removePrefix(URL_SEARCH_PREFIX))

                return@apply
            }

            add("vars[s]", query)

            var metaQueryIdx = if (filterNonMangaItems && showOnlyManga) 1 else 0
            var taxQueryIdx = 0
            val genres = filters.filterIsInstance<GenreList>().firstOrNull()?.state
                ?.filter { it.state }
                ?.map { it.id }
                .orEmpty()

            filters.forEach { filter ->
                when (filter) {
                    is AuthorFilter -> {
                        if (filter.state.isNotBlank()) {
                            add("vars[tax_query][$taxQueryIdx][taxonomy]", "wp-manga-author")
                            add("vars[tax_query][$taxQueryIdx][field]", "name")
                            add("vars[tax_query][$taxQueryIdx][terms]", filter.state)

                            taxQueryIdx++
                        }
                    }
                    is ArtistFilter -> {
                        if (filter.state.isNotBlank()) {
                            add("vars[tax_query][$taxQueryIdx][taxonomy]", "wp-manga-artist")
                            add("vars[tax_query][$taxQueryIdx][field]", "name")
                            add("vars[tax_query][$taxQueryIdx][terms]", filter.state)

                            taxQueryIdx++
                        }
                    }
                    is YearFilter -> {
                        if (filter.state.isNotBlank()) {
                            add("vars[tax_query][$taxQueryIdx][taxonomy]", "wp-manga-release")
                            add("vars[tax_query][$taxQueryIdx][field]", "name")
                            add("vars[tax_query][$taxQueryIdx][terms]", filter.state)

                            taxQueryIdx++
                        }
                    }
                    is StatusFilter -> {
                        val statuses = filter.state
                            .filter { it.state }
                            .map { it.id }

                        if (statuses.isNotEmpty()) {
                            add("vars[meta_query][$metaQueryIdx][key]", "_wp_manga_status")

                            statuses.forEachIndexed { i, slug ->
                                add("vars[meta_query][$metaQueryIdx][value][$i]", slug)
                            }

                            metaQueryIdx++
                        }
                    }
                    is OrderByFilter -> {
                        if (filter.state != 0) {
                            when (filter.toUriPart()) {
                                "latest" -> {
                                    add("vars[orderby]", "meta_value_num")
                                    add("vars[order]", "DESC")
                                    add("vars[meta_key]", "_latest_update")
                                }
                                "alphabet" -> {
                                    add("vars[orderby]", "post_title")
                                    add("vars[order]", "ASC")
                                }
                                "rating" -> {
                                    add("vars[orderby][query_average_reviews]", "DESC")
                                    add("vars[orderby][query_total_reviews]", "DESC")
                                }
                                "trending" -> {
                                    add("vars[orderby]", "meta_value_num")
                                    add("vars[meta_key]", "_wp_manga_week_views_value")
                                    add("vars[order]", "DESC")
                                }
                                "views" -> {
                                    add("vars[orderby]", "meta_value_num")
                                    add("vars[meta_key]", "_wp_manga_views")
                                    add("vars[order]", "DESC")
                                }
                                else -> {
                                    add("vars[orderby]", "date")
                                    add("vars[order]", "DESC")
                                }
                            }
                        }
                    }
                    is AdultContentFilter -> {
                        if (filter.state != 0) {
                            add("vars[meta_query][$metaQueryIdx][key]", "manga_adult_content")
                            add(
                                "vars[meta_query][$metaQueryIdx][compare]",
                                if (filter.state == 1) "not exists" else "exists",
                            )

                            metaQueryIdx++
                        }
                    }
                    is GenreConditionFilter -> {
                        if (filter.state == 1 && genres.isNotEmpty()) {
                            add("vars[tax_query][$taxQueryIdx][operation]", "AND")
                        }
                    }
                    is GenreList -> {
                        if (genres.isNotEmpty()) {
                            add("vars[tax_query][$taxQueryIdx][taxonomy]", "wp-manga-genre")
                            add("vars[tax_query][$taxQueryIdx][field]", "slug")

                            genres.forEachIndexed { i, slug ->
                                add("vars[tax_query][$taxQueryIdx][terms][$i]", slug)
                            }

                            taxQueryIdx++
                        }
                    }
                    else -> {}
                }
            }
        }

        val searchHeaders = headersBuilder()
            .add("X-Requested-With", "XMLHttpRequest")
            .build()

        return POST(
            "$baseUrl/wp-admin/admin-ajax.php",
            searchHeaders,
            formBodyBuilder.build(),
            CacheControl.FORCE_NETWORK,
        )
    }

    protected open val authorFilterTitle: String = when (lang) {
        "pt-BR" -> "Autor"
        else -> "Author"
    }

    protected open val artistFilterTitle: String = when (lang) {
        "pt-BR" -> "Artista"
        else -> "Artist"
    }

    protected open val yearFilterTitle: String = when (lang) {
        "pt-BR" -> "Ano de lançamento"
        else -> "Year of Released"
    }

    protected open val statusFilterTitle: String = when (lang) {
        "pt-BR" -> "Estado"
        else -> "Status"
    }

    protected open val statusFilterOptions: Array<String> = when (lang) {
        "pt-BR" -> arrayOf("Completo", "Em andamento", "Cancelado", "Pausado")
        else -> arrayOf("Completed", "Ongoing", "Canceled", "On Hold")
    }

    protected val statusFilterOptionsValues: Array<String> = arrayOf(
        "end",
        "on-going",
        "canceled",
        "on-hold",
    )

    protected open val orderByFilterTitle: String = when (lang) {
        "pt-BR" -> "Ordenar por"
        else -> "Order By"
    }

    protected open val orderByFilterOptions: Array<String> = when (lang) {
        "pt-BR" -> arrayOf(
            "Relevância",
            "Recentes",
            "A-Z",
            "Avaliação",
            "Tendência",
            "Visualizações",
            "Novos",
        )
        else -> arrayOf(
            "Relevance",
            "Latest",
            "A-Z",
            "Rating",
            "Trending",
            "Most Views",
            "New",
        )
    }

    protected val orderByFilterOptionsValues: Array<String> = arrayOf(
        "",
        "latest",
        "alphabet",
        "rating",
        "trending",
        "views",
        "new-manga",
    )

    protected open val genreConditionFilterTitle: String = when (lang) {
        "pt-BR" -> "Operador dos gêneros"
        else -> "Genre condition"
    }

    protected open val genreConditionFilterOptions: Array<String> = when (lang) {
        "pt-BR" -> arrayOf("OU", "E")
        else -> arrayOf("OR", "AND")
    }

    protected open val adultContentFilterTitle: String = when (lang) {
        "pt-BR" -> "Conteúdo adulto"
        else -> "Adult Content"
    }

    protected open val adultContentFilterOptions: Array<String> = when (lang) {
        "pt-BR" -> arrayOf("Indiferente", "Nenhum", "Somente")
        else -> arrayOf("All", "None", "Only")
    }

    protected open val genreFilterHeader: String = when (lang) {
        "pt-BR" -> "O filtro de gêneros pode não funcionar"
        else -> "Genres filter may not work for all sources"
    }

    protected open val genreFilterTitle: String = when (lang) {
        "pt-BR" -> "Gêneros"
        else -> "Genres"
    }

    protected open val genresMissingWarning: String = when (lang) {
        "pt-BR" -> "Aperte 'Redefinir' para tentar mostrar os gêneros"
        else -> "Press 'Reset' to attempt to show the genres"
    }

    protected open val showOnlyMangaEntriesLabel: String = when (lang) {
        "pt-BR" -> "Mostrar somente mangás"
        else -> "Show only manga entries"
    }

    protected class AuthorFilter(title: String) : Filter.Text(title)
    protected class ArtistFilter(title: String) : Filter.Text(title)
    protected class YearFilter(title: String) : Filter.Text(title)
    protected class StatusFilter(title: String, status: List<Tag>) :
        Filter.Group<Tag>(title, status)

    protected class OrderByFilter(title: String, options: List<Pair<String, String>>, state: Int = 0) :
        UriPartFilter(title, options.toTypedArray(), state)

    protected class GenreConditionFilter(title: String, options: Array<String>) : UriPartFilter(
        title,
        options.zip(arrayOf("", "1")).toTypedArray(),
    )

    protected class AdultContentFilter(title: String, options: Array<String>) : UriPartFilter(
        title,
        options.zip(arrayOf("", "0", "1")).toTypedArray(),
    )

    protected class GenreList(title: String, genres: List<Genre>) : Filter.Group<Genre>(title, genres)
    class Genre(name: String, val id: String = name) : Filter.CheckBox(name)

    protected class ShowOnlyMangaFilter(label: String) : Filter.CheckBox(label, true)

    override fun getFilterList(): FilterList {
        val filters = mutableListOf(
            AuthorFilter(authorFilterTitle),
            ArtistFilter(artistFilterTitle),
            YearFilter(yearFilterTitle),
            StatusFilter(statusFilterTitle, getStatusList()),
            OrderByFilter(
                orderByFilterTitle,
                orderByFilterOptions.zip(orderByFilterOptionsValues),
                if (useLoadMoreSearch) 5 else 0,
            ),
            AdultContentFilter(adultContentFilterTitle, adultContentFilterOptions),
        )

        if (useLoadMoreSearch) {
            filters.add(ShowOnlyMangaFilter(showOnlyMangaEntriesLabel))
        }

        if (genresList.isNotEmpty()) {
            filters += listOf(
                Filter.Separator(),
                Filter.Header(genreFilterHeader),
                GenreConditionFilter(genreConditionFilterTitle, genreConditionFilterOptions),
                GenreList(genreFilterTitle, genresList),
            )
        } else if (fetchGenres) {
            filters += listOf(
                Filter.Separator(),
                Filter.Header(genresMissingWarning),
            )
        }

        return FilterList(filters)
    }

    protected fun getStatusList() = statusFilterOptionsValues
        .zip(statusFilterOptions)
        .map { Tag(it.first, it.second) }

    open class UriPartFilter(displayName: String, private val vals: Array<Pair<String, String>>, state: Int = 0) :
        Filter.Select<String>(displayName, vals.map { it.first }.toTypedArray(), state) {
        fun toUriPart() = vals[state].second
    }

    open class Tag(val id: String, name: String) : Filter.CheckBox(name)

    override fun searchMangaParse(response: Response): MangasPage {
        runCatching { fetchGenres() }
        return super.searchMangaParse(response)
    }

    override fun searchMangaSelector() = "div.c-tabs-item__content"

    override fun searchMangaFromElement(element: Element): SManga {
        val manga = SManga.create()

        with(element) {
            select("div.post-title a").first()?.let {
                manga.setUrlWithoutDomain(it.attr("abs:href"))
                manga.title = it.ownText()
            }
            select("img").first()?.let {
                manga.thumbnail_url = imageFromElement(it)
            }
        }

        return manga
    }

    override fun searchMangaNextPageSelector(): String? = when {
        useLoadMoreSearch -> popularMangaNextPageSelector()
        else -> "div.nav-previous, nav.navigation-ajax, a.nextpostslink"
    }

    // Manga Details Parse

    protected val completedStatusList: Array<String> = arrayOf(
        "Completed",
        "Completo",
        "Concluído",
        "Concluido",
        "Terminé",
        "Hoàn Thành",
        "مكتملة",
        "مكتمل",
    )

    protected val ongoingStatusList: Array<String> = arrayOf(
        "OnGoing", "Продолжается", "Updating", "Em Lançamento", "Em lançamento", "Em andamento",
        "Em Andamento", "En cours", "Ativo", "Lançando", "Đang Tiến Hành", "Devam Ediyor",
        "Devam ediyor", "In Corso", "In Arrivo", "مستمرة", "مستمر", "En Curso",
    )

    protected val hiatusStatusList: Array<String> = arrayOf(
        "On Hold",
    )

    protected val canceledStatusList: Array<String> = arrayOf(
        "Canceled",
    )

    override fun mangaDetailsParse(document: Document): SManga {
        val manga = SManga.create()
        with(document) {
            select(mangaDetailsSelectorTitle).first()?.let {
                manga.title = it.ownText()
            }
            select(mangaDetailsSelectorAuthor).eachText().filter {
                it.notUpdating()
            }.joinToString().takeIf { it.isNotBlank() }?.let {
                manga.author = it
            }
            select(mangaDetailsSelectorArtist).eachText().filter {
                it.notUpdating()
            }.joinToString().takeIf { it.isNotBlank() }?.let {
                manga.artist = it
            }
            select(mangaDetailsSelectorDescription).let {
                if (it.select("p").text().isNotEmpty()) {
                    manga.description = it.select("p").joinToString(separator = "\n\n") { p ->
                        p.text().replace("<br>", "\n")
                    }
                } else {
                    manga.description = it.text()
                }
            }
            select(mangaDetailsSelectorThumbnail).first()?.let {
                manga.thumbnail_url = imageFromElement(it)
            }
            select(mangaDetailsSelectorStatus).last()?.let {
                manga.status = when (it.text()) {
                    in completedStatusList -> SManga.COMPLETED
                    in ongoingStatusList -> SManga.ONGOING
                    in hiatusStatusList -> SManga.ON_HIATUS
                    in canceledStatusList -> SManga.CANCELLED
                    else -> SManga.UNKNOWN
                }
            }
            val genres = select(mangaDetailsSelectorGenre)
                .map { element -> element.text().lowercase(Locale.ROOT) }
                .toMutableSet()

            // add tag(s) to genre
            val mangaTitle = try {
                manga.title
            } catch (_: UninitializedPropertyAccessException) {
                "not initialized"
            }

            if (mangaDetailsSelectorTag.isNotEmpty()) {
                select(mangaDetailsSelectorTag).forEach { element ->
                    if (genres.contains(element.text()).not() &&
                        element.text().length <= 25 &&
                        element.text().contains("read", true).not() &&
                        element.text().contains(name, true).not() &&
                        element.text().contains(name.replace(" ", ""), true).not() &&
                        element.text().contains(mangaTitle, true).not() &&
                        element.text().contains(altName, true).not()
                    ) {
                        genres.add(element.text().lowercase(Locale.ROOT))
                    }
                }
            }

            // add manga/manhwa/manhua thinggy to genre
            document.select(seriesTypeSelector).firstOrNull()?.ownText()?.let {
                if (it.isEmpty().not() && it.notUpdating() && it != "-" && genres.contains(it).not()) {
                    genres.add(it.lowercase(Locale.ROOT))
                }
            }

            manga.genre = genres.toList().joinToString(", ") { it.capitalize(Locale.ROOT) }

            // add alternative name to manga description
            document.select(altNameSelector).firstOrNull()?.ownText()?.let {
                if (it.isBlank().not() && it.notUpdating()) {
                    manga.description = when {
                        manga.description.isNullOrBlank() -> altName + it
                        else -> manga.description + "\n\n$altName" + it
                    }
                }
            }
        }

        return manga
    }

    // Manga Details Selector
    open val mangaDetailsSelectorTitle = "div.post-title h3, div.post-title h1"
    open val mangaDetailsSelectorAuthor = "div.author-content > a"
    open val mangaDetailsSelectorArtist = "div.artist-content > a"
    open val mangaDetailsSelectorStatus = "div.summary-content"
    open val mangaDetailsSelectorDescription = "div.description-summary div.summary__content, div.summary_content div.post-content_item > h5 + div, div.summary_content div.manga-excerpt"
    open val mangaDetailsSelectorThumbnail = "div.summary_image img"
    open val mangaDetailsSelectorGenre = "div.genres-content a"
    open val mangaDetailsSelectorTag = "div.tags-content a"

    open val seriesTypeSelector = ".post-content_item:contains(Type) .summary-content"
    open val altNameSelector = ".post-content_item:contains(Alt) .summary-content"
    open val altName = when (lang) {
        "pt-BR" -> "Nomes alternativos: "
        else -> "Alternative Names: "
    }
    open val updatingRegex = "Updating|Atualizando".toRegex(RegexOption.IGNORE_CASE)

    fun String.notUpdating(): Boolean {
        return this.contains(updatingRegex).not()
    }

    protected open fun imageFromElement(element: Element): String? {
        return when {
            element.hasAttr("data-src") -> element.attr("abs:data-src")
            element.hasAttr("data-lazy-src") -> element.attr("abs:data-lazy-src")
            element.hasAttr("srcset") -> element.attr("abs:srcset").substringBefore(" ")
            else -> element.attr("abs:src")
        }
    }

    /**
     * Set it to true if the source uses the new AJAX endpoint to
     * fetch the manga chapters instead of the old admin-ajax.php one.
     */
    protected open val useNewChapterEndpoint: Boolean = false

    /**
     * Internal attribute to control if it should always use the
     * new chapter endpoint after a first check if useNewChapterEndpoint is
     * set to false. Using a separate variable to still allow the other
     * one to be overridable manually in each source.
     */
    private var oldChapterEndpointDisabled: Boolean = false

    protected open fun oldXhrChaptersRequest(mangaId: String): Request {
        val form = FormBody.Builder()
            .add("action", "manga_get_chapters")
            .add("manga", mangaId)
            .build()

        val xhrHeaders = headersBuilder()
            .add("Content-Length", form.contentLength().toString())
            .add("Content-Type", form.contentType().toString())
            .add("Referer", "$baseUrl/")
            .add("X-Requested-With", "XMLHttpRequest")
            .build()

        return POST("$baseUrl/wp-admin/admin-ajax.php", xhrHeaders, form)
    }

    protected open fun xhrChaptersRequest(mangaUrl: String): Request {
        val xhrHeaders = headersBuilder()
            .add("Referer", "$baseUrl/")
            .add("X-Requested-With", "XMLHttpRequest")
            .build()

        return POST("$mangaUrl/ajax/chapters", xhrHeaders)
    }

    override fun chapterListParse(response: Response): List<SChapter> {
        val document = response.asJsoup()
        val chaptersWrapper = document.select("div[id^=manga-chapters-holder]")

        var chapterElements = document.select(chapterListSelector())

        if (chapterElements.isEmpty() && !chaptersWrapper.isNullOrEmpty()) {
            val mangaUrl = document.location().removeSuffix("/")
            val mangaId = chaptersWrapper.attr("data-id")

            var xhrRequest = if (useNewChapterEndpoint || oldChapterEndpointDisabled) {
                xhrChaptersRequest(mangaUrl)
            } else {
                oldXhrChaptersRequest(mangaId)
            }
            var xhrResponse = client.newCall(xhrRequest).execute()

            // Newer Madara versions throws HTTP 400 when using the old endpoint.
            if (!useNewChapterEndpoint && xhrResponse.code == 400) {
                xhrResponse.close()
                // Set it to true so following calls will be made directly to the new endpoint.
                oldChapterEndpointDisabled = true

                xhrRequest = xhrChaptersRequest(mangaUrl)
                xhrResponse = client.newCall(xhrRequest).execute()
            }

            chapterElements = xhrResponse.asJsoup().select(chapterListSelector())
            xhrResponse.close()
        }

        countViews(document)

        return chapterElements.map(::chapterFromElement)
    }

    override fun chapterListSelector() = "li.wp-manga-chapter"

    protected open fun chapterDateSelector() = "span.chapter-release-date"

    open val chapterUrlSelector = "a"

    // can cause some issue for some site. blocked by cloudflare when opening the chapter pages
    open val chapterUrlSuffix = "?style=list"

    override fun chapterFromElement(element: Element): SChapter {
        val chapter = SChapter.create()

        with(element) {
            select(chapterUrlSelector).first()?.let { urlElement ->
                chapter.url = urlElement.attr("abs:href").let {
                    it.substringBefore("?style=paged") + if (!it.endsWith(chapterUrlSuffix)) chapterUrlSuffix else ""
                }
                chapter.name = urlElement.text()
            }
            // Dates can be part of a "new" graphic or plain text
            // Added "title" alternative
            chapter.date_upload = select("img:not(.thumb)").firstOrNull()?.attr("alt")?.let { parseRelativeDate(it) }
                ?: select("span a").firstOrNull()?.attr("title")?.let { parseRelativeDate(it) }
                ?: parseChapterDate(select(chapterDateSelector()).firstOrNull()?.text())
        }

        return chapter
    }

    open fun parseChapterDate(date: String?): Long {
        date ?: return 0

        fun SimpleDateFormat.tryParse(string: String): Long {
            return try {
                parse(string)?.time ?: 0
            } catch (_: ParseException) {
                0
            }
        }

        return when {
            // Handle 'yesterday' and 'today', using midnight
            WordSet("yesterday", "يوم واحد").startsWith(date) -> {
                Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_MONTH, -1) // yesterday
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
            }
            WordSet("today").startsWith(date) -> {
                Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
            }
            WordSet("يومين").startsWith(date) -> {
                Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_MONTH, -2) // day before yesterday
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
            }
            WordSet("ago", "atrás", "önce", "قبل").endsWith(date) -> {
                parseRelativeDate(date)
            }
            date.contains(Regex("""\d(st|nd|rd|th)""")) -> {
                // Clean date (e.g. 5th December 2019 to 5 December 2019) before parsing it
                date.split(" ").map {
                    if (it.contains(Regex("""\d\D\D"""))) {
                        it.replace(Regex("""\D"""), "")
                    } else {
                        it
                    }
                }
                    .let { dateFormat.tryParse(it.joinToString(" ")) }
            }
            else -> dateFormat.tryParse(date)
        }
    }

    // Parses dates in this form:
    // 21 horas ago
    protected open fun parseRelativeDate(date: String): Long {
        val number = Regex("""(\d+)""").find(date)?.value?.toIntOrNull() ?: return 0
        val cal = Calendar.getInstance()

        return when {
            WordSet("hari", "gün", "jour", "día", "dia", "day", "วัน", "ngày", "giorni", "أيام").anyWordIn(date) -> cal.apply { add(Calendar.DAY_OF_MONTH, -number) }.timeInMillis
            WordSet("jam", "saat", "heure", "hora", "hour", "ชั่วโมง", "giờ", "ore", "ساعة").anyWordIn(date) -> cal.apply { add(Calendar.HOUR, -number) }.timeInMillis
            WordSet("menit", "dakika", "min", "minute", "minuto", "นาที", "دقائق").anyWordIn(date) -> cal.apply { add(Calendar.MINUTE, -number) }.timeInMillis
            WordSet("detik", "segundo", "second", "วินาที").anyWordIn(date) -> cal.apply { add(Calendar.SECOND, -number) }.timeInMillis
            WordSet("week").anyWordIn(date) -> cal.apply { add(Calendar.DAY_OF_MONTH, -number * 7) }.timeInMillis
            WordSet("month").anyWordIn(date) -> cal.apply { add(Calendar.MONTH, -number) }.timeInMillis
            WordSet("year").anyWordIn(date) -> cal.apply { add(Calendar.YEAR, -number) }.timeInMillis
            else -> 0
        }
    }

    override fun pageListRequest(chapter: SChapter): Request {
        if (chapter.url.startsWith("http")) {
            return GET(chapter.url, headers)
        }
        return super.pageListRequest(chapter)
    }

    open val pageListParseSelector = "div.page-break, li.blocks-gallery-item, .reading-content .text-left:not(:has(.blocks-gallery-item)) img"

    override fun pageListParse(document: Document): List<Page> {
        countViews(document)

        return document.select(pageListParseSelector).mapIndexed { index, element ->
            Page(
                index,
                document.location(),
                element.select("img").first()?.let {
                    it.absUrl(if (it.hasAttr("data-src")) "data-src" else "src")
                },
            )
        }
    }

    override fun imageRequest(page: Page): Request {
        return GET(page.imageUrl!!, headers.newBuilder().set("Referer", page.url).build())
    }

    override fun imageUrlParse(document: Document) = ""

    /**
     * Set it to false if you want to disable the extension reporting the view count
     * back to the source website through admin-ajax.php.
     */
    protected open val sendViewCount: Boolean = true

    protected open fun countViewsRequest(document: Document): Request? {
        val wpMangaData = document.select("script#wp-manga-js-extra").firstOrNull()
            ?.data() ?: return null

        val wpMangaInfo = wpMangaData
            .substringAfter("var manga = ")
            .substringBeforeLast(";")

        val wpManga = runCatching { json.parseToJsonElement(wpMangaInfo).jsonObject }
            .getOrNull() ?: return null

        if (wpManga["enable_manga_view"]?.jsonPrimitive?.content == "1") {
            val formBuilder = FormBody.Builder()
                .add("action", "manga_views")
                .add("manga", wpManga["manga_id"]!!.jsonPrimitive.content)

            if (wpManga["chapter_slug"] != null) {
                formBuilder.add("chapter", wpManga["chapter_slug"]!!.jsonPrimitive.content)
            }

            val formBody = formBuilder.build()

            val newHeaders = headersBuilder()
                .set("Content-Length", formBody.contentLength().toString())
                .set("Content-Type", formBody.contentType().toString())
                .set("Referer", document.location())
                .build()

            val ajaxUrl = wpManga["ajax_url"]!!.jsonPrimitive.content

            return POST(ajaxUrl, newHeaders, formBody)
        }

        return null
    }

    /**
     * Send the view count request to the Madara endpoint.
     *
     * @param document The response document with the wp-manga data
     */
    protected open fun countViews(document: Document) {
        if (!sendViewCount) {
            return
        }

        val request = countViewsRequest(document) ?: return
        runCatching { client.newCall(request).execute().close() }
    }

    /**
     * Fetch the genres from the source to be used in the filters.
     */
    protected open fun fetchGenres() {
        if (fetchGenres && fetchGenresAttempts <= 3 && (genresList.isEmpty() || fetchGenresFailed)) {
            val genres = runCatching {
                client.newCall(genresRequest()).execute()
                    .use { parseGenres(it.asJsoup()) }
            }

            fetchGenresFailed = genres.isFailure
            genresList = genres.getOrNull().orEmpty()
            fetchGenresAttempts++
        }
    }

    /**
     * The request to the search page (or another one) that have the genres list.
     */
    protected open fun genresRequest(): Request {
        return GET("$baseUrl/?s=genre&post_type=wp-manga", headers)
    }

    /**
     * Get the genres from the search page document.
     *
     * @param document The search page document
     */
    protected open fun parseGenres(document: Document): List<Genre> {
        return document.selectFirst("div.checkbox-group")
            ?.select("div.checkbox")
            .orEmpty()
            .map { li ->
                Genre(
                    li.selectFirst("label")!!.text(),
                    li.selectFirst("input[type=checkbox]")!!.`val`(),
                )
            }
    }

    override fun setupPreferenceScreen(screen: PreferenceScreen) {
        if (hasUaIntercept) {
            val prefUserAgent = SwitchPreferenceCompat(screen.context).apply {
                key = PREF_KEY_RANDOM_UA
                title = TITLE_RANDOM_UA
                summary = if (preferences.getBoolean(PREF_KEY_RANDOM_UA, useRandomUserAgentByDefault)) userAgent else ""
                setDefaultValue(useRandomUserAgentByDefault)

                setOnPreferenceChangeListener { _, newValue ->
                    val useRandomUa = newValue as Boolean
                    preferences.edit().putBoolean(PREF_KEY_RANDOM_UA, useRandomUa).apply()
                    if (!useRandomUa) {
                        Toast.makeText(screen.context, RESTART_APP_STRING, Toast.LENGTH_LONG).show()
                    } else {
                        userAgent = null
                        if (preferences.getString(PREF_KEY_CUSTOM_UA, "").isNullOrBlank().not()) {
                            Toast.makeText(screen.context, SUMMARY_CLEANING_CUSTOM_UA, Toast.LENGTH_LONG).show()
                        }
                    }

                    preferences.edit().putString(PREF_KEY_CUSTOM_UA, "").apply()
                    // prefCustomUserAgent.summary = ""
                    true
                }
            }
            screen.addPreference(prefUserAgent)

            val prefCustomUserAgent = EditTextPreference(screen.context).apply {
                key = PREF_KEY_CUSTOM_UA
                title = TITLE_CUSTOM_UA
                summary = preferences.getString(PREF_KEY_CUSTOM_UA, "")!!.trim()
                setOnPreferenceChangeListener { _, newValue ->
                    val customUa = newValue as String
                    preferences.edit().putString(PREF_KEY_CUSTOM_UA, customUa).apply()
                    if (customUa.isBlank()) {
                        Toast.makeText(screen.context, RESTART_APP_STRING, Toast.LENGTH_LONG).show()
                    } else {
                        userAgent = null
                    }
                    summary = customUa.trim()
                    prefUserAgent.summary = ""
                    prefUserAgent.isChecked = false
                    true
                }
            }
            screen.addPreference(prefCustomUserAgent)
        } else {
            Toast.makeText(screen.context, DOESNOT_SUPPORT_STRING, Toast.LENGTH_LONG).show()
        }
    }

    companion object {
        const val TITLE_RANDOM_UA = "Use Random Latest User-Agent"
        const val PREF_KEY_RANDOM_UA = "pref_key_random_ua"

        const val TITLE_CUSTOM_UA = "Custom User-Agent"
        const val PREF_KEY_CUSTOM_UA = "pref_key_custom_ua"

        const val SUMMARY_CLEANING_CUSTOM_UA = "$TITLE_CUSTOM_UA cleared."

        const val RESTART_APP_STRING = "Restart Tachiyomi to apply new setting."
        const val DOESNOT_SUPPORT_STRING = "This extension doesn't support User-Agent options."
        const val URL_SEARCH_PREFIX = "slug:"
        private const val UA_DB_URL = "https://tachiyomiorg.github.io/user-agents/user-agents.json"
    }
}

class WordSet(private vararg val words: String) {
    fun anyWordIn(dateString: String): Boolean = words.any { dateString.contains(it, ignoreCase = true) }
    fun startsWith(dateString: String): Boolean = words.any { dateString.startsWith(it, ignoreCase = true) }
    fun endsWith(dateString: String): Boolean = words.any { dateString.endsWith(it, ignoreCase = true) }
}
