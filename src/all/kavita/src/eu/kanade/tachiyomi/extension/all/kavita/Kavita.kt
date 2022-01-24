package eu.kanade.tachiyomi.extension.all.kavita

import android.app.Application
import android.content.SharedPreferences
import android.text.InputType
import android.util.Log
import android.widget.Toast
import androidx.preference.EditTextPreference
import androidx.preference.MultiSelectListPreference
import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.extension.all.kavita.dto.AuthenticationDto
import eu.kanade.tachiyomi.extension.all.kavita.dto.ChapterDto
import eu.kanade.tachiyomi.extension.all.kavita.dto.KavitaComicsSearch
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
import eu.kanade.tachiyomi.extension.all.kavita.dto.SeriesDto
import eu.kanade.tachiyomi.extension.all.kavita.dto.SeriesMetadataDto
import eu.kanade.tachiyomi.extension.all.kavita.dto.VolumeDto
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
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaTypeOrNull
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
import java.security.MessageDigest

class Kavita(suffix: String = "") : ConfigurableSource, HttpSource() {

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
    private val LOG_TAG = "extension.all.kavita_${preferences.getString(KavitaConstants.customSourceNamePref,suffix)!!.replace(' ','_')}"
    private var isLoged = false // Used to know if login was correct and not send login requests anymore

    private val json: Json by injectLazy()
    private val helper = KavitaHelper()
    private inline fun <reified T> Response.parseAs(): T =
        use {
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
        return POST(
            "$apiUrl/series/recently-added?pageNumber=$page&libraryId=0&pageSize=20",
            headersBuilder().build(),
            buildFilterBody()
        )
    }

    override fun latestUpdatesParse(response: Response): MangasPage {
        val result = response.parseAs<List<SeriesDto>>()
        series = result
        val mangaList = result.map { item -> helper.createSeriesDto(item, apiUrl) }
        return MangasPage(mangaList, helper.hasNextPage(response))
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
                        // disabled till the search api is stable
                        // isFilterOn = true
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
            }
        }

        if (isFilterOn || query.isEmpty()) {
            return popularMangaRequest(page)
        } else {
            return GET("$apiUrl/Library/search?queryString=$query", headers)
        }
    }

    override fun searchMangaParse(response: Response): MangasPage {
        if (isFilterOn) {
            return popularMangaParse(response)
        } else {
            if (response.request.url.toString().contains("api/series/all"))
                return popularMangaParse(response)

            val result = response.parseAs<List<KavitaComicsSearch>>()
            val mangaList = result.map(::searchMangaFromObject)
            return MangasPage(mangaList, false)
        }
    }

    private fun searchMangaFromObject(obj: KavitaComicsSearch): SManga = SManga.create().apply {
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
                Log.d(LOG_TAG, "fetchMangaDetails response body: ```${response.peekBody(Long.MAX_VALUE).string()}```")
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
            manga.artist = result.coverArtists.joinToString { it.name }
            manga.description = result.summary
            manga.author = result.writers.joinToString { it.name }
            manga.genre = result.genres.joinToString { it.title }

            return manga
        }

        return SManga.create().apply {
            url = "$apiUrl/Series/${result.seriesId}"
            artist = result.coverArtists.joinToString { ", " }
            author = result.writers.joinToString { ", " }
            genre = result.genres.joinToString { ", " }
            thumbnail_url = "$apiUrl/image/series-cover?seriesId=${result.seriesId}"
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
            name = obj.range
        } else {
            val cleanedName = obj.title.replaceFirst("^0+(?!$)".toRegex(), "")
            name = "Chapter $cleanedName"
        }
        date_upload = helper.parseDate(obj.created)
        chapter_number = obj.number.toFloat()
        scanlator = obj.pages.toString()
    }

    private fun chapterFromVolume(obj: ChapterDto, volume: VolumeDto): SChapter =
        SChapter.create().apply {
            // If there are multiple chapters to this volume, then prefix with Volume number
            if (volume.chapters.isNotEmpty() && obj.number != "0") {
                name = "Volume ${volume.number} Chapter ${obj.number}"
            } else if (obj.number == "0") {
                // This chapter is solely on volume
                if (volume.number == 0) {
                    // Treat as special
                    if (obj.range == "") {
                        name = "Chapter 0"
                    } else {
                        name = obj.range
                    }
                } else {
                    name = "Volume ${volume.number}"
                }
            } else {
                name = "Unhandled Else Volume ${volume.number}"
            }
            url = obj.id.toString()
            date_upload = helper.parseDate(obj.created)
            chapter_number = obj.number.toFloat()
            scanlator = "${obj.pages}"
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
            allChapterList.reverse()
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
        val numPages = chapter.scanlator?.toInt()
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
    override fun headersBuilder(): Headers.Builder {
        if (jwtToken.isEmpty()) throw LoginErrorException("403 Error\nOPDS address got modified or is incorrect")
        return Headers.Builder()
            .add("User-Agent", "Tachiyomi Kavita v${BuildConfig.VERSION_NAME}")
            .add("Content-Type", "application/json")
            .add("Authorization", "Bearer $jwtToken")
    }
    private fun setupLoginHeaders(): Headers.Builder {
        return Headers.Builder()
            .add("User-Agent", "Tachiyomi Kavita v${BuildConfig.VERSION_NAME}")
            .add("Content-Type", "application/json")
            .add("Authorization", "Bearer $jwtToken")
    }
    private fun buildFilterBody(filter: MetadataPayload = toFilter): RequestBody {
        var filter = filter
        if (!isFilterOn) {
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
                        filter.readStatus.forEach { status ->
                            if (status in listOf("notRead", "inProgress", "read")) {
                                put(status, JsonPrimitive(true))
                            } else {
                                put(status, JsonPrimitive(false))
                            }
                        }
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
                    val res = preferences.edit().putString(title, newValue as String).commit()
                    Toast.makeText(
                        context,
                        "Restart Tachiyomi to apply new setting.",
                        Toast.LENGTH_LONG
                    ).show()
                    setupLogin(newValue)
                    Log.v(LOG_TAG, "[Preferences] Successfully modified OPDS URL")
                    res
                } catch (e: Exception) {
                    e.printStackTrace()
                    false
                }
            }
        }
    }

    // private fun getPrefapiKey(): String = preferences.getString("APIKEY", "")!!
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
    private fun setupLogin(addressFromPreference: String = "") {
        Log.v(LOG_TAG, "[Setup Login] Starting setup")
        val validaddress = if (address.isEmpty()) addressFromPreference else address
        val tokens = validaddress.split("/api/opds/")
        val apiKey = tokens[1]
        val baseUrlSetup = tokens[0].replace("\n", "\\n")

        if (!baseUrlSetup.startsWith("http")) {
            try {
                throw Exception("""Url does not start with "http/s" but with ${baseUrlSetup.split("://")[0]} """)
            } catch (e: Exception) {
                throw Exception("""Malformed Url: $baseUrlSetup""")
            }
        }
        preferences.edit().putString("BASEURL", baseUrlSetup).commit()
        preferences.edit().putString("APIKEY", apiKey).commit()
        preferences.edit().putString("APIURL", "$baseUrlSetup/api").commit()
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
            if (it.code == 200) {
                try {
                    jwtToken = it.parseAs<AuthenticationDto>().token
                    isLoged = true
                } catch (e: Exception) {
                    Log.e(LOG_TAG, "Possible outdated kavita", e)
                    throw IOException("Please check your kavita version.\nv0.5+ is required for the extension to work properly")
                }
            } else {
                Log.e(LOG_TAG, "[LOGIN] login failed. Authentication was not successful -> Code: ${it.code}.Response message: ${it.message} Response body: ${it.body!!}.")
                throw LoginErrorException("[LOGIN] login failed. Authentication was not successful")
            }
        }
        Log.v(LOG_TAG, "[Login] Login successful")
    }

    init {
        if (apiUrl.isNotBlank()) {
            Single.fromCallable {
                // Login
                var loginSuccesful = false
                try {
                    doLogin()
                    loginSuccesful = true
                } catch (e: LoginErrorException) {
                    Log.e(LOG_TAG, "Init login failed: $e")
                }
                if (loginSuccesful) { // doing this check to not clutter LOGS
                    // Genres
                    Log.v(LOG_TAG, "[Filter] Fetching filters ")
                    try {
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
                    } catch (e: Exception) {
                        Log.e(LOG_TAG, "[Filter] Error loading genres for filters", e)
                    }
                    // tagsListMeta
                    try {
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
                    } catch (e: Exception) {
                        Log.e(LOG_TAG, "[Filter] Error loading tagsList for filters", e)
                    }
                    // age-ratings
                    try {
                        client.newCall(
                            GET(
                                "$apiUrl/Metadata/age-ratings",
                                headersBuilder().build()
                            )
                        ).execute().use { response ->
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
                    } catch (e: Exception) {
                        Log.e(LOG_TAG, "[Filter] Error loading age-ratings for age-ratings", e)
                    }
                    // collectionsListMeta
                    try {
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
                    } catch (e: Exception) {
                        Log.e(LOG_TAG, "[Filter] Error loading collectionsListMeta for collectionsListMeta", e)
                    }
                    // languagesListMeta
                    try {
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
                    } catch (e: Exception) {
                        Log.e(LOG_TAG, "[Filter] Error loading languagesListMeta for languagesListMeta", e)
                    }
                    // libraries
                    try {
                        client.newCall(GET("$apiUrl/Library", headersBuilder().build())).execute()
                            .use { response ->
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
                    } catch (e: Exception) {
                        Log.e(LOG_TAG, "[Filter] Error loading libraries for languagesListMeta", e)
                    }
                    // peopleListMeta
                    try {
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
                    } catch (e: Exception) {
                        Log.e(LOG_TAG, "[Filter] Error loading tagsList for peopleListMeta", e)
                    }
                    try {
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
                    } catch (e: Exception) {
                        Log.e(LOG_TAG, "[Filter] Error loading tagsList for peopleListMeta", e)
                    }

                    Log.v(LOG_TAG, "[Filter] Successfully loaded metadata tags from server")
                }
                Log.v(LOG_TAG, "Successfully ended init")
            }
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(
                    {},
                    { tr ->
                        Log.e(LOG_TAG, "error while doing initial calls", tr)
                    }
                )
        }
    }
}
