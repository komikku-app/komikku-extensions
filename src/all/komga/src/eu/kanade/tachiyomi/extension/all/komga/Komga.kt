package eu.kanade.tachiyomi.extension.all.komga

import android.app.Application
import android.content.SharedPreferences
import android.text.InputType
import android.util.Log
import android.widget.Toast
import eu.kanade.tachiyomi.extension.BuildConfig
import eu.kanade.tachiyomi.extension.all.komga.dto.AuthorDto
import eu.kanade.tachiyomi.extension.all.komga.dto.BookDto
import eu.kanade.tachiyomi.extension.all.komga.dto.CollectionDto
import eu.kanade.tachiyomi.extension.all.komga.dto.LibraryDto
import eu.kanade.tachiyomi.extension.all.komga.dto.PageDto
import eu.kanade.tachiyomi.extension.all.komga.dto.PageWrapperDto
import eu.kanade.tachiyomi.extension.all.komga.dto.ReadListDto
import eu.kanade.tachiyomi.extension.all.komga.dto.SeriesDto
import eu.kanade.tachiyomi.network.GET
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
import okhttp3.Credentials
import okhttp3.Dns
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import rx.Observable
import rx.Single
import rx.schedulers.Schedulers
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

open class Komga(suffix: String = "") : ConfigurableSource, HttpSource() {
    override fun popularMangaRequest(page: Int): Request =
        GET("$baseUrl/api/v1/series?page=${page - 1}&deleted=false", headers)

    override fun popularMangaParse(response: Response): MangasPage =
        processSeriesPage(response)

    override fun latestUpdatesRequest(page: Int): Request =
        GET("$baseUrl/api/v1/series/latest?page=${page - 1}&deleted=false", headers)

    override fun latestUpdatesParse(response: Response): MangasPage =
        processSeriesPage(response)

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        val collectionId = (filters.find { it is CollectionSelect } as? CollectionSelect)?.let {
            it.values[it.state].id
        }

        val type = when {
            collectionId != null -> "collections/$collectionId/series"
            filters.find { it is TypeSelect }?.state == 1 -> "readlists"
            else -> "series"
        }

        val url = "$baseUrl/api/v1/$type?search=$query&page=${page - 1}&deleted=false".toHttpUrlOrNull()!!.newBuilder()

        filters.forEach { filter ->
            when (filter) {
                is UnreadFilter -> {
                    if (filter.state) {
                        url.addQueryParameter("read_status", "UNREAD")
                        url.addQueryParameter("read_status", "IN_PROGRESS")
                    }
                }
                is InProgressFilter -> {
                    if (filter.state) {
                        url.addQueryParameter("read_status", "IN_PROGRESS")
                    }
                }
                is ReadFilter -> {
                    if (filter.state) {
                        url.addQueryParameter("read_status", "READ")
                    }
                }
                is LibraryGroup -> {
                    val libraryToInclude = mutableListOf<String>()
                    filter.state.forEach { content ->
                        if (content.state) {
                            libraryToInclude.add(content.id)
                        }
                    }
                    if (libraryToInclude.isNotEmpty()) {
                        url.addQueryParameter("library_id", libraryToInclude.joinToString(","))
                    }
                }
                is StatusGroup -> {
                    val statusToInclude = mutableListOf<String>()
                    filter.state.forEach { content ->
                        if (content.state) {
                            statusToInclude.add(content.name.toUpperCase(Locale.ROOT))
                        }
                    }
                    if (statusToInclude.isNotEmpty()) {
                        url.addQueryParameter("status", statusToInclude.joinToString(","))
                    }
                }
                is GenreGroup -> {
                    val genreToInclude = mutableListOf<String>()
                    filter.state.forEach { content ->
                        if (content.state) {
                            genreToInclude.add(content.name)
                        }
                    }
                    if (genreToInclude.isNotEmpty()) {
                        url.addQueryParameter("genre", genreToInclude.joinToString(","))
                    }
                }
                is TagGroup -> {
                    val tagToInclude = mutableListOf<String>()
                    filter.state.forEach { content ->
                        if (content.state) {
                            tagToInclude.add(content.name)
                        }
                    }
                    if (tagToInclude.isNotEmpty()) {
                        url.addQueryParameter("tag", tagToInclude.joinToString(","))
                    }
                }
                is PublisherGroup -> {
                    val publisherToInclude = mutableListOf<String>()
                    filter.state.forEach { content ->
                        if (content.state) {
                            publisherToInclude.add(content.name)
                        }
                    }
                    if (publisherToInclude.isNotEmpty()) {
                        url.addQueryParameter("publisher", publisherToInclude.joinToString(","))
                    }
                }
                is AuthorGroup -> {
                    val authorToInclude = mutableListOf<AuthorDto>()
                    filter.state.forEach { content ->
                        if (content.state) {
                            authorToInclude.add(content.author)
                        }
                    }
                    authorToInclude.forEach {
                        url.addQueryParameter("author", "${it.name},${it.role}")
                    }
                }
                is Filter.Sort -> {
                    var sortCriteria = when (filter.state?.index) {
                        0 -> "metadata.titleSort"
                        1 -> "createdDate"
                        2 -> "lastModifiedDate"
                        else -> ""
                    }
                    if (sortCriteria.isNotEmpty()) {
                        sortCriteria += "," + if (filter.state?.ascending!!) "asc" else "desc"
                        url.addQueryParameter("sort", sortCriteria)
                    }
                }
            }
        }

        return GET(url.toString(), headers)
    }

    override fun searchMangaParse(response: Response): MangasPage =
        processSeriesPage(response)

    override fun fetchMangaDetails(manga: SManga): Observable<SManga> {
        return client.newCall(GET(manga.url, headers))
            .asObservableSuccess()
            .map { response ->
                mangaDetailsParse(response).apply { initialized = true }
            }
    }

    override fun mangaDetailsRequest(manga: SManga): Request =
        GET(manga.url.replaceFirst("api/v1/", "", ignoreCase = true), headers)

    override fun mangaDetailsParse(response: Response): SManga {
        val responseBody = response.body
            ?: throw IllegalStateException("Response code ${response.code}")

        return responseBody.use { body ->
            if (response.fromReadList()) {
                val readList = json.decodeFromString<ReadListDto>(body.string())
                readList.toSManga()
            } else {
                val series = json.decodeFromString<SeriesDto>(body.string())
                series.toSManga()
            }
        }
    }

    override fun chapterListRequest(manga: SManga): Request =
        GET("${manga.url}/books?unpaged=true&media_status=READY&deleted=false", headers)

    override fun chapterListParse(response: Response): List<SChapter> {
        val responseBody = response.body
            ?: throw IllegalStateException("Response code ${response.code}")

        val page = responseBody.use { json.decodeFromString<PageWrapperDto<BookDto>>(it.string()).content }

        val r = page.mapIndexed { index, book ->
            SChapter.create().apply {
                chapter_number = if (!response.fromReadList()) book.metadata.numberSort else index + 1F
                name = "${if (!response.fromReadList()) "${book.metadata.number} - " else ""}${book.metadata.title} (${book.size})"
                url = "$baseUrl/api/v1/books/${book.id}"
                scanlator = book.metadata.authors.groupBy({ it.role }, { it.name })["translator"]?.joinToString()
                date_upload = book.metadata.releaseDate?.let { parseDate(it) }
                    ?: parseDateTime(book.fileLastModified)
            }
        }
        return r.sortedByDescending { it.chapter_number }
    }

    override fun pageListRequest(chapter: SChapter): Request =
        GET("${chapter.url}/pages")

    override fun pageListParse(response: Response): List<Page> {
        val responseBody = response.body
            ?: throw IllegalStateException("Response code ${response.code}")

        val pages = responseBody.use { json.decodeFromString<List<PageDto>>(it.string()) }
        return pages.map {
            val url = "${response.request.url}/${it.number}" +
                if (!supportedImageTypes.contains(it.mediaType)) {
                    "?convert=png"
                } else {
                    ""
                }
            Page(
                index = it.number - 1,
                imageUrl = url
            )
        }
    }

    private fun processSeriesPage(response: Response): MangasPage {
        val responseBody = response.body
            ?: throw IllegalStateException("Response code ${response.code}")

        return responseBody.use { body ->
            if (response.fromReadList()) {
                with(json.decodeFromString<PageWrapperDto<ReadListDto>>(body.string())) {
                    MangasPage(content.map { it.toSManga() }, !last)
                }
            } else {
                with(json.decodeFromString<PageWrapperDto<SeriesDto>>(body.string())) {
                    MangasPage(content.map { it.toSManga() }, !last)
                }
            }
        }
    }

    private fun SeriesDto.toSManga(): SManga =
        SManga.create().apply {
            title = metadata.title
            url = "$baseUrl/api/v1/series/$id"
            thumbnail_url = "$url/thumbnail"
            status = when (metadata.status) {
                "ONGOING" -> SManga.ONGOING
                "ENDED" -> SManga.COMPLETED
                else -> SManga.UNKNOWN
            }
            genre = (metadata.genres + metadata.tags + booksMetadata.tags).distinct().joinToString(", ")
            description = metadata.summary.ifBlank { booksMetadata.summary }
            booksMetadata.authors.groupBy { it.role }.let { map ->
                author = map["writer"]?.map { it.name }?.distinct()?.joinToString()
                artist = map["penciller"]?.map { it.name }?.distinct()?.joinToString()
            }
        }

    private fun ReadListDto.toSManga(): SManga =
        SManga.create().apply {
            title = name
            description = summary
            url = "$baseUrl/api/v1/readlists/$id"
            thumbnail_url = "$url/thumbnail"
            status = SManga.UNKNOWN
        }

    private fun Response.fromReadList() = request.url.toString().contains("/api/v1/readlists")

    private fun parseDate(date: String?): Long =
        if (date == null)
            Date().time
        else {
            try {
                SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(date).time
            } catch (ex: Exception) {
                Date().time
            }
        }

    private fun parseDateTime(date: String?): Long =
        if (date == null)
            Date().time
        else {
            try {
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).parse(date).time
            } catch (ex: Exception) {
                try {
                    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.S", Locale.US).parse(date).time
                } catch (ex: Exception) {
                    Date().time
                }
            }
        }

    override fun imageUrlParse(response: Response): String = ""

    private class TypeSelect : Filter.Select<String>("Search for", arrayOf(TYPE_SERIES, TYPE_READLISTS))
    private class LibraryFilter(val id: String, name: String) : Filter.CheckBox(name, false)
    private class LibraryGroup(libraries: List<LibraryFilter>) : Filter.Group<LibraryFilter>("Libraries", libraries)
    private class CollectionSelect(collections: List<CollectionFilterEntry>) : Filter.Select<CollectionFilterEntry>("Collection", collections.toTypedArray())
    private class SeriesSort : Filter.Sort("Sort", arrayOf("Alphabetically", "Date added", "Date updated"), Selection(0, true))
    private class StatusFilter(name: String) : Filter.CheckBox(name, false)
    private class StatusGroup(filters: List<StatusFilter>) : Filter.Group<StatusFilter>("Status", filters)
    private class UnreadFilter : Filter.CheckBox("Unread", false)
    private class InProgressFilter : Filter.CheckBox("In Progress", false)
    private class ReadFilter : Filter.CheckBox("Read", false)
    private class GenreFilter(genre: String) : Filter.CheckBox(genre, false)
    private class GenreGroup(genres: List<GenreFilter>) : Filter.Group<GenreFilter>("Genres", genres)
    private class TagFilter(tag: String) : Filter.CheckBox(tag, false)
    private class TagGroup(tags: List<TagFilter>) : Filter.Group<TagFilter>("Tags", tags)
    private class PublisherFilter(publisher: String) : Filter.CheckBox(publisher, false)
    private class PublisherGroup(publishers: List<PublisherFilter>) : Filter.Group<PublisherFilter>("Publishers", publishers)
    private class AuthorFilter(val author: AuthorDto) : Filter.CheckBox(author.name, false)
    private class AuthorGroup(role: String, authors: List<AuthorFilter>) : Filter.Group<AuthorFilter>(role, authors)

    private data class CollectionFilterEntry(
        val name: String,
        val id: String? = null
    ) {
        override fun toString() = name
    }

    override fun getFilterList(): FilterList {
        val filters = try {
            mutableListOf<Filter<*>>(
                UnreadFilter(),
                InProgressFilter(),
                ReadFilter(),
                TypeSelect(),
                CollectionSelect(listOf(CollectionFilterEntry("None")) + collections.map { CollectionFilterEntry(it.name, it.id) }),
                LibraryGroup(libraries.map { LibraryFilter(it.id, it.name) }.sortedBy { it.name.toLowerCase(Locale.ROOT) }),
                StatusGroup(listOf("Ongoing", "Ended", "Abandoned", "Hiatus").map { StatusFilter(it) }),
                GenreGroup(genres.map { GenreFilter(it) }),
                TagGroup(tags.map { TagFilter(it) }),
                PublisherGroup(publishers.map { PublisherFilter(it) })
            ).also { list ->
                list.addAll(authors.map { (role, authors) -> AuthorGroup(role, authors.map { AuthorFilter(it) }) })
                list.add(SeriesSort())
            }
        } catch (e: Exception) {
            Log.e(LOG_TAG, "error while creating filter list", e)
            emptyList()
        }

        return FilterList(filters)
    }

    private var libraries = emptyList<LibraryDto>()
    private var collections = emptyList<CollectionDto>()
    private var genres = emptySet<String>()
    private var tags = emptySet<String>()
    private var publishers = emptySet<String>()
    private var authors = emptyMap<String, List<AuthorDto>>() // roles to list of authors

    override val name = "Komga${if (suffix.isNotBlank()) " ($suffix)" else ""}"
    override val lang = "all"
    override val supportsLatest = true
    private val LOG_TAG = "extension.all.komga${if (suffix.isNotBlank()) ".$suffix" else ""}"

    // keep the previous ID when lang was "en", so that preferences and manga bindings are not lost
    override val id by lazy {
        val key = "${name.toLowerCase()}/en/$versionId"
        val bytes = MessageDigest.getInstance("MD5").digest(key.toByteArray())
        (0..7).map { bytes[it].toLong() and 0xff shl 8 * (7 - it) }.reduce(Long::or) and Long.MAX_VALUE
    }

    override val baseUrl by lazy { getPrefBaseUrl() }
    private val username by lazy { getPrefUsername() }
    private val password by lazy { getPrefPassword() }
    private val json: Json by injectLazy()

    override fun headersBuilder(): Headers.Builder =
        Headers.Builder()
            .add("User-Agent", "TachiyomiKomga/${BuildConfig.VERSION_NAME}")

    private val preferences: SharedPreferences by lazy {
        Injekt.get<Application>().getSharedPreferences("source_$id", 0x0000)
    }

    override val client: OkHttpClient =
        network.client.newBuilder()
            .authenticator { _, response ->
                if (response.request.header("Authorization") != null) {
                    null // Give up, we've already failed to authenticate.
                } else {
                    response.request.newBuilder()
                        .addHeader("Authorization", Credentials.basic(username, password))
                        .build()
                }
            }
            .dns(Dns.SYSTEM) // don't use DNS over HTTPS as it breaks IP addressing
            .build()

    override fun setupPreferenceScreen(screen: androidx.preference.PreferenceScreen) {
        screen.addPreference(screen.editTextPreference(ADDRESS_TITLE, ADDRESS_DEFAULT, baseUrl))
        screen.addPreference(screen.editTextPreference(USERNAME_TITLE, USERNAME_DEFAULT, username))
        screen.addPreference(screen.editTextPreference(PASSWORD_TITLE, PASSWORD_DEFAULT, password, true))
    }

    private fun androidx.preference.PreferenceScreen.editTextPreference(title: String, default: String, value: String, isPassword: Boolean = false): androidx.preference.EditTextPreference {
        return androidx.preference.EditTextPreference(context).apply {
            key = title
            this.title = title
            summary = value
            this.setDefaultValue(default)
            dialogTitle = title

            if (isPassword) {
                setOnBindEditTextListener {
                    it.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                }
            }

            setOnPreferenceChangeListener { _, newValue ->
                try {
                    val res = preferences.edit().putString(title, newValue as String).commit()
                    Toast.makeText(context, "Restart Tachiyomi to apply new setting.", Toast.LENGTH_LONG).show()
                    res
                } catch (e: Exception) {
                    e.printStackTrace()
                    false
                }
            }
        }
    }

    private fun getPrefBaseUrl(): String = preferences.getString(ADDRESS_TITLE, ADDRESS_DEFAULT)!!
    private fun getPrefUsername(): String = preferences.getString(USERNAME_TITLE, USERNAME_DEFAULT)!!
    private fun getPrefPassword(): String = preferences.getString(PASSWORD_TITLE, PASSWORD_DEFAULT)!!

    init {
        if (baseUrl.isNotBlank()) {
            Single.fromCallable {
                try {
                    client.newCall(GET("$baseUrl/api/v1/libraries", headers)).execute().use { response ->
                        libraries = try {
                            val responseBody = response.body
                            if (responseBody != null) {
                                responseBody.use { json.decodeFromString(it.string()) }
                            } else {
                                Log.e(LOG_TAG, "error while decoding JSON for libraries filter: response body is null. Response code: ${response.code}")
                                emptyList()
                            }
                        } catch (e: Exception) {
                            Log.e(LOG_TAG, "error while decoding JSON for libraries filter", e)
                            emptyList()
                        }
                    }
                } catch (e: Exception) {
                    Log.e(LOG_TAG, "error while loading libraries for filters", e)
                }

                try {
                    client.newCall(GET("$baseUrl/api/v1/collections?unpaged=true", headers)).execute().use { response ->
                        collections = try {
                            val responseBody = response.body
                            if (responseBody != null) {
                                responseBody.use { json.decodeFromString<PageWrapperDto<CollectionDto>>(it.string()).content }
                            } else {
                                Log.e(LOG_TAG, "error while decoding JSON for collections filter: response body is null. Response code: ${response.code}")
                                emptyList()
                            }
                        } catch (e: Exception) {
                            Log.e(LOG_TAG, "error while decoding JSON for collections filter", e)
                            emptyList()
                        }
                    }
                } catch (e: Exception) {
                    Log.e(LOG_TAG, "error while loading collections for filters", e)
                }

                try {
                    client.newCall(GET("$baseUrl/api/v1/genres", headers)).execute().use { response ->
                        genres = try {
                            val responseBody = response.body
                            if (responseBody != null) {
                                responseBody.use { json.decodeFromString(it.string()) }
                            } else {
                                Log.e(LOG_TAG, "error while decoding JSON for genres filter: response body is null. Response code: ${response.code}")
                                emptySet()
                            }
                        } catch (e: Exception) {
                            Log.e(LOG_TAG, "error while decoding JSON for genres filter", e)
                            emptySet()
                        }
                    }
                } catch (e: Exception) {
                    Log.e(LOG_TAG, "error while loading genres for filters", e)
                }

                try {
                    client.newCall(GET("$baseUrl/api/v1/tags", headers)).execute().use { response ->
                        tags = try {
                            val responseBody = response.body
                            if (responseBody != null) {
                                responseBody.use { json.decodeFromString(it.string()) }
                            } else {
                                Log.e(LOG_TAG, "error while decoding JSON for tags filter: response body is null. Response code: ${response.code}")
                                emptySet()
                            }
                        } catch (e: Exception) {
                            Log.e(LOG_TAG, "error while decoding JSON for tags filter", e)
                            emptySet()
                        }
                    }
                } catch (e: Exception) {
                    Log.e(LOG_TAG, "error while loading tags for filters", e)
                }

                try {
                    client.newCall(GET("$baseUrl/api/v1/publishers", headers)).execute().use { response ->
                        publishers = try {
                            val responseBody = response.body
                            if (responseBody != null) {
                                responseBody.use { json.decodeFromString(it.string()) }
                            } else {
                                Log.e(LOG_TAG, "error while decoding JSON for publishers filter: response body is null. Response code: ${response.code}")
                                emptySet()
                            }
                        } catch (e: Exception) {
                            Log.e(LOG_TAG, "error while decoding JSON for publishers filter", e)
                            emptySet()
                        }
                    }
                } catch (e: Exception) {
                    Log.e(LOG_TAG, "error while loading publishers for filters", e)
                }

                try {
                    client.newCall(GET("$baseUrl/api/v1/authors", headers)).execute().use { response ->
                        authors = try {
                            val responseBody = response.body
                            if (responseBody != null) {
                                val list: List<AuthorDto> = responseBody.use { json.decodeFromString(it.string()) }
                                list.groupBy { it.role }
                            } else {
                                Log.e(LOG_TAG, "error while decoding JSON for authors filter: response body is null. Response code: ${response.code}")
                                emptyMap()
                            }
                        } catch (e: Exception) {
                            Log.e(LOG_TAG, "error while decoding JSON for authors filter", e)
                            emptyMap()
                        }
                    }
                } catch (e: Exception) {
                    Log.e(LOG_TAG, "error while loading authors for filters", e)
                }
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

    companion object {
        private const val ADDRESS_TITLE = "Address"
        private const val ADDRESS_DEFAULT = ""
        private const val USERNAME_TITLE = "Username"
        private const val USERNAME_DEFAULT = ""
        private const val PASSWORD_TITLE = "Password"
        private const val PASSWORD_DEFAULT = ""

        private val supportedImageTypes = listOf("image/jpeg", "image/png", "image/gif", "image/webp")

        private const val TYPE_SERIES = "Series"
        private const val TYPE_READLISTS = "Read lists"
    }
}
