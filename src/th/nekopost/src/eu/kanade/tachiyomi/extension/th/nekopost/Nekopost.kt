package eu.kanade.tachiyomi.extension.th.nekopost

import eu.kanade.tachiyomi.extension.th.nekopost.model.RawChapterInfo
import eu.kanade.tachiyomi.extension.th.nekopost.model.RawProjectInfo
import eu.kanade.tachiyomi.extension.th.nekopost.model.RawProjectNameListItem
import eu.kanade.tachiyomi.extension.th.nekopost.model.RawProjectSummaryList
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.asObservableSuccess
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import rx.Observable
import uy.kohesive.injekt.injectLazy
import java.text.SimpleDateFormat
import java.util.Locale

class Nekopost : ParsedHttpSource() {
    private val json: Json by injectLazy()
    override val baseUrl: String = "https://www.nekopost.net/project"

    private val latestMangaEndpoint: String =
        "https://tuner.nekopost.net/ApiTest/getLatestChapterOffset/m"
    private val projectDataEndpoint: String =
        "https://tuner.nekopost.net/ApiTest/getProjectDetailFull"
    private val fileHost: String = "https://fs.nekopost.net"

    override val client: OkHttpClient = network.cloudflareClient

    override fun headersBuilder(): Headers.Builder {
        return super.headersBuilder().add("Referer", baseUrl)
    }

    private val existingProject: HashSet<String> = HashSet()

    override val lang: String = "th"
    override val name: String = "Nekopost"

    override val supportsLatest: Boolean = false

    private fun getStatus(status: String) = when (status) {
        "1" -> SManga.ONGOING
        "2" -> SManga.COMPLETED
        "3" -> SManga.LICENSED
        else -> SManga.UNKNOWN
    }

    override fun latestUpdatesRequest(page: Int): Request = throw NotImplementedError("Unused")

    override fun latestUpdatesParse(response: Response): MangasPage =
        throw NotImplementedError("Unused")

    override fun chapterListSelector(): String = throw NotImplementedError("Unused")

    override fun chapterFromElement(element: Element): SChapter =
        throw NotImplementedError("Unused")

    override fun fetchImageUrl(page: Page): Observable<String> = Observable.just(page.imageUrl)

    override fun imageUrlParse(document: Document): String = throw NotImplementedError("Unused")

    override fun latestUpdatesFromElement(element: Element): SManga = throw Exception("Unused")

    override fun latestUpdatesNextPageSelector(): String = throw Exception("Unused")

    override fun latestUpdatesSelector(): String = throw Exception("Unused")

    override fun mangaDetailsParse(document: Document): SManga = throw NotImplementedError("Unused")

    override fun fetchMangaDetails(manga: SManga): Observable<SManga> {
        return client.newCall(GET("$projectDataEndpoint/${manga.url}"))
            .asObservableSuccess()
            .map { response ->
                val responseBody =
                    response.body ?: throw Error("Unable to fetch manga detail of ${manga.title}")
                val projectInfo: RawProjectInfo = json.decodeFromString(responseBody.string())

                manga.apply {
                    projectInfo.projectInfo.let {
                        url = it.npProjectId
                        title = it.npName
                        artist = it.artistName
                        author = it.authorName
                        description = it.npInfo
                        status = getStatus(it.npStatus)
                        initialized = true
                    }

                    genre =
                        projectInfo.projectCategoryUsed.joinToString(", ") { it.npcName }
                }
            }
    }

    override fun fetchChapterList(manga: SManga): Observable<List<SChapter>> {
        return if (manga.status != SManga.LICENSED) {
            client.newCall(GET("$projectDataEndpoint/${manga.url}"))
                .asObservableSuccess()
                .map { response ->
                    val responseBody =
                        response.body
                            ?: throw Error("Unable to fetch manga detail of ${manga.title}")
                    val projectInfo: RawProjectInfo = json.decodeFromString(responseBody.string())

                    projectInfo.projectChapterList.map { chapter ->
                        SChapter.create().apply {
                            url = "${manga.url}/${chapter.ncChapterId}/${chapter.ncDataFile}"
                            name = chapter.ncChapterName
                            date_upload = SimpleDateFormat(
                                "yyyy-MM-dd HH:mm:ss",
                                Locale("th")
                            ).parse(chapter.ncCreatedDate)?.time
                                ?: 0L
                            chapter_number = chapter.ncChapterNo.toFloat()
                            scanlator = chapter.cuDisplayname
                        }
                    }
                }
        } else {
            Observable.error(Exception("Licensed - No chapter to show"))
        }
    }

    override fun fetchPageList(chapter: SChapter): Observable<List<Page>> {
        return client.newCall(GET("$fileHost/collectManga/${chapter.url}"))
            .asObservableSuccess()
            .map { response ->
                val responseBody =
                    response.body
                        ?: throw Error("Unable to fetch page list of chapter ${chapter.chapter_number}")
                val chapterInfo: RawChapterInfo = json.decodeFromString(responseBody.string())

                chapterInfo.pageItem.map { page ->
                    Page(
                        index = page.pageNo,
                        imageUrl = "$fileHost/collectManga/${chapterInfo.projectId}/${chapterInfo.chapterId}/${page.fileName}",
                    )
                }
            }
    }

    override fun pageListParse(document: Document): List<Page> = throw NotImplementedError("Unused")

    override fun popularMangaRequest(page: Int): Request {
        if (page <= 1) existingProject.clear()

        return GET("$latestMangaEndpoint/${page - 1}")
    }

    override fun popularMangaParse(response: Response): MangasPage {
        val responseBody = response.body ?: throw Error("Unable to fetch mangas")
        val projectList: RawProjectSummaryList = json.decodeFromString(responseBody.string())

        val mangaList: List<SManga> =
            projectList.listItem
                ?.filter { !existingProject.contains(it.npProjectId) }
                ?.map {
                    SManga.create().apply {
                        url = it.npProjectId
                        title = it.npName
                        thumbnail_url =
                            "$fileHost/collectManga/${it.npProjectId}/${it.npProjectId}_cover.jpg"
                        initialized = false
                        status = 0
                    }
                } ?: return MangasPage(emptyList(), hasNextPage = false)

        mangaList.forEach { existingProject.add(it.url) }

        return MangasPage(mangaList, hasNextPage = true)
    }

    override fun popularMangaFromElement(element: Element): SManga =
        throw NotImplementedError("Unused")

    override fun popularMangaNextPageSelector(): String = throw Exception("Unused")

    override fun popularMangaSelector(): String = throw Exception("Unused")

    override fun searchMangaFromElement(element: Element): SManga = throw Exception("Unused")

    override fun searchMangaNextPageSelector(): String = throw Exception("Unused")

    override fun fetchSearchManga(
        page: Int,
        query: String,
        filters: FilterList
    ): Observable<MangasPage> {
        return client.newCall(GET("$fileHost/dataJson/dataProjectName.json"))
            .asObservableSuccess()
            .map { response ->
                val responseBody = response.body ?: throw Error("Unable to fetch title list")
                val projectList: List<RawProjectNameListItem> = json.decodeFromString(responseBody.string())

                val mangaList: List<SManga> = projectList.filter { project ->
                    Regex(
                        query,
                        setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE)
                    ).find(project.npName) != null
                }.map { project ->
                    SManga.create().apply {
                        url = project.npProjectId
                        title = project.npName
                        status = getStatus(project.npStatus)
                        initialized = false
                    }
                }

                MangasPage(mangaList, false)
            }
    }

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request =
        throw Exception("Unused")

    override fun searchMangaParse(response: Response): MangasPage = throw Exception("Unused")

    override fun searchMangaSelector(): String = throw Exception("Unused")
}
