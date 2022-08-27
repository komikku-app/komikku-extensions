package eu.kanade.tachiyomi.extension.all.kavita

import android.app.Application
import android.content.SharedPreferences
import android.text.InputType
import android.util.Log
import android.widget.Toast
import androidx.preference.EditTextPreference
import androidx.preference.MultiSelectListPreference
import eu.kanade.tachiyomi.AppInfo
import eu.kanade.tachiyomi.extension.all.kavita.dto.AuthenticationDto
import eu.kanade.tachiyomi.extension.all.kavita.dto.ChapterDto
import eu.kanade.tachiyomi.extension.all.kavita.dto.MangaFormat
import eu.kanade.tachiyomi.extension.all.kavita.dto.MetadataAgeRatings
import eu.kanade.tachiyomi.extension.all.kavita.dto.MetadataCollections
import eu.kanade.tachiyomi.extension.all.kavita.dto.MetadataGenres
import eu.kanade.tachiyomi.extension.all.kavita.dto.MetadataLanguages
import eu.kanade.tachiyomi.extension.all.kavita.dto.MetadataLibrary
import eu.kanade.tachiyomi.extension.all.kavita.dto.MetadataPayload
import eu.kanade.tachiyomi.extension.all.kavita.dto.MetadataPeople
import eu.kanade.tachiyomi.extension.all.kavita.dto.MetadataPubStatus
import eu.kanade.tachiyomi.extension.all.kavita.dto.MetadataTag
import eu.kanade.tachiyomi.extension.all.kavita.dto.PersonRole
import eu.kanade.tachiyomi.extension.all.kavita.dto.SearchResultsDto
import eu.kanade.tachiyomi.extension.all.kavita.dto.SeriesDto
import eu.kanade.tachiyomi.extension.all.kavita.dto.SeriesMetadataDto
import eu.kanade.tachiyomi.extension.all.kavita.dto.SeriesSearchDto
import eu.kanade.tachiyomi.extension.all.kavita.dto.ServerInfoDto
import eu.kanade.tachiyomi.extension.all.kavita.dto.VolumeDto
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.asObservableSuccess
import eu.kanade.tachiyomi.source.ConfigurableSource
import eu.kanade.tachiyomi.source.UnmeteredSource
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.Dns
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import rx.Observable
import rx.Single
import rx.schedulers.Schedulers
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy
import java.io.IOException
import java.net.ConnectException
import java.security.MessageDigest

class Kavita(private val suffix: String = "") : ConfigurableSource, UnmeteredSource, HttpSource() {
    class CompareChapters {
        companion object : Comparator<SChapter> {
            override fun compare(a: SChapter, b: SChapter): Int {
                if (a.chapter_number < 1.0 && b.chapter_number < 1.0) {
                    // Both are volumes, multiply by 100 and do normal sort
                    return if ((a.chapter_number * 100) < (b.chapter_number * 100)) {
                        1
                    } else -1
                } else {
                    if (a.chapter_number < 1.0 && b.chapter_number >= 1.0) {
                        // A is volume, b is not. A should sort first
                        return 1
                    } else if (a.chapter_number >= 1.0 && b.chapter_number < 1.0) {
                        return -1
                    }
                }
                if (a.chapter_number < b.chapter_number) return 1
                if (a.chapter_number > b.chapter_number) return -1
                return 0
            }
        }
    }
    override val client: OkHttpClient =
        network.client.newBuilder()
            .dns(Dns.SYSTEM)
            .build()
    override val id by lazy {
        val key = "${"kavita_$suffix"}/all/$versionId"
        val bytes = MessageDigest.getInstance("MD5").digest(key.toByteArray())
        (0..7).map { bytes[it].toLong() and 0xff shl 8 * (7 - it) }.reduce(Long::or) and Long.MAX_VALUE
    }
    private val preferences: SharedPreferences by lazy {
        Injekt.get<Application>().getSharedPreferences("source_$id", 0x0000)
    }
    override val name = "Kavita (${preferences.getString(KavitaConstants.customSourceNamePref,suffix)})"
    override val lang = "all"
    override val supportsLatest = true
    private val apiUrl by lazy { getPrefApiUrl() }
    override val baseUrl by lazy { getPrefBaseUrl() }
    private val address by lazy { getPrefAddress() } // Address for the Kavita OPDS url. Should be http(s)://host:(port)/api/opds/api-key
    private var jwtToken = "" // * JWT Token for authentication with the server. Stored in memory.
    private val LOG_TAG = """extension.all.kavita_${"[$suffix]_" + preferences.getString(KavitaConstants.customSourceNamePref,"[$suffix]")!!.replace(' ','_')}"""
    private var isLoged = false // Used to know if login was correct and not send login requests anymore

    private val json: Json by injectLazy()
    private val helper = KavitaHelper()
    private inline fun <reified T> Response.parseAs(): T =
        use {
            if (it.code == 401) {
                Log.e(LOG_TAG, "Http error 401 - Not authorized: ${it.request.url}")
                Throwable("Http error 401 - Not authorized: ${it.request.url}")
            }

            if (it.peekBody(Long.MAX_VALUE).string().isEmpty()) {
                Log.e(LOG_TAG, "Empty body String for request url: ${it.request.url}")
                throw EmptyRequestBody(
                    "Body of the response is empty. RequestUrl=${it.request.url}\nPlease check your kavita instance is up to date",
                    Throwable("Error. Request body is empty")
                )
            }
            json.decodeFromString(it.body?.string().orEmpty())
        }
    private inline fun <reified T : Enum<T>> safeValueOf(type: String): T {
        return java.lang.Enum.valueOf(T::class.java, type)
    }

    private var series = emptyList<SeriesDto>() // Acts as a cache

    override fun popularMangaRequest(page: Int): Request {
        if (!isLoged) {
            doLogin()
        }
        return POST(
            "$apiUrl/series/all?pageNumber=$page&libraryId=0&pageSize=20",
            headersBuilder().build(),
            buildFilterBody()
        )
    }

    override fun popularMangaParse(response: Response): MangasPage {
        try {
            val result = response.parseAs<List<SeriesDto>>()
            series = result
            val mangaList = result.map { item -> helper.createSeriesDto(item, apiUrl) }
            return MangasPage(mangaList, helper.hasNextPage(response))
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Possible outdated kavita", e)
            throw IOException("Please check your kavita version.\nv0.5+ is required for the extension to work properly")
        }
    }

    override fun latestUpdatesRequest(page: Int): Request {
        if (!isLoged) {
            doLogin()
        }
        return POST(
            "$apiUrl/series/all?pageNumber=$page&libraryId=0&pageSize=20",
            headersBuilder().build(),
            buildFilterBody(MetadataPayload(sorting = 4, sorting_asc = false, forceUseMetadataPayload = true))
        )
    }

    override fun latestUpdatesParse(response: Response): MangasPage {
        return popularMangaParse(response)
    }

    /**
     * SEARCH MANGA
     * **/
    private var isFilterOn = false // If any filter option is enabled this is true
    private var toFilter = MetadataPayload()
    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        toFilter = MetadataPayload() // need to reset it or will double
        isFilterOn = false
        filters.forEach { filter ->
            when (filter) {

                is SortFilter -> {
                    if (filter.state != null) {
                        toFilter.sorting = filter.state!!.index + 1
                        toFilter.sorting_asc = filter.state!!.ascending
                        // Disabled until search is stable
//                        isFilterOn = false
                    }
                }
                is StatusFilterGroup -> {
                    filter.state.forEach { content ->
                        if (content.state) {
                            toFilter.readStatus.add(content.name)
                            isFilterOn = true
                        }
                    }
                }
                is GenreFilterGroup -> {
                    filter.state.forEach { content ->
                        if (content.state) {
                            toFilter.genres.add(genresListMeta.find { it.title == content.name }!!.id)
                            isFilterOn = true
                        }
                    }
                }
                is UserRating -> {
                    toFilter.userRating = filter.state
                    isFilterOn = true
                }
                is TagFilterGroup -> {
                    filter.state.forEach { content ->
                        if (content.state) {
                            toFilter.tags.add(tagsListMeta.find { it.title == content.name }!!.id)
                            isFilterOn = true
                        }
                    }
                }
                is AgeRatingFilterGroup -> {
                    filter.state.forEach { content ->
                        if (content.state) {
                            toFilter.ageRating.add(ageRatingsListMeta.find { it.title == content.name }!!.value)
                            isFilterOn = true
                        }
                    }
                }
                is FormatsFilterGroup -> {
                    filter.state.forEach { content ->
                        if (content.state) {
                            toFilter.formats.add(MangaFormat.valueOf(content.name).ordinal)
                            isFilterOn = true
                        }
                    }
                }
                is CollectionFilterGroup -> {
                    filter.state.forEach { content ->
                        if (content.state) {
                            toFilter.collections.add(collectionsListMeta.find { it.title == content.name }!!.id)
                            isFilterOn = true
                        }
                    }
                }

                is LanguageFilterGroup -> {
                    filter.state.forEach { content ->
                        if (content.state) {
                            toFilter.language.add(languagesListMeta.find { it.title == content.name }!!.isoCode)
                            isFilterOn = true
                        }
                    }
                }
                is LibrariesFilterGroup -> {
                    filter.state.forEach { content ->
                        if (content.state) {
                            toFilter.libraries.add(libraryListMeta.find { it.name == content.name }!!.id)
                            isFilterOn = true
                        }
                    }
                }

                is PubStatusFilterGroup -> {
                    filter.state.forEach { content ->
                        if (content.state) {
                            toFilter.pubStatus.add(pubStatusListMeta.find { it.title == content.name }!!.value)
                            isFilterOn = true
                        }
                    }
                }

                is WriterPeopleFilterGroup -> {
                    filter.state.forEach { content ->
                        if (content.state) {
                            toFilter.peopleWriters.add(peopleListMeta.find { it.name == content.name }!!.id)
                            isFilterOn = true
                        }
                    }
                }
                is PencillerPeopleFilterGroup -> {
                    filter.state.forEach { content ->
                        if (content.state) {
                            toFilter.peoplePenciller.add(peopleListMeta.find { it.name == content.name }!!.id)
                            isFilterOn = true
                        }
                    }
                }
                is InkerPeopleFilterGroup -> {
                    filter.state.forEach { content ->
                        if (content.state) {
                            toFilter.peopleInker.add(peopleListMeta.find { it.name == content.name }!!.id)
                            isFilterOn = true
                        }
                    }
                }
                is ColoristPeopleFilterGroup -> {
                    filter.state.forEach { content ->
                        if (content.state) {
                            toFilter.peoplePeoplecolorist.add(peopleListMeta.find { it.name == content.name }!!.id)
                            isFilterOn = true
                        }
                    }
                }
                is LettererPeopleFilterGroup -> {
                    filter.state.forEach { content ->
                        if (content.state) {
                            toFilter.peopleLetterer.add(peopleListMeta.find { it.name == content.name }!!.id)
                            isFilterOn = true
                        }
                    }
                }
                is CoverArtistPeopleFilterGroup -> {
                    filter.state.forEach { content ->
                        if (content.state) {
                            toFilter.peopleCoverArtist.add(peopleListMeta.find { it.name == content.name }!!.id)
                            isFilterOn = true
                        }
                    }
                }
                is EditorPeopleFilterGroup -> {
                    filter.state.forEach { content ->
                        if (content.state) {
                            toFilter.peopleEditor.add(peopleListMeta.find { it.name == content.name }!!.id)
                            isFilterOn = true
                        }
                    }
                }
                is PublisherPeopleFilterGroup -> {
                    filter.state.forEach { content ->
                        if (content.state) {
                            toFilter.peoplePublisher.add(peopleListMeta.find { it.name == content.name }!!.id)
                            isFilterOn = true
                        }
                    }
                }
                is CharacterPeopleFilterGroup -> {
                    filter.state.forEach { content ->
                        if (content.state) {
                            toFilter.peopleCharacter.add(peopleListMeta.find { it.name == content.name }!!.id)
                            isFilterOn = true
                        }
                    }
                }
                is TranslatorPeopleFilterGroup -> {
                    filter.state.forEach { content ->
                        if (content.state) {
                            toFilter.peopleTranslator.add(peopleListMeta.find { it.name == content.name }!!.id)
                            isFilterOn = true
                        }
                    }
                }
                else -> isFilterOn = false
            }
        }

        if (query.isEmpty()) {
            isFilterOn = true
            return popularMangaRequest(page)
        } else {
            isFilterOn = false
            val url = "$apiUrl/Library/search".toHttpUrl().newBuilder()
                .addQueryParameter("queryString", query)
            return GET(url.toString(), headers)
        }
    }

    override fun searchMangaParse(response: Response): MangasPage {
        if (isFilterOn) {
            return popularMangaParse(response)
        } else {
            if (response.request.url.toString().contains("api/series/all"))
                return popularMangaParse(response)

            val result = response.parseAs<SearchResultsDto>().series
            val mangaList = result.map(::searchMangaFromObject)
            return MangasPage(mangaList, false)
        }
    }

    private fun searchMangaFromObject(obj: SeriesSearchDto): SManga = SManga.create().apply {
        title = obj.name
        thumbnail_url = "$apiUrl/Image/series-cover?seriesId=${obj.seriesId}"
        description = "None"
        url = "$apiUrl/Series/${obj.seriesId}"
    }

    /**
     * MANGA DETAILS (metadata about series)
     * **/

    override fun fetchMangaDetails(manga: SManga): Observable<SManga> {
        val serieId = helper.getIdFromUrl(manga.url)
        return client.newCall(GET("$apiUrl/series/metadata?seriesId=$serieId", headersBuilder().build()))
            .asObservableSuccess()
            .map { response ->
                mangaDetailsParse(response).apply { initialized = true }
            }
    }

    override fun mangaDetailsRequest(manga: SManga): Request {
        val serieId = helper.getIdFromUrl(manga.url)
        val foundSerie = series.find { dto -> dto.id == serieId }
        return GET(
            "$baseUrl/library/${foundSerie!!.libraryId}/series/$serieId",
            headersBuilder().build()
        )
    }

    override fun mangaDetailsParse(response: Response): SManga {

        val result = response.parseAs<SeriesMetadataDto>()

        val existingSeries = series.find { dto -> dto.id == result.seriesId }
        if (existingSeries != null) {
            val manga = helper.createSeriesDto(existingSeries, apiUrl)
            manga.url = "$apiUrl/Series/${result.seriesId}"
            manga.artist = result.coverArtists.joinToString { it.name }
            manga.description = result.summary
            manga.author = result.writers.joinToString { it.name }
            manga.genre = result.genres.joinToString { it.title }
            manga.thumbnail_url = "$apiUrl/image/series-cover?seriesId=${result.seriesId}"

            return manga
        }
        val serieDto = client.newCall(GET("$apiUrl/Series/${result.seriesId}", headersBuilder().build()))
            .execute()
            .parseAs<SeriesDto>()

        return SManga.create().apply {
            url = "$apiUrl/Series/${result.seriesId}"
            artist = result.coverArtists.joinToString { it.name }
            description = result.summary
            author = result.writers.joinToString { it.name }
            genre = result.genres.joinToString { it.title }
            title = serieDto.name
        }
    }

    /**
     * CHAPTER LIST
     * **/
    override fun chapterListRequest(manga: SManga): Request {
        val url = "$apiUrl/Series/volumes?seriesId=${helper.getIdFromUrl(manga.url)}"
        return GET(url, headersBuilder().build())
    }

    private fun chapterFromObject(obj: ChapterDto): SChapter = SChapter.create().apply {
        url = obj.id.toString()
        if (obj.number == "0" && obj.isSpecial) {
            // This is a special. Chapter name is special name
            name = obj.range
        } else {
            val cleanedName = obj.title.replaceFirst("^0+(?!$)".toRegex(), "")
            name = "Chapter $cleanedName"
        }
        date_upload = helper.parseDate(obj.created)
        chapter_number = obj.number.toFloat()
        scanlator = "${obj.pages} pages"
    }

    private fun chapterFromVolume(obj: ChapterDto, volume: VolumeDto): SChapter =
        SChapter.create().apply {
            // If there are multiple chapters to this volume, then prefix with Volume number
            if (volume.chapters.isNotEmpty() && obj.number != "0") {
                // This volume is not volume 0, hence they are not loose chapters
                // We just add a nice Volume X to the chapter title
                // Chapter-based Volume
                name = "Volume ${volume.number} Chapter ${obj.number}"
                chapter_number = obj.number.toFloat()
            } else if (obj.number == "0") {
                // Both specials and volume has chapter number 0
                if (volume.number == 0) {
                    // Treat as special
                    // Special is not in a volume
                    if (obj.range == "") {
                        // Special does not have any Title
                        name = "Chapter 0"
                        chapter_number = obj.number.toFloat()
                    } else {
                        // We use it's own special tile
                        name = obj.range
                        chapter_number = obj.number.toFloat()
                    }
                } else {
                    // Is a single-file volume
                    // We encode the chapter number to support tracking
                    name = "Volume ${volume.number}"
                    chapter_number = volume.number.toFloat() / 100
                }
            } else {
                name = "Unhandled Else Volume ${volume.number}"
            }
            url = obj.id.toString()
            date_upload = helper.parseDate(obj.created)

            scanlator = "${obj.pages} pages"
        }
    override fun chapterListParse(response: Response): List<SChapter> {
        try {
            val volumes = response.parseAs<List<VolumeDto>>()
            val allChapterList = mutableListOf<SChapter>()
            volumes.forEach { volume ->
                run {
                    if (volume.number == 0) {
                        // Regular chapters
                        volume.chapters.map {
                            allChapterList.add(chapterFromObject(it))
                        }
                    } else {
                        // Volume chapter
                        volume.chapters.map {
                            allChapterList.add(chapterFromVolume(it, volume))
                        }
                    }
                }
            }

            allChapterList.sortWith(CompareChapters)
            return allChapterList
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Unhandled exception parsing chapters. Send logs to kavita devs", e)
            throw IOException("Unhandled exception parsing chapters. Send logs to kavita devs")
        }
    }

    /**
     * Fetches the "url" of each page from the chapter
     * **/
    override fun pageListRequest(chapter: SChapter): Request {
        return GET("${chapter.url}/Reader/chapter-info")
    }

    override fun fetchPageList(chapter: SChapter): Observable<List<Page>> {
        val chapterId = chapter.url
        val numPages = chapter.scanlator?.replace(" pages", "")?.toInt()
        val numPages2 = "$numPages".toInt() - 1
        val pages = mutableListOf<Page>()
        for (i in 0..numPages2) {
            pages.add(
                Page(
                    index = i,
                    imageUrl = "$apiUrl/Reader/image?chapterId=$chapterId&page=$i"
                )
            )
        }
        return Observable.just(pages)
    }

    override fun pageListParse(response: Response): List<Page> =
        throw UnsupportedOperationException("Not used")

    override fun imageUrlParse(response: Response): String = ""

    /**
     * FILTERING
     **/

    /** Some variable names already exist. im not good at naming add Meta suffix */
    private var genresListMeta = emptyList<MetadataGenres>()
    private var tagsListMeta = emptyList<MetadataTag>()
    private var ageRatingsListMeta = emptyList<MetadataAgeRatings>()
    private var peopleListMeta = emptyList<MetadataPeople>()
    private var pubStatusListMeta = emptyList<MetadataPubStatus>()
    private var languagesListMeta = emptyList<MetadataLanguages>()
    private var libraryListMeta = emptyList<MetadataLibrary>()
    private var collectionsListMeta = emptyList<MetadataCollections>()
    private val personRoles = listOf(
        "Writer",
        "Penciller",
        "Inker",
        "Colorist",
        "Letterer",
        "CoverArtist",
        "Editor",
        "Publisher",
        "Character",
        "Translator"
    )

    private class UserRating() :
        Filter.Select<String>(
            "Minimum Rating",
            arrayOf(
                "Any",
                "1 star",
                "2 stars",
                "3 stars",
                "4 stars",
                "5 stars"
            )
        )

    private class SortFilter(sortables: Array<String>) : Filter.Sort("Sort by", sortables, Selection(0, true))

    val sortableList = listOf(
        Pair("Sort name", 1),
        Pair("Created", 2),
        Pair("Last modified", 3),
        Pair("Item added", 4),
    )
    private class StatusFilter(name: String) : Filter.CheckBox(name, false)
    private class StatusFilterGroup(filters: List<StatusFilter>) :
        Filter.Group<StatusFilter>("Status", filters)

    private class GenreFilter(name: String) : Filter.CheckBox(name, false)
    private class GenreFilterGroup(genres: List<GenreFilter>) :
        Filter.Group<GenreFilter>("Genres", genres)

    private class TagFilter(name: String) : Filter.CheckBox(name, false)
    private class TagFilterGroup(tags: List<TagFilter>) : Filter.Group<TagFilter>("Tags", tags)

    private class AgeRatingFilter(name: String) : Filter.CheckBox(name, false)
    private class AgeRatingFilterGroup(ageRatings: List<AgeRatingFilter>) :
        Filter.Group<AgeRatingFilter>("Age Rating", ageRatings)

    private class FormatFilter(name: String) : Filter.CheckBox(name, false)
    private class FormatsFilterGroup(formats: List<FormatFilter>) :
        Filter.Group<FormatFilter>("Formats", formats)

    private class CollectionFilter(name: String) : Filter.CheckBox(name, false)
    private class CollectionFilterGroup(collections: List<CollectionFilter>) :
        Filter.Group<CollectionFilter>("Collection", collections)

    private class LanguageFilter(name: String) : Filter.CheckBox(name, false)
    private class LanguageFilterGroup(languages: List<LanguageFilter>) :
        Filter.Group<LanguageFilter>("Language", languages)

    private class LibraryFilter(library: String) : Filter.CheckBox(library, false)
    private class LibrariesFilterGroup(libraries: List<LibraryFilter>) :
        Filter.Group<LibraryFilter>("Libraries", libraries)

    private class PubStatusFilter(name: String) : Filter.CheckBox(name, false)
    private class PubStatusFilterGroup(status: List<PubStatusFilter>) :
        Filter.Group<PubStatusFilter>("Publication Status", status)

    private class PeopleHeaderFilter(name: String) :
        Filter.Header(name)
    private class PeopleSeparatorFilter() :
        Filter.Separator()

    private class WriterPeopleFilter(name: String) : Filter.CheckBox(name, false)
    private class WriterPeopleFilterGroup(peoples: List<WriterPeopleFilter>) :
        Filter.Group<WriterPeopleFilter>("Writer", peoples)

    private class PencillerPeopleFilter(name: String) : Filter.CheckBox(name, false)
    private class PencillerPeopleFilterGroup(peoples: List<PencillerPeopleFilter>) :
        Filter.Group<PencillerPeopleFilter>("Penciller", peoples)

    private class InkerPeopleFilter(name: String) : Filter.CheckBox(name, false)
    private class InkerPeopleFilterGroup(peoples: List<InkerPeopleFilter>) :
        Filter.Group<InkerPeopleFilter>("Inker", peoples)

    private class ColoristPeopleFilter(name: String) : Filter.CheckBox(name, false)
    private class ColoristPeopleFilterGroup(peoples: List<ColoristPeopleFilter>) :
        Filter.Group<ColoristPeopleFilter>("Colorist", peoples)

    private class LettererPeopleFilter(name: String) : Filter.CheckBox(name, false)
    private class LettererPeopleFilterGroup(peoples: List<LettererPeopleFilter>) :
        Filter.Group<LettererPeopleFilter>("Letterer", peoples)

    private class CoverArtistPeopleFilter(name: String) : Filter.CheckBox(name, false)
    private class CoverArtistPeopleFilterGroup(peoples: List<CoverArtistPeopleFilter>) :
        Filter.Group<CoverArtistPeopleFilter>("Cover Artist", peoples)

    private class EditorPeopleFilter(name: String) : Filter.CheckBox(name, false)
    private class EditorPeopleFilterGroup(peoples: List<EditorPeopleFilter>) :
        Filter.Group<EditorPeopleFilter>("Editor", peoples)

    private class PublisherPeopleFilter(name: String) : Filter.CheckBox(name, false)
    private class PublisherPeopleFilterGroup(peoples: List<PublisherPeopleFilter>) :
        Filter.Group<PublisherPeopleFilter>("Publisher", peoples)

    private class CharacterPeopleFilter(name: String) : Filter.CheckBox(name, false)
    private class CharacterPeopleFilterGroup(peoples: List<CharacterPeopleFilter>) :
        Filter.Group<CharacterPeopleFilter>("Character", peoples)

    private class TranslatorPeopleFilter(name: String) : Filter.CheckBox(name, false)
    private class TranslatorPeopleFilterGroup(peoples: List<TranslatorPeopleFilter>) :
        Filter.Group<TranslatorPeopleFilter>("Translator", peoples)

    override fun getFilterList(): FilterList {
        val toggledFilters = getToggledFilters()

        val filters = try {
            val peopleInRoles = mutableListOf<List<MetadataPeople>>()
            personRoles.map { role ->
                val peoplesWithRole = mutableListOf<MetadataPeople>()
                peopleListMeta.map {
                    if (it.role == safeValueOf<PersonRole>(role).role) {
                        peoplesWithRole.add(it)
                    }
                }
                peopleInRoles.add(peoplesWithRole)
            }

            val filtersLoaded = mutableListOf<Filter<*>>()

            if (sortableList.isNotEmpty() and toggledFilters.contains("Sort Options")) {
                filtersLoaded.add(
                    SortFilter(sortableList.map { it.first }.toTypedArray())
                )
            }
            if (toggledFilters.contains("Read Status")) {
                filtersLoaded.add(
                    StatusFilterGroup(
                        listOf(
                            "notRead",
                            "inProgress",
                            "read"
                        ).map { StatusFilter(it) }
                    )
                )
            }

            if (genresListMeta.isNotEmpty() and toggledFilters.contains("Genres")) {
                filtersLoaded.add(
                    GenreFilterGroup(genresListMeta.map { GenreFilter(it.title) })
                )
            }
            if (tagsListMeta.isNotEmpty() and toggledFilters.contains("Tags")) {
                filtersLoaded.add(
                    TagFilterGroup(tagsListMeta.map { TagFilter(it.title) })
                )
            }
            if (ageRatingsListMeta.isNotEmpty() and toggledFilters.contains("Age Rating")) {
                filtersLoaded.add(
                    AgeRatingFilterGroup(ageRatingsListMeta.map { AgeRatingFilter(it.title) })
                )
            }
            if (toggledFilters.contains("Format")) {
                filtersLoaded.add(
                    FormatsFilterGroup(
                        listOf(
                            "Image",
                            "Archive",
                            "Unknown",
                            "Epub",
                            "Pdf"
                        ).map { FormatFilter(it) }
                    )
                )
            }
            if (collectionsListMeta.isNotEmpty() and toggledFilters.contains("Collections")) {
                filtersLoaded.add(
                    CollectionFilterGroup(collectionsListMeta.map { CollectionFilter(it.title) })
                )
            }
            if (languagesListMeta.isNotEmpty() and toggledFilters.contains("Languages")) {
                filtersLoaded.add(
                    LanguageFilterGroup(languagesListMeta.map { LanguageFilter(it.title) })
                )
            }
            if (libraryListMeta.isNotEmpty() and toggledFilters.contains("Libraries")) {
                filtersLoaded.add(
                    LibrariesFilterGroup(libraryListMeta.map { LibraryFilter(it.name) })
                )
            }
            if (pubStatusListMeta.isNotEmpty() and toggledFilters.contains("Publication Status")) {
                filtersLoaded.add(
                    PubStatusFilterGroup(pubStatusListMeta.map { PubStatusFilter(it.title) })
                )
            }
            if (pubStatusListMeta.isNotEmpty() and toggledFilters.contains("Rating")) {
                filtersLoaded.add(
                    UserRating()
                )
            }

            // People Metadata:
            if (personRoles.isNotEmpty() and toggledFilters.any { personRoles.contains(it) }) {
                filtersLoaded.addAll(
                    listOf<Filter<*>>(
                        PeopleHeaderFilter(""),
                        PeopleSeparatorFilter(),
                        PeopleHeaderFilter("PEOPLE")
                    )
                )
                if (peopleInRoles[0].isNotEmpty() and toggledFilters.contains("Writer")) {
                    filtersLoaded.add(
                        WriterPeopleFilterGroup(
                            peopleInRoles[0].map { WriterPeopleFilter(it.name) }
                        )
                    )
                }
                if (peopleInRoles[1].isNotEmpty() and toggledFilters.contains("Penciller")) {
                    filtersLoaded.add(
                        PencillerPeopleFilterGroup(
                            peopleInRoles[1].map { PencillerPeopleFilter(it.name) }
                        )
                    )
                }
                if (peopleInRoles[2].isNotEmpty() and toggledFilters.contains("Inker")) {
                    filtersLoaded.add(
                        InkerPeopleFilterGroup(
                            peopleInRoles[2].map { InkerPeopleFilter(it.name) }
                        )
                    )
                }
                if (peopleInRoles[3].isNotEmpty() and toggledFilters.contains("Colorist")) {
                    filtersLoaded.add(
                        ColoristPeopleFilterGroup(
                            peopleInRoles[3].map { ColoristPeopleFilter(it.name) }
                        )
                    )
                }
                if (peopleInRoles[4].isNotEmpty() and toggledFilters.contains("Letterer")) {
                    filtersLoaded.add(
                        LettererPeopleFilterGroup(
                            peopleInRoles[4].map { LettererPeopleFilter(it.name) }
                        )
                    )
                }
                if (peopleInRoles[5].isNotEmpty() and toggledFilters.contains("CoverArtist")) {
                    filtersLoaded.add(
                        CoverArtistPeopleFilterGroup(
                            peopleInRoles[5].map { CoverArtistPeopleFilter(it.name) }
                        )
                    )
                }
                if (peopleInRoles[6].isNotEmpty() and toggledFilters.contains("Editor")) {
                    filtersLoaded.add(
                        EditorPeopleFilterGroup(
                            peopleInRoles[6].map { EditorPeopleFilter(it.name) }
                        )
                    )
                }

                if (peopleInRoles[7].isNotEmpty() and toggledFilters.contains("Publisher")) {
                    filtersLoaded.add(
                        PublisherPeopleFilterGroup(
                            peopleInRoles[7].map { PublisherPeopleFilter(it.name) }
                        )
                    )
                }
                if (peopleInRoles[8].isNotEmpty() and toggledFilters.contains("Character")) {
                    filtersLoaded.add(
                        CharacterPeopleFilterGroup(
                            peopleInRoles[8].map { CharacterPeopleFilter(it.name) }
                        )
                    )
                }
                if (peopleInRoles[9].isNotEmpty() and toggledFilters.contains("Translator")) {
                    filtersLoaded.add(
                        TranslatorPeopleFilterGroup(
                            peopleInRoles[9].map { TranslatorPeopleFilter(it.name) }
                        )
                    )
                    filtersLoaded
                } else {
                    filtersLoaded
                }
            } else { filtersLoaded }
        } catch (e: Exception) {
            Log.e(LOG_TAG, "[FILTERS] Error while creating filter list", e)
            emptyList()
        }
        return FilterList(filters)
    }

    /**
     *
     * Finished filtering
     *
     * */
    class LoginErrorException(message: String? = null, cause: Throwable? = null) : Exception(message, cause) {
        constructor(cause: Throwable) : this(null, cause)
    }
    class OpdsurlExistsInPref(message: String? = null, cause: Throwable? = null) : Exception(message, cause) {
        constructor(cause: Throwable) : this(null, cause)
    }
    class EmptyRequestBody(message: String? = null, cause: Throwable? = null) : Exception(message, cause) {
        constructor(cause: Throwable) : this(null, cause)
    }
    class LoadingFilterFailed(message: String? = null, cause: Throwable? = null) : Exception(message, cause) {
        constructor(cause: Throwable) : this(null, cause)
    }

    override fun headersBuilder(): Headers.Builder {
        if (jwtToken.isEmpty()) throw LoginErrorException("401 Error\nOPDS address got modified or is incorrect")
        return Headers.Builder()
            .add("User-Agent", "Tachiyomi Kavita v${AppInfo.getVersionName()}")
            .add("Content-Type", "application/json")
            .add("Authorization", "Bearer $jwtToken")
    }
    private fun setupLoginHeaders(): Headers.Builder {
        return Headers.Builder()
            .add("User-Agent", "Tachiyomi Kavita v${AppInfo.getVersionName()}")
            .add("Content-Type", "application/json")
            .add("Authorization", "Bearer $jwtToken")
    }
    private fun buildFilterBody(filter: MetadataPayload = toFilter): RequestBody {
        var filter = filter
        if (!isFilterOn and !filter.forceUseMetadataPayload) {
            filter = MetadataPayload()
        }

        val formats = if (filter.formats.isEmpty()) {
            buildJsonArray {
                add(MangaFormat.Archive.ordinal)
                add(MangaFormat.Image.ordinal)
                add(MangaFormat.Pdf.ordinal)
            }
        } else {
            buildJsonArray { filter.formats.map { add(it) } }
        }

        val payload = buildJsonObject {
            put("formats", formats)
            put("libraries", buildJsonArray { filter.libraries.map { add(it) } })
            put(
                "readStatus",
                buildJsonObject {
                    if (filter.readStatus.isNotEmpty()) {
                        filter.readStatusList
                            .forEach { status -> put(status, JsonPrimitive(status in filter.readStatus)) }
                    } else {
                        put("notRead", JsonPrimitive(true))
                        put("inProgress", JsonPrimitive(true))
                        put("read", JsonPrimitive(true))
                    }
                }
            )
            put("genres", buildJsonArray { filter.genres.map { add(it) } })
            put("writers", buildJsonArray { filter.peopleWriters.map { add(it) } })
            put("penciller", buildJsonArray { filter.peoplePenciller.map { add(it) } })
            put("inker", buildJsonArray { filter.peopleInker.map { add(it) } })
            put("colorist", buildJsonArray { filter.peoplePeoplecolorist.map { add(it) } })
            put("letterer", buildJsonArray { filter.peopleLetterer.map { add(it) } })
            put("coverArtist", buildJsonArray { filter.peopleCoverArtist.map { add(it) } })
            put("editor", buildJsonArray { filter.peopleEditor.map { add(it) } })
            put("publisher", buildJsonArray { filter.peoplePublisher.map { add(it) } })
            put("character", buildJsonArray { filter.peopleCharacter.map { add(it) } })
            put("translators", buildJsonArray { filter.peopleTranslator.map { add(it) } })
            put("collectionTags", buildJsonArray { filter.collections.map { add(it) } })
            put("languages", buildJsonArray { filter.language.map { add(it) } })
            put("publicationStatus", buildJsonArray { filter.pubStatus.map { add(it) } })
            put("tags", buildJsonArray { filter.tags.map { add(it) } })
            put("rating", filter.userRating)
            put("ageRating", buildJsonArray { filter.ageRating.map { add(it) } })
            put(
                "sortOptions",
                buildJsonObject {
                    put("sortField", filter.sorting)
                    put("isAscending", JsonPrimitive(filter.sorting_asc))
                }
            )
        }
        return payload.toString().toRequestBody(JSON_MEDIA_TYPE)
    }

    override fun setupPreferenceScreen(screen: androidx.preference.PreferenceScreen) {
        val opdsAddressPref = screen.editTextPreference(
            ADDRESS_TITLE,
            "OPDS url",
            "",
            "The OPDS url copied from User Settings. This should include address and the api key on end."
        )
        val enabledFiltersPref = MultiSelectListPreference(screen.context).apply {
            key = KavitaConstants.toggledFiltersPref
            title = "Default filters shown"
            summary = "Show these filters in the filter list"
            entries = KavitaConstants.filterPrefEntries
            entryValues = KavitaConstants.filterPrefEntriesValue
            setDefaultValue(KavitaConstants.defaultFilterPrefEntries)
            setOnPreferenceChangeListener { _, newValue ->
                val checkValue = newValue as Set<String>
                preferences.edit()
                    .putStringSet(KavitaConstants.toggledFiltersPref, checkValue)
                    .commit()
            }
        }
        val customSourceNamePref = EditTextPreference(screen.context).apply {
            key = KavitaConstants.customSourceNamePref
            title = "Displayed name for source"
            summary = "Here you can change this source name.\n" +
                "You can write a descriptive name to identify this opds URL"
            setOnPreferenceChangeListener { _, newValue ->
                val res = preferences.edit()
                    .putString(KavitaConstants.customSourceNamePref, newValue.toString())
                    .commit()
                Toast.makeText(
                    screen.context,
                    "Restart Tachiyomi to apply new setting.",
                    Toast.LENGTH_LONG
                ).show()
                Log.v(LOG_TAG, "[Preferences] Successfully modified custom source name: $newValue")
                res
            }
        }
        screen.addPreference(customSourceNamePref)
        screen.addPreference(opdsAddressPref)
        screen.addPreference(enabledFiltersPref)
    }

    private fun androidx.preference.PreferenceScreen.editTextPreference(
        preKey: String,
        title: String,
        default: String,
        summary: String,
        isPassword: Boolean = false
    ): EditTextPreference {
        return EditTextPreference(context).apply {
            key = preKey
            this.title = title
            val input = preferences.getString(title, null)
            this.summary = if (input == null || input.isEmpty()) summary else input
            this.setDefaultValue(default)
            dialogTitle = title

            if (isPassword) {
                setOnBindEditTextListener {
                    it.inputType =
                        InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                }
            }
            setOnPreferenceChangeListener { _, newValue ->
                try {
                    val opdsUrlInPref = opdsUrlInPreferences(newValue.toString()) // We don't allow hot have multiple sources with same ip or domain
                    if (opdsUrlInPref.isNotEmpty()) {
                        // TODO("Add option to allow multiple sources with same url at the cost of tracking")
                        preferences.edit().putString(title, "").apply()

                        Toast.makeText(
                            context,
                            "URL exists in a different source -> $opdsUrlInPref",
                            Toast.LENGTH_LONG
                        ).show()
                        throw OpdsurlExistsInPref("Url exists in a different source -> $opdsUrlInPref")
                    }

                    val res = preferences.edit().putString(title, newValue as String).commit()
                    Toast.makeText(
                        context,
                        "Restart Tachiyomi to apply new setting.",
                        Toast.LENGTH_LONG
                    ).show()
                    setupLogin(newValue)
                    Log.v(LOG_TAG, "[Preferences] Successfully modified OPDS URL")
                    res
                } catch (e: OpdsurlExistsInPref) {
                    Log.e(LOG_TAG, "Url exists in a different sourcce")
                    false
                } catch (e: Exception) {
                    Log.e(LOG_TAG, "Unrecognised error", e)
                    false
                }
            }
        }
    }

    private fun getPrefBaseUrl(): String = preferences.getString("BASEURL", "")!!
    private fun getPrefApiUrl(): String = preferences.getString("APIURL", "")!!
    private fun getPrefKey(key: String): String = preferences.getString(key, "")!!
    private fun getToggledFilters() = preferences.getStringSet(KavitaConstants.toggledFiltersPref, KavitaConstants.defaultFilterPrefEntries)!!

    // We strip the last slash since we will append it above
    private fun getPrefAddress(): String {
        var path = preferences.getString(ADDRESS_TITLE, "")!!
        if (path.isNotEmpty() && path.last() == '/') {
            path = path.substring(0, path.length - 1)
        }
        return path
    }

    companion object {
        private const val ADDRESS_TITLE = "Address"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaTypeOrNull()
    }

    /**
     * LOGIN
     **/

    private fun opdsUrlInPreferences(url: String): String {
        fun getCleanedApiUrl(url: String): String = "${url.split("/api/").first()}/api"
        /**Used to check if a url already exists in preference in any source
         * This is a limitation needed for tracking.**/
        for (sourceId in 1..3) { // There's 3 sources so 3 preferences to check
            val sourceSuffixID by lazy {
                val key = "${"kavita_$sourceId"}/all/1" // Hardcoded versionID to 1
                val bytes = MessageDigest.getInstance("MD5").digest(key.toByteArray())
                (0..7).map { bytes[it].toLong() and 0xff shl 8 * (7 - it) }
                    .reduce(Long::or) and Long.MAX_VALUE
            }
            val preferences: SharedPreferences by lazy {
                Injekt.get<Application>().getSharedPreferences("source_$sourceSuffixID", 0x0000)
            }
            val prefApiUrl = preferences.getString("APIURL", "")!!

            if (prefApiUrl.isNotEmpty()) {
                if (prefApiUrl == getCleanedApiUrl(url)) {
                    if (sourceId.toString() != suffix) {
                        return preferences.getString(KavitaConstants.customSourceNamePref, sourceId.toString())!!
                    }
                }
            }
        }
        return ""
    }

    private fun setupLogin(addressFromPreference: String = "") {
        Log.v(LOG_TAG, "[Setup Login] Starting setup")
        val validAddress = address.ifEmpty { addressFromPreference }
        val tokens = validAddress.split("/api/opds/")
        val apiKey = tokens[1]
        val baseUrlSetup = tokens[0].replace("\n", "\\n")

        if (baseUrlSetup.toHttpUrlOrNull() == null) {
            Log.e(LOG_TAG, "Invalid URL $baseUrlSetup")
            throw Exception("""Invalid URL: $baseUrlSetup""")
        }
        preferences.edit().putString("BASEURL", baseUrlSetup).apply()
        preferences.edit().putString("APIKEY", apiKey).apply()
        preferences.edit().putString("APIURL", "$baseUrlSetup/api").apply()
        Log.v(LOG_TAG, "[Setup Login] Setup successful")
    }

    private fun doLogin() {

        if (address.isEmpty()) {
            Log.e(LOG_TAG, "OPDS URL is empty or null")
            throw IOException("You must setup the Address to communicate with Kavita")
        }
        if (address.split("/opds/").size != 2) {
            throw IOException("Address is not correct. Please copy from User settings -> OPDS Url")
        }
        if (jwtToken.isEmpty()) setupLogin()
        Log.v(LOG_TAG, "[Login] Starting login")
        val request = POST(
            "$apiUrl/Plugin/authenticate?apiKey=${getPrefKey("APIKEY")}&pluginName=Tachiyomi-Kavita",
            setupLoginHeaders().build(), "{}".toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        )
        client.newCall(request).execute().use {
            val peekbody = it.peekBody(Long.MAX_VALUE).toString()

            if (it.code == 200) {
                try {
                    jwtToken = it.parseAs<AuthenticationDto>().token
                    isLoged = true
                } catch (e: Exception) {
                    Log.e(LOG_TAG, "Possible outdated kavita", e)
                    throw IOException("Please check your kavita version.\nv0.5+ is required for the extension to work properly")
                }
            } else {
                if (it.code == 500) {
                    Log.e(LOG_TAG, "[LOGIN] login failed. There was some error -> Code: ${it.code}.Response message: ${it.message} Response body: $peekbody.")
                    throw LoginErrorException("[LOGIN] login failed. Something went wrong")
                } else {
                    Log.e(LOG_TAG, "[LOGIN] login failed. Authentication was not successful -> Code: ${it.code}.Response message: ${it.message} Response body: $peekbody.")
                    throw LoginErrorException("[LOGIN] login failed. Something went wrong")
                }
            }
        }
        Log.v(LOG_TAG, "[Login] Login successful")
    }

    init {
        if (apiUrl.isNotBlank()) {
            Single.fromCallable {
                // Login
                doLogin()
                try { // Get current version
                    val requestUrl = "$apiUrl/Server/server-info"
                    val serverInfoDto = client.newCall(GET(requestUrl, headersBuilder().build()))
                        .execute()
                        .parseAs<ServerInfoDto>()
                    Log.e(
                        LOG_TAG,
                        "Extension version: code=${AppInfo.getVersionCode()}  name=${AppInfo.getVersionName()}" +
                            " - - Kavita version: ${serverInfoDto.kavitaVersion}"
                    ) // this is not a real error. Using this so it gets printed in dump logs if there's any error
                } catch (e: EmptyRequestBody) {
                    Log.e(LOG_TAG, "Extension version: code=${AppInfo.getVersionCode()} - name=${AppInfo.getVersionName()}")
                } catch (e: Exception) {
                    Log.e(LOG_TAG, "Tachiyomi version: code=${AppInfo.getVersionCode()} - name=${AppInfo.getVersionName()}", e)
                }
                try { // Load Filters
                    // Genres
                    Log.v(LOG_TAG, "[Filter] Fetching filters ")
                    client.newCall(GET("$apiUrl/Metadata/genres", headersBuilder().build()))
                        .execute().use { response ->

                            genresListMeta = try {
                                val responseBody = response.body
                                if (responseBody != null) {
                                    responseBody.use { json.decodeFromString(it.string()) }
                                } else {
                                    Log.e(
                                        LOG_TAG,
                                        "[Filter] Error decoding JSON for genres filter: response body is null. Response code: ${response.code}"
                                    )
                                    emptyList()
                                }
                            } catch (e: Exception) {
                                Log.e(LOG_TAG, "[Filter] Error decoding JSON for genres filter -> ${response.body!!}", e)
                                emptyList()
                            }
                        }
                    // tagsListMeta
                    client.newCall(GET("$apiUrl/Metadata/tags", headersBuilder().build()))
                        .execute().use { response ->
                            tagsListMeta = try {
                                val responseBody = response.body
                                if (responseBody != null) {
                                    responseBody.use { json.decodeFromString(it.string()) }
                                } else {
                                    Log.e(
                                        LOG_TAG,
                                        "[Filter] Error decoding JSON for tagsList filter: response body is null. Response code: ${response.code}"
                                    )
                                    emptyList()
                                }
                            } catch (e: Exception) {
                                Log.e(LOG_TAG, "[Filter] Error decoding JSON for tagsList filter", e)
                                emptyList()
                            }
                        }
                    // age-ratings
                    client.newCall(GET("$apiUrl/Metadata/age-ratings", headersBuilder().build()))
                        .execute().use { response ->
                            ageRatingsListMeta = try {
                                val responseBody = response.body
                                if (responseBody != null) {
                                    responseBody.use { json.decodeFromString(it.string()) }
                                } else {
                                    Log.e(
                                        LOG_TAG,
                                        "[Filter] Error decoding JSON for age-ratings filter: response body is null. Response code: ${response.code}"
                                    )
                                    emptyList()
                                }
                            } catch (e: Exception) {
                                Log.e(
                                    LOG_TAG,
                                    "[Filter] Error decoding JSON for age-ratings filter",
                                    e
                                )
                                emptyList()
                            }
                        }
                    // collectionsListMeta
                    client.newCall(GET("$apiUrl/Collection", headersBuilder().build()))
                        .execute().use { response ->
                            collectionsListMeta = try {
                                val responseBody = response.body
                                if (responseBody != null) {
                                    responseBody.use { json.decodeFromString(it.string()) }
                                } else {
                                    Log.e(
                                        LOG_TAG,
                                        "[Filter] Error decoding JSON for collectionsListMeta filter: response body is null. Response code: ${response.code}"
                                    )
                                    emptyList()
                                }
                            } catch (e: Exception) {
                                Log.e(
                                    LOG_TAG,
                                    "[Filter] Error decoding JSON for collectionsListMeta filter",
                                    e
                                )
                                emptyList()
                            }
                        }
                    // languagesListMeta
                    client.newCall(GET("$apiUrl/Metadata/languages", headersBuilder().build()))
                        .execute().use { response ->
                            languagesListMeta = try {
                                val responseBody = response.body
                                if (responseBody != null) {
                                    responseBody.use { json.decodeFromString(it.string()) }
                                } else {
                                    Log.e(
                                        LOG_TAG,
                                        "[Filter] Error decoding JSON for languagesListMeta filter: response body is null. Response code: ${response.code}"
                                    )
                                    emptyList()
                                }
                            } catch (e: Exception) {
                                Log.e(
                                    LOG_TAG,
                                    "[Filter] Error decoding JSON for languagesListMeta filter",
                                    e
                                )
                                emptyList()
                            }
                        }
                    // libraries
                    client.newCall(GET("$apiUrl/Library", headersBuilder().build()))
                        .execute().use { response ->
                            libraryListMeta = try {
                                val responseBody = response.body
                                if (responseBody != null) {
                                    responseBody.use { json.decodeFromString(it.string()) }
                                } else {
                                    Log.e(
                                        LOG_TAG,
                                        "[Filter] Error decoding JSON for libraries filter: response body is null. Response code: ${response.code}"
                                    )
                                    emptyList()
                                }
                            } catch (e: Exception) {
                                Log.e(
                                    LOG_TAG,
                                    "[Filter] Error decoding JSON for libraries filter",
                                    e
                                )
                                emptyList()
                            }
                        }
                    // peopleListMeta
                    client.newCall(GET("$apiUrl/Metadata/people", headersBuilder().build()))
                        .execute().use { response ->
                            peopleListMeta = try {
                                val responseBody = response.body
                                if (responseBody != null) {
                                    responseBody.use { json.decodeFromString(it.string()) }
                                } else {
                                    Log.e(
                                        LOG_TAG,
                                        "error while decoding JSON for peopleListMeta filter: response body is null. Response code: ${response.code}"
                                    )
                                    emptyList()
                                }
                            } catch (e: Exception) {
                                Log.e(
                                    LOG_TAG,
                                    "error while decoding JSON for peopleListMeta filter",
                                    e
                                )
                                emptyList()
                            }
                        }
                    client.newCall(GET("$apiUrl/Metadata/publication-status", headersBuilder().build()))
                        .execute().use { response ->
                            pubStatusListMeta = try {
                                val responseBody = response.body
                                if (responseBody != null) {
                                    responseBody.use { json.decodeFromString(it.string()) }
                                } else {
                                    Log.e(
                                        LOG_TAG,
                                        "error while decoding JSON for publicationStatusListMeta filter: response body is null. Response code: ${response.code}"
                                    )
                                    emptyList()
                                }
                            } catch (e: Exception) {
                                Log.e(
                                    LOG_TAG,
                                    "error while decoding JSON for publicationStatusListMeta filter",
                                    e
                                )
                                emptyList()
                            }
                        }
                    Log.v(LOG_TAG, "[Filter] Successfully loaded metadata tags from server")
                } catch (e: Exception) {
                    throw LoadingFilterFailed("Failed Loading Filters", e.cause)
                }
            }
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(
                    {},
                    { tr ->
                        /**
                         * Avoid polluting logs with traces of exception
                         * **/
                        if (tr is EmptyRequestBody || tr is LoginErrorException) {
                            Log.e(LOG_TAG, "error while doing initial calls\n${tr.cause}")
                            return@subscribe
                        }
                        if (tr is ConnectException) { // avoid polluting logs with traces of exception
                            Log.e(LOG_TAG, "Error while doing initial calls\n${tr.cause}")
                            return@subscribe
                        }
                        Log.e(LOG_TAG, "error while doing initial calls", tr)
                    }
                )
        }
    }
}
