package eu.kanade.tachiyomi.extension.all.projectsuki

import android.icu.text.BreakIterator
import android.icu.text.Collator
import android.icu.text.RuleBasedCollator
import android.icu.text.StringSearch
import android.os.Build
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SManga
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.text.StringCharacterIterator

/**
 *  @see EXTENSION_INFO Found in ProjectSuki.kt
 */
@Suppress("unused")
private inline val INFO: Nothing get() = error("INFO")

internal val callpageUrl = homepageUrl.newBuilder().addPathSegment("callpage").build()
internal val apiBookSearchUrl = homepageUrl.newBuilder()
    .addPathSegment("api")
    .addPathSegment("book")
    .addPathSegment("search")
    .build()

/**
 * Json [MIME/media type](https://developer.mozilla.org/en-US/docs/Web/HTTP/Basics_of_HTTP/MIME_types)
 */
internal val jsonMediaType = "application/json;charset=UTF-8".toMediaType()

private val imageExtensions = setOf(".jpg", ".png", ".jpeg", ".webp", ".gif", ".avif", ".tiff")
private val simpleSrcVariants = listOf("src", "data-src", "data-lazy-src")

/**
 * Function that tries to extract the image URL from some known ways to store that information.
 */
fun Element.imageSrc(): HttpUrl? {
    simpleSrcVariants.forEach { variant ->
        if (hasAttr(variant)) {
            return attr("abs:$variant").toHttpUrlOrNull()
        }
    }

    if (hasAttr("srcset")) {
        return attr("abs:srcset").substringBefore(" ").toHttpUrlOrNull()
    }

    return attributes().firstOrNull {
        it.key.contains("src") && imageExtensions.any { ext -> it.value.contains(ext) }
    }?.value?.substringBefore(" ")?.toHttpUrlOrNull()
}

internal typealias BookTitle = String

/**
 * Singleton responsible for handling API communications with Project Suki's server.
 *
 * @author Federico d'Alonzo &lt;me@npgx.dev&gt;
 */
object ProjectSukiAPI {

    private inline fun <reified T : Any> Any.tryAs(): T? = this as? T

    /**
     * Represents the data that needs to be send to [callpageUrl] to obtain the pages of a chapter.
     *
     * @param first Actually represents a boolean, needs to be string, otherwise server is unhappy, seems mostly ignored
     */
    @Serializable
    data class PagesRequestData(
        @SerialName("bookid")
        val bookID: BookID,
        @SerialName("chapterid")
        val chapterID: ChapterID,
        @SerialName("first")
        val first: String,
    ) {
        init {
            if (first != "true" && first != "false") {
                reportErrorToUser {
                    "PagesRequestData, first was \"$first\""
                }
            }
        }
    }

    /**
     * Creates a [Request] for the server to send the chapter's pages.
     */
    fun chapterPagesRequest(json: Json, headers: Headers, bookID: BookID, chapterID: ChapterID): Request {
        val newHeaders: Headers = headers.newBuilder()
            .add("X-Requested-With", "XMLHttpRequest")
            .add("Content-Type", "application/json;charset=UTF-8")
            .build()

        val body: RequestBody = json.encodeToString(PagesRequestData(bookID, chapterID, "true"))
            .toRequestBody(jsonMediaType)

        return POST(callpageUrl.toUri().toASCIIString(), newHeaders, body)
    }

    /**
     * Handles the [Response] returned from [chapterPagesRequest]'s [call][okhttp3.OkHttpClient.newCall].
     */
    fun parseChapterPagesResponse(json: Json, response: Response): List<Page> {
        // response is a json object containing 2 elements
        // chapter_id: seems to be the same as chapterid, I'm not really sure what it's supposed to be
        // src: source html that will be appended in the page
        // we're interested in src, which will be a simple string containing a list of very verbose <img> tags
        // most important of which is the src tag, which should actually be an absolute url,
        // but we can handle both cases at once anyways.
        // urls are of the form /images/gallery/<bookid>/<uuid>/<page>
        // the uuid is a 128-bit number in base 16 (hex), most likely there to manage edits and versions.

        // page 001 is included in the response
        val rawSrc: String = json.runCatching { parseToJsonElement(response.body.string()) }
            .getOrNull()
            ?.tryAs<JsonObject>()
            ?.get("src")
            ?.tryAs<JsonPrimitive>()
            ?.content ?: reportErrorToUser {
            "chapter pages aren't in the expected format!"
        }

        // we can handle relative urls by specifying manually the location of the "document"
        val srcFragment: Element = Jsoup.parseBodyFragment(rawSrc, homepageUri.toASCIIString())

        val urls: Map<HttpUrl, PathMatchResult> = srcFragment.allElements
            .asSequence()
            .mapNotNull { it.imageSrc() }
            .associateWith { it.matchAgainst(pageUrlPattern) } // create match result
            .filterValues { it.doesMatch } // make sure they are the urls we expect

        if (urls.isEmpty()) {
            reportErrorToUser {
                "chapter pages URLs aren't in the expected format!"
            }
        }

        return urls.entries
            .sortedBy { (_, match) -> match["pagenum"]!!.value.toUInt() } // they should already be sorted, but you're never too sure
            .mapIndexed { index, (url, _) ->
                Page(
                    index = index,
                    url = "", // skip fetchImageUrl
                    imageUrl = url.toUri().toASCIIString(),
                )
            }
    }

    /** Represents the data that needs to be send to [apiBookSearchUrl] to obtain the complete list of books that have chapters. */
    @Serializable
    data class SearchRequestData(
        @SerialName("hash")
        val hash: String?,
    )

    /**
     * Creates a [Request] for the server to send the books.
     */
    fun bookSearchRequest(json: Json, headers: Headers): Request {
        val newHeaders: Headers = headers.newBuilder()
            .add("X-Requested-With", "XMLHttpRequest")
            .add("Content-Type", "application/json;charset=UTF-8")
            .add("Referer", homepageUrl.newBuilder().addPathSegment("browse").build().toUri().toASCIIString())
            .build()

        val body: RequestBody = json.encodeToString(SearchRequestData(null))
            .toRequestBody(jsonMediaType)

        return POST(apiBookSearchUrl.toUri().toASCIIString(), newHeaders, body)
    }

    /**
     * Handles the [Response] returned from [parseBookSearchResponse]'s [call][okhttp3.OkHttpClient.newCall].
     */
    fun parseBookSearchResponse(json: Json, response: Response): Map<BookID, BookTitle> {
        val data: JsonObject = json.runCatching { parseToJsonElement(response.body.string()) }
            .getOrNull()
            ?.tryAs<JsonObject>()
            ?.get("data")
            ?.tryAs<JsonObject>() ?: reportErrorToUser {
            "books data isn't in the expected format!"
        }

        val refined: Map<BookID, BookTitle> = buildMap {
            data.forEach { (id: BookID, valueObj: JsonElement) ->
                val title: BookTitle = valueObj.tryAs<JsonObject>()
                    ?.get("value")
                    ?.tryAs<JsonPrimitive>()
                    ?.content ?: reportErrorToUser {
                    "books data isn't in the expected format!"
                }

                this[id] = title
            }
        }

        return refined
    }
}

private val alphaNumericRegex = """\p{Alnum}+""".toRegex(RegexOption.IGNORE_CASE)

/**
 * Creates a [MangasPage] containing a sorted list of mangas from best match to words.
 *
 * If Even a single "word" from [searchQuery] matches, then the manga will be included,
 * but sorting is done based on the amount of matches.
 */
internal fun Map<BookID, BookTitle>.toMangasPage(searchQuery: String, useSimpleMode: Boolean): MangasPage {
    data class Match(val bookID: BookID, val title: BookTitle, val count: Int) {
        val bookUrl: HttpUrl = homepageUrl.newBuilder()
            .addPathSegment("book")
            .addPathSegment(bookID)
            .build()
    }

    when {
        useSimpleMode -> {
            // simple search, possibly faster
            val words: Set<String> = alphaNumericRegex.findAll(searchQuery).mapTo(HashSet()) { it.value }

            val matches: Map<BookID, Match> = mapValues { (bookID, bookTitle) ->
                val matchesCount: Int = words.sumOf { word ->
                    var count = 0
                    var idx = 0

                    while (true) {
                        val found = bookTitle.indexOf(word, idx, ignoreCase = true)
                        if (found < 0) break

                        idx = found + 1
                        count++
                    }

                    count
                }

                Match(bookID, bookTitle, matchesCount)
            }.filterValues { it.count > 0 }

            return MangasPage(
                mangas = matches.entries
                    .sortedWith(compareBy({ -it.value.count }, { it.value.title }))
                    .map { (bookID, match: Match) ->
                        SManga.create().apply {
                            title = match.title
                            url = match.bookUrl.rawRelative ?: reportErrorToUser { "Could not relativize ${match.bookUrl}" }
                            thumbnail_url = bookThumbnailUrl(bookID, "").toUri().toASCIIString()
                        }
                    },
                hasNextPage = false,
            )
        }

        Build.VERSION.SDK_INT >= Build.VERSION_CODES.N -> {
            // use ICU, better

            val searchWords: Set<String> = BreakIterator.getWordInstance().run {
                text = StringCharacterIterator(searchQuery)

                var left = first()
                var right = next()

                buildSet {
                    while (right != BreakIterator.DONE) {
                        if (ruleStatus != BreakIterator.WORD_NONE) {
                            add(searchQuery.substring(left, right))
                        }
                        left = right
                        right = next()
                    }
                }
            }

            val stringSearch = StringSearch(
                /* pattern = */ "dummy",
                /* target = */ StringCharacterIterator("dummy"),
                /* collator = */
                (Collator.getInstance() as RuleBasedCollator).apply {
                    isCaseLevel = true
                    strength = Collator.PRIMARY
                    decomposition = Collator.CANONICAL_DECOMPOSITION
                },
            )

            val matches: Map<BookID, Match> = mapValues { (bookID, bookTitle) ->
                stringSearch.target = StringCharacterIterator(bookTitle)

                val matchesCount: Int = searchWords.sumOf { word ->
                    val search: StringSearch = stringSearch.apply {
                        this.pattern = word
                    }

                    var count = 0
                    var idx = search.first()
                    while (idx != StringSearch.DONE) {
                        count++
                        idx = search.next()
                    }

                    count
                }

                Match(bookID, bookTitle, matchesCount)
            }.filterValues { it.count > 0 }

            return MangasPage(
                mangas = matches.entries
                    .sortedWith(compareBy({ -it.value.count }, { it.value.title }))
                    .map { (bookID, match: Match) ->
                        SManga.create().apply {
                            title = match.title
                            url = match.bookUrl.rawRelative ?: reportErrorToUser { "Could not relativize ${match.bookUrl}" }
                            thumbnail_url = bookThumbnailUrl(bookID, "").toUri().toASCIIString()
                        }
                    },
                hasNextPage = false,
            )
        }

        else -> error(
            buildString {
                append("Please enable ")
                append(ProjectSukiFilters.SearchMode.SIMPLE)
                append(" Search Mode: ")
                append(ProjectSukiFilters.SearchMode.SMART)
                append(" search requires Android API version >= 24, but ")
                append(Build.VERSION.SDK_INT)
                append(" was found!")
            },
        )
    }
}
