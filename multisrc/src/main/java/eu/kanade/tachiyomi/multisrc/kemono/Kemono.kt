package eu.kanade.tachiyomi.multisrc.kemono

import android.app.Application
import androidx.preference.ListPreference
import androidx.preference.PreferenceScreen
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.interceptor.rateLimit
import eu.kanade.tachiyomi.source.ConfigurableSource
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.util.asJsoup
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import okio.blackholeSink
import org.jsoup.nodes.Element
import org.jsoup.select.Evaluator
import rx.Observable
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy
import java.io.IOException
import java.util.TimeZone
import kotlin.math.min

open class Kemono(
    override val name: String,
    override val baseUrl: String,
    override val lang: String = "all",
) : HttpSource(), ConfigurableSource {
    override val supportsLatest = true

    private val isNewDesign get() = name == "Kemono"

    override val client = network.client.newBuilder().rateLimit(2).build()

    override fun headersBuilder() = super.headersBuilder()
        .add("Referer", "$baseUrl/")

    private val json: Json by injectLazy()

    private val preferences by lazy {
        Injekt.get<Application>().getSharedPreferences("source_$id", 0x0000)
    }

    override fun popularMangaRequest(page: Int): Request =
        GET("$baseUrl/artists?o=${PAGE_SIZE * (page - 1)}", headers)

    override fun popularMangaParse(response: Response): MangasPage {
        val document = response.asJsoup()
        val cardList = document.selectFirst(Evaluator.Class("card-list"))!!
        val creators = cardList.select(Evaluator.Tag("article")).map {
            val children = it.children()
            val avatar = children[0].selectFirst(Evaluator.Tag("img"))!!.attr("src")
            val link = children[1].child(0)
            val service = children[2].ownText()
            SManga.create().apply {
                url = link.attr("href")
                title = link.ownText()
                author = service
                thumbnail_url = baseUrl + avatar
                description = PROMPT
                initialized = true
            }
        }.filterUnsupported()
        return MangasPage(creators, document.hasNextPage())
    }

    override fun latestUpdatesRequest(page: Int): Request =
        GET("$baseUrl/artists/updated?o=${PAGE_SIZE * (page - 1)}", headers)

    override fun latestUpdatesParse(response: Response) = popularMangaParse(response)

    override fun fetchPopularManga(page: Int): Observable<MangasPage> {
        if (!isNewDesign) return super.fetchPopularManga(page)
        return Observable.fromCallable {
            fetchNewDesignListing(page, "/artists", compareByDescending { it.favorited })
        }
    }

    override fun fetchLatestUpdates(page: Int): Observable<MangasPage> {
        if (!isNewDesign) return super.fetchLatestUpdates(page)
        return Observable.fromCallable {
            fetchNewDesignListing(page, "/artists/updated", compareByDescending { it.updatedDate })
        }
    }

    private fun fetchNewDesignListing(
        page: Int,
        path: String,
        comparator: Comparator<KemonoCreatorDto>,
    ): MangasPage {
        val baseUrl = baseUrl
        return if (page == 1) {
            val document = client.newCall(GET(baseUrl + path, headers)).execute().asJsoup()
            val cardList = document.selectFirst(Evaluator.Class("card-list__items"))!!
            val creators = cardList.children().map {
                SManga.create().apply {
                    url = it.attr("href")
                    title = it.selectFirst(Evaluator.Class("user-card__name"))!!.ownText()
                    author = it.selectFirst(Evaluator.Class("user-card__service"))!!.ownText()
                    thumbnail_url = baseUrl + it.selectFirst(Evaluator.Tag("img"))!!.attr("src")
                    description = PROMPT
                    initialized = true
                }
            }.filterUnsupported()
            MangasPage(creators, true).also { cacheCreators() }
        } else {
            fetchCreatorsPage(page) { it.apply { sortWith(comparator) } }
        }
    }

    override fun fetchSearchManga(page: Int, query: String, filters: FilterList): Observable<MangasPage> = Observable.fromCallable {
        if (query.isBlank()) throw Exception("Query is empty")
        fetchCreatorsPage(page) { all ->
            val result = all.filterTo(ArrayList()) { it.name.contains(query, ignoreCase = true) }
            if (result.isEmpty()) return@fetchCreatorsPage emptyList()
            if (result[0].favorited != -1) {
                result.sortByDescending { it.favorited }
            } else {
                result.sortByDescending { it.updatedDate }
            }
            result
        }
    }

    private fun fetchCreatorsPage(
        page: Int,
        block: (ArrayList<KemonoCreatorDto>) -> List<KemonoCreatorDto>,
    ): MangasPage {
        val baseUrl = this.baseUrl
        val response = client.newCall(GET("$baseUrl/api/creators", headers)).execute()
        val allCreators = block(response.parseAs())
        val count = allCreators.size
        val fromIndex = (page - 1) * NEW_PAGE_SIZE
        val toIndex = min(count, fromIndex + NEW_PAGE_SIZE)
        val creators = allCreators.subList(fromIndex, toIndex)
            .map { it.toSManga(baseUrl) }
            .filterUnsupported()
        return MangasPage(creators, toIndex < count)
    }

    private fun cacheCreators() {
        val callback = object : Callback {
            override fun onResponse(call: Call, response: Response) =
                response.body.source().run {
                    readAll(blackholeSink())
                    close()
                }

            override fun onFailure(call: Call, e: IOException) = Unit
        }
        client.newCall(GET("$baseUrl/api/creators", headers)).enqueue(callback)
    }

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList) = throw UnsupportedOperationException("Not used.")
    override fun searchMangaParse(response: Response) = throw UnsupportedOperationException("Not used.")

    override fun fetchMangaDetails(manga: SManga): Observable<SManga> = Observable.just(manga)

    override fun mangaDetailsParse(response: Response) = throw UnsupportedOperationException("Not used.")

    override fun fetchChapterList(manga: SManga): Observable<List<SChapter>> = Observable.fromCallable {
        KemonoPostDto.dateFormat.timeZone = when (manga.author) {
            "Pixiv Fanbox", "Fantia" -> TimeZone.getTimeZone("GMT+09:00")
            else -> TimeZone.getTimeZone("GMT")
        }
        val maxPosts = preferences.getString(POST_PAGES_PREF, POST_PAGES_DEFAULT)!!
            .toInt().coerceAtMost(POST_PAGES_MAX) * POST_PAGE_SIZE
        var offset = 0
        var hasNextPage = true
        val result = ArrayList<SChapter>()
        while (offset < maxPosts && hasNextPage) {
            val request = GET("$baseUrl/api${manga.url}?limit=$POST_PAGE_SIZE&o=$offset", headers)
            val page: List<KemonoPostDto> = client.newCall(request).execute().parseAs()
            page.forEach { post -> if (post.images.isNotEmpty()) result.add(post.toSChapter()) }
            offset += POST_PAGE_SIZE
            hasNextPage = page.size == POST_PAGE_SIZE
        }
        result
    }

    override fun chapterListParse(response: Response) = throw UnsupportedOperationException("Not used.")

    override fun pageListRequest(chapter: SChapter): Request =
        GET("$baseUrl/api${chapter.url}", headers)

    override fun pageListParse(response: Response): List<Page> {
        val post: List<KemonoPostDto> = response.parseAs()
        return post[0].images.mapIndexed { i, path -> Page(i, imageUrl = baseUrl + path) }
    }

    override fun imageUrlParse(response: Response) = throw UnsupportedOperationException("Not used.")

    private inline fun <reified T> Response.parseAs(): T = use {
        json.decodeFromStream(it.body.byteStream())
    }

    override fun setupPreferenceScreen(screen: PreferenceScreen) {
        ListPreference(screen.context).apply {
            key = POST_PAGES_PREF
            title = "Maximum posts to load"
            summary = "Loading more posts costs more time and network traffic.\nCurrently: %s"
            entryValues = (1..POST_PAGES_MAX).map { it.toString() }.toTypedArray()
            entries = (1..POST_PAGES_MAX).map {
                if (it == 1) "1 page ($POST_PAGE_SIZE posts)" else "$it pages (${it * POST_PAGE_SIZE} posts)"
            }.toTypedArray()
            setDefaultValue(POST_PAGES_DEFAULT)
        }.let { screen.addPreference(it) }
    }

    companion object {
        private const val PAGE_SIZE = 25
        private const val NEW_PAGE_SIZE = 50
        const val PROMPT = "You can change how many posts to load in the extension preferences."

        private const val POST_PAGE_SIZE = 50
        private const val POST_PAGES_PREF = "POST_PAGES"
        private const val POST_PAGES_DEFAULT = "1"
        private const val POST_PAGES_MAX = 50

        private fun Element.hasNextPage(): Boolean {
            val pagination = selectFirst(Evaluator.Class("paginator"))!!
            return pagination.selectFirst("a[title=Next page]") != null
        }

        private fun List<SManga>.filterUnsupported() = filterNot { it.author == "Discord" }
    }
}
