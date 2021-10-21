package eu.kanade.tachiyomi.extension.all.comickfun

import eu.kanade.tachiyomi.lib.ratelimit.SpecificHostRateLimitInterceptor
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.asObservableSuccess
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import okhttp3.CacheControl
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import rx.Observable
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.lang.UnsupportedOperationException

const val SEARCH_PAGE_LIMIT = 100

abstract class ComickFun(override val lang: String, private val comickFunLang: String) : HttpSource() {
    override val name = "Comick.fun"
    final override val baseUrl = "https://comick.fun"
    private val apiBase = "https://api.comick.fun"
    override val supportsLatest = true

    @ExperimentalSerializationApi
    private val json: Json by lazy {
        Json(from = Injekt.get()) {
            serializersModule = SerializersModule {
                polymorphic(SManga::class) { default { SMangaDeserializer() } }
                polymorphic(SChapter::class) { default { SChapterDeserializer() } }
            }
        }
    }

    @ExperimentalSerializationApi
    private val mangaIdCache = SMangaDeserializer.mangaIdCache

    final override fun headersBuilder() = Headers.Builder().apply {
        add("User-Agent", "Tachiyomi " + System.getProperty("http.agent"))
    }

    final override val client: OkHttpClient

    init {
        val builder = super.client.newBuilder()
        if (comickFunLang != "all")
        // Add interceptor to enforce language
            builder.addInterceptor(
                Interceptor { chain ->
                    val request = chain.request()
                    val path = request.url.pathSegments
                    when {
                        ((path.size == 1) && (path[0] == "chapter")) ||
                            ((path.size == 3) && (path[0] == "comic") && (path[2] == "chapter")) ->
                            chain.proceed(request.newBuilder().url(request.url.newBuilder().addQueryParameter("lang", comickFunLang).build()).build())
                        else -> chain.proceed(request)
                    }
                }
            )
        // Add interceptor to append "tachiyomi=true" to all requests (api returns slightly different response to 3rd parties)
        builder.addInterceptor(
            Interceptor { chain ->
                val request = chain.request()
                return@Interceptor when (request.url.toString().startsWith(apiBase)) {
                    true -> chain.proceed(request.newBuilder().url(request.url.newBuilder().addQueryParameter("tachiyomi", "true").build()).build())
                    false -> chain.proceed(request)
                }
            }
        )
        // Add interceptor to ratelimit api calls
        builder.addNetworkInterceptor(SpecificHostRateLimitInterceptor(apiBase.toHttpUrl(), 2))
        this.client = builder.build()
    }

    /**  Utils **/

    /** Returns an observable which emits a single value -> the manga's id **/
    @ExperimentalSerializationApi
    private fun chapterId(manga: SManga): Observable<Int> {
        val mangaSlug = slug(manga)
        return mangaIdCache[mangaSlug]?.let { Observable.just(it) }
            ?: fetchMangaDetails(manga).map { mangaIdCache[mangaSlug] }
    }

    /** Returns an identifier referred to as `hid` for chapter **/
    private fun hid(chapter: SChapter) = "$baseUrl${chapter.url}".toHttpUrl().pathSegments[2].substringBefore("-")

    /** Returns an identifier referred to as a  `slug` for manga **/
    private fun slug(manga: SManga) = "$baseUrl${manga.url}".toHttpUrl().pathSegments[1]

    /** Popular Manga **/

    @ExperimentalSerializationApi
    override fun fetchPopularManga(page: Int) = fetchSearchManga(page, "", FilterList(emptyList()))
    override fun popularMangaRequest(page: Int) = throw UnsupportedOperationException("Not used")
    override fun popularMangaParse(response: Response) = throw UnsupportedOperationException("Not used")

    /** Latest Manga **/
    @ExperimentalSerializationApi
    override fun latestUpdatesParse(response: Response): MangasPage {
        val noResults = MangasPage(emptyList(), false)
        if (response.code == 204)
            return noResults
        return json.decodeFromString<List<SManga>>(
            deserializer = ListSerializer(deepSelectDeserializer("md_comics")),
            response.body!!.string()
        ).let { MangasPage(it, true) }
    }

    override fun latestUpdatesRequest(page: Int): Request {
        val url = "$apiBase/chapter".toHttpUrl().newBuilder()
            .addQueryParameter("page", "${page - 1}")
            .addQueryParameter("device-memory", "8")
        return GET("$url", headers)
    }

    @ExperimentalSerializationApi
    override fun fetchSearchManga(page: Int, query: String, filters: FilterList): Observable<MangasPage> {
        if (!query.startsWith(SLUG_SEARCH_PREFIX))
            return super.fetchSearchManga(page, query, filters)

        // deeplinking
        val potentialUrl = "/comic/${query.substringAfter(SLUG_SEARCH_PREFIX)}"
        return fetchMangaDetails(SManga.create().apply { this.url = potentialUrl })
            .map { MangasPage(listOf(it.apply { this.url = potentialUrl }), false) }
            .onErrorReturn { MangasPage(emptyList(), false) }
    }

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        val url = apiBase.toHttpUrl().newBuilder().addPathSegment("search")
        if (query.isNotEmpty()) {
            url.addQueryParameter("q", query)
        } else {
            url.addQueryParameter("page", "$page")
                .addQueryParameter("limit", "$SEARCH_PAGE_LIMIT")
            filters.forEach { filter ->
                when (filter) {
                    is UrlEncoded -> filter.encode(url)
                }
            }
        }
        return GET("$url", headers)
    }

    @ExperimentalSerializationApi
    override fun searchMangaParse(response: Response) = json.decodeFromString<List<SManga>>(response.body!!.string())
        .let { MangasPage(it, it.size == SEARCH_PAGE_LIMIT) }

    /** Manga Details **/

    private fun apiMangaDetailsRequest(manga: SManga): Request {
        return GET("$apiBase/comic/${slug(manga)}", headers)
    }

    // Shenanigans to allow "open in webview" to show a webpage instead of JSON
    @ExperimentalSerializationApi
    override fun fetchMangaDetails(manga: SManga): Observable<SManga> {
        return client.newCall(apiMangaDetailsRequest(manga))
            .asObservableSuccess()
            .map { response ->
                mangaDetailsParse(response).apply { initialized = true }
            }
    }

    @ExperimentalSerializationApi
    override fun mangaDetailsParse(response: Response) = json.decodeFromString(
        deserializer = jsonFlatten<SManga>(objKey = "comic", "id", "title", "desc", "status", "country", "slug"),
        response.body!!.string()
    )

    /** Chapter List **/

    private fun chapterListRequest(page: Int, mangaId: Int) =
        GET("$apiBase/comic/$mangaId/chapter?page=$page&limit=$SEARCH_PAGE_LIMIT", headers)

    @ExperimentalSerializationApi
    override fun fetchChapterList(manga: SManga): Observable<List<SChapter>> {
        return if (manga.status != SManga.LICENSED) {
            chapterId(manga).concatMap { id ->
                /**
                 * Returns an observable which emits the list of chapters found on a page,
                 * for every page starting from specified page
                 */
                fun getAllPagesFrom(page: Int, pred: Observable<List<SChapter>> = Observable.just(emptyList())): Observable<List<SChapter>> =
                    client.newCall(chapterListRequest(page, id))
                        .asObservableSuccess()
                        .concatMap { response ->
                            val cp = chapterListParse(response).map { it.apply { this.url = "${manga.url}${this.url}" } }
                            if (cp.size == SEARCH_PAGE_LIMIT)
                                getAllPagesFrom(page + 1, pred = pred.concatWith(Observable.just(cp))) // tail call to avoid blowing the stack
                            else // by the pigeon-hole principle
                                pred.concatWith(Observable.just(cp))
                        }
                getAllPagesFrom(1).reduce(List<SChapter>::plus)
            }
        } else {
            Observable.error(Exception("Licensed - No chapters to show"))
        }
    }

    @ExperimentalSerializationApi
    override fun chapterListParse(response: Response) = json.decodeFromString(
        deserializer = deepSelectDeserializer<List<SChapter>>("chapters"),
        response.body!!.string()
    )

    /** Page List **/

    override fun pageListRequest(chapter: SChapter) = GET("$apiBase/chapter/${hid(chapter)}", headers, CacheControl.FORCE_NETWORK)

    @ExperimentalSerializationApi
    override fun pageListParse(response: Response) =
        json.decodeFromString(
            deserializer = deepSelectDeserializer<List<String>>("chapter", "images", tDeserializer = ListSerializer(deepSelectDeserializer("url"))),
            response.body!!.string()
        ).mapIndexed { i, url -> Page(i, imageUrl = url) }

    override fun imageUrlParse(response: Response) = "" // idk what this does, leave me alone kotlin

    /** Filters **/

    private interface UrlEncoded {
        fun encode(url: HttpUrl.Builder)
    }

    private interface ArrayUrlParam : UrlEncoded {
        val paramName: String
        val selected: Sequence<LabeledValue>
        override fun encode(url: HttpUrl.Builder) {
            selected.forEach { url.addQueryParameter(paramName, it.value) }
        }
    }

    private interface QueryParam : UrlEncoded {
        val paramName: String
        val selected: LabeledValue
        override fun encode(url: HttpUrl.Builder) {
            url.addQueryParameter(paramName, selected.value)
        }
    }

    // essentially a named pair
    protected class LabeledValue(private val displayname: String, private val _value: String?) {
        val value: String get() = _value ?: displayname
        override fun toString(): String = displayname
    }

    private open class Select<T>(header: String, values: Array<T>, state: Int = 0) : Filter.Select<T>(header, values, state) {
        val selected: T
            get() = this.values[this.state]
    }

    private open class MultiSelect<T>(header: String, val elems: List<T>) :
        Filter.Group<Filter.CheckBox>(header, elems.map { object : Filter.CheckBox("$it") {} }) {
        val selected: Sequence<T>
            get() = this.elems.asSequence().filterIndexed { i, _ -> this.state[i].state }
    }

    private open class MultiTriSelect<T>(header: String, val elems: List<T>) :
        Filter.Group<Filter.TriState>(header, elems.map { object : Filter.TriState("$it") {} }) {
        val selected: Pair<Sequence<T>, Sequence<T>>
            get() {
                return this.elems.asSequence()
                    .mapIndexed { index, it -> index to it }
                    .filterNot { (index, _) -> this.state[index].isIgnored() }
                    .partition { (index, _) -> this.state[index].isIncluded() }
                    .let { (included, excluded) ->
                        included.asSequence().map { it.second } to excluded.asSequence().map { it.second }
                    }
            }
    }

    override fun getFilterList() = FilterList(
        Filter.Header("NOTE: Ignored if using text search!"),
        GenreFilter(),
        DemographicFilter(),
        TypesFilter(),
        CreatedAtFilter(),
        MinChaptersFilter(),
        SortFilter()
    )

    private fun GenreFilter() = object : MultiTriSelect<LabeledValue>("Genre", getGenreList()), UrlEncoded {
        val included = object : ArrayUrlParam {
            override val paramName = "genres"
            override var selected: Sequence<LabeledValue> = sequence {}
        }
        val excluded = object : ArrayUrlParam {
            override val paramName = "excludes"
            override var selected: Sequence<LabeledValue> = sequence {}
        }

        override fun encode(url: HttpUrl.Builder) {
            this.selected.let { (includedGenres, excludedGenres) ->
                included.apply { selected = includedGenres }.encode(url)
                excluded.apply { selected = excludedGenres }.encode(url)
            }
        }
    }

    private fun SortFilter() = object : Select<LabeledValue>("Sort", getSorts()), QueryParam {
        override val paramName = "sort"
    }

    private fun DemographicFilter() = object : MultiSelect<LabeledValue>("Demographic", getDemographics()), ArrayUrlParam {
        override val paramName = "demographic"
    }

    private fun TypesFilter() = object : MultiSelect<LabeledValue>("Type", getContentType()), ArrayUrlParam {
        override val paramName = "country"
    }

    private fun CreatedAtFilter() = object : Select<LabeledValue>("Created At", getCreatedAt()), QueryParam {
        override val paramName = "time"
        override fun encode(url: HttpUrl.Builder) {
            // api will reject a request with an empty time
            if (selected.value.isNotBlank()) super.encode(url)
        }
    }

    private fun MinChaptersFilter() = object : Filter.Text("Minimum Chapters", ""), UrlEncoded {
        override fun encode(url: HttpUrl.Builder) {
            if (state.isBlank()) return
            state.toIntOrNull()?.takeUnless { it < 0 }?.let {
                url.addQueryParameter("minimum", "$it")
            } ?: throw RuntimeException("Minimum must be an integer greater than 0")
        }
    }

    protected fun getGenreList() = listOf(
        LabeledValue("4-Koma", "4-koma"),
        LabeledValue("Action", "action"),
        LabeledValue("Adaptation", "adaptation"),
        LabeledValue("Adult", "adult"),
        LabeledValue("Adventure", "adventure"),
        LabeledValue("Aliens", "aliens"),
        LabeledValue("Animals", "animals"),
        LabeledValue("Anthology", "anthology"),
        LabeledValue("Award Winning", "award-winning"),
        LabeledValue("Comedy", "comedy"),
        LabeledValue("Cooking", "cooking"),
        LabeledValue("Crime", "crime"),
        LabeledValue("Crossdressing", "crossdressing"),
        LabeledValue("Delinquents", "delinquents"),
        LabeledValue("Demons", "demons"),
        LabeledValue("Doujinshi", "doujinshi"),
        LabeledValue("Drama", "drama"),
        LabeledValue("Ecchi", "ecchi"),
        LabeledValue("Fan Colored", "fan-colored"),
        LabeledValue("Fantasy", "fantasy"),
        LabeledValue("Full Color", "full-color"),
        LabeledValue("Gender Bender", "gender-bender"),
        LabeledValue("Genderswap", "genderswap"),
        LabeledValue("Ghosts", "ghosts"),
        LabeledValue("Gore", "gore"),
        LabeledValue("Gyaru", "gyaru"),
        LabeledValue("Harem", "harem"),
        LabeledValue("Historical", "historical"),
        LabeledValue("Horror", "horror"),
        LabeledValue("Incest", "incest"),
        LabeledValue("Isekai", "isekai"),
        LabeledValue("Loli", "loli"),
        LabeledValue("Long Strip", "long-strip"),
        LabeledValue("Mafia", "mafia"),
        LabeledValue("Magic", "magic"),
        LabeledValue("Magical Girls", "magical-girls"),
        LabeledValue("Martial Arts", "martial-arts"),
        LabeledValue("Mature", "mature"),
        LabeledValue("Mecha", "mecha"),
        LabeledValue("Medical", "medical"),
        LabeledValue("Military", "military"),
        LabeledValue("Monster Girls", "monster-girls"),
        LabeledValue("Monsters", "monsters"),
        LabeledValue("Music", "music"),
        LabeledValue("Mystery", "mystery"),
        LabeledValue("Ninja", "ninja"),
        LabeledValue("Office Workers", "office-workers"),
        LabeledValue("Official Colored", "official-colored"),
        LabeledValue("Oneshot", "oneshot"),
        LabeledValue("Philosophical", "philosophical"),
        LabeledValue("Police", "police"),
        LabeledValue("Post-Apocalyptic", "post-apocalyptic"),
        LabeledValue("Psychological", "psychological"),
        LabeledValue("Reincarnation", "reincarnation"),
        LabeledValue("Reverse Harem", "reverse-harem"),
        LabeledValue("Romance", "romance"),
        LabeledValue("Samurai", "samurai"),
        LabeledValue("School Life", "school-life"),
        LabeledValue("Sci-Fi", "sci-fi"),
        LabeledValue("Sexual Violence", "sexual-violence"),
        LabeledValue("Shota", "shota"),
        LabeledValue("Shoujo Ai", "shoujo-ai"),
        LabeledValue("Shounen Ai", "shounen-ai"),
        LabeledValue("Slice of Life", "slice-of-life"),
        LabeledValue("Smut", "smut"),
        LabeledValue("Sports", "sports"),
        LabeledValue("Superhero", "superhero"),
        LabeledValue("Supernatural", "supernatural"),
        LabeledValue("Survival", "survival"),
        LabeledValue("Thriller", "thriller"),
        LabeledValue("Time Travel", "time-travel"),
        LabeledValue("Traditional Games", "traditional-games"),
        LabeledValue("Tragedy", "tragedy"),
        LabeledValue("User Created", "user-created"),
        LabeledValue("Vampires", "vampires"),
        LabeledValue("Video Games", "video-games"),
        LabeledValue("Villainess", "villainess"),
        LabeledValue("Virtual Reality", "virtual-reality"),
        LabeledValue("Web Comic", "web-comic"),
        LabeledValue("Wuxia", "wuxia"),
        LabeledValue("Yaoi", "yaoi"),
        LabeledValue("Yuri", "yuri"),
        LabeledValue("Zombies", "zombies")
    )

    private fun getDemographics() = listOf(
        LabeledValue("Shonen", "1"),
        LabeledValue("Shoujo", "2"),
        LabeledValue("Seinen", "3"),
        LabeledValue("Josei", "4"),

    )

    private fun getContentType() = listOf(
        LabeledValue("Manga", "jp"),
        LabeledValue("Manhwa", "kr"),
        LabeledValue("Manhua", "cn"),
    )

    private fun getCreatedAt() = arrayOf(
        LabeledValue("", ""),
        LabeledValue("30 days", "30"),
        LabeledValue("3 months", "90"),
        LabeledValue("6 months", "180"),
        LabeledValue("1 year", "365"),
    )

    private fun getSorts() = arrayOf(
        LabeledValue("", ""),
        LabeledValue("Most follows", "follow"),
        LabeledValue("Most views", "view"),
        LabeledValue("High rating", "rating"),
        LabeledValue("Last updated", "uploaded")
    )

    companion object {
        const val SLUG_SEARCH_PREFIX = "id:"
    }
}
