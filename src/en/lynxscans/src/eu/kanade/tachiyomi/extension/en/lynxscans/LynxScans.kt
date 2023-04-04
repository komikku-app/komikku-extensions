package eu.kanade.tachiyomi.extension.en.lynxscans

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.Request
import okhttp3.Response
import uy.kohesive.injekt.injectLazy

class LynxScans : HttpSource() {
    override val baseUrl: String = "https://lynxscans.com"
    private val apiUrl: String = "https://api.lynxscans.com/api"

    override val lang: String = "en"
    override val name: String = "LynxScans"

    override val versionId = 2

    override val supportsLatest: Boolean = true

    private val json: Json by injectLazy()

    // Popular
    override fun popularMangaRequest(page: Int): Request {
        return GET("$apiUrl/comics?page=$page", headers)
    }

    override fun popularMangaParse(response: Response): MangasPage {
        val data = json.decodeFromString<Popular>(response.body.string())

        val titles = data.comics.data.map(PopularComicsData::toSManga)

        return MangasPage(titles, !data.comics.next_page_url.isNullOrEmpty())
    }

    // Latest
    override fun latestUpdatesRequest(page: Int): Request {
        return GET("$apiUrl/latest?page=$page", headers)
    }

    override fun latestUpdatesParse(response: Response): MangasPage {
        val data = json.decodeFromString<Latest>(response.body.string())

        val titles = data.chapters.data.distinctBy { it.comic_titleSlug }.map(LatestChaptersData::toSManga)

        return MangasPage(titles, !data.chapters.next_page_url.isNullOrEmpty())
    }

    // Search
    override fun searchMangaParse(response: Response): MangasPage {
        throw UnsupportedOperationException("Not used")
    }

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        throw Exception("Search is not supported")
    }

    // Details
    override fun mangaDetailsParse(response: Response): SManga {
        val data = json.decodeFromString<MangaDetails>(response.body.string())

        return data.comic.toSManga()
    }

    override fun mangaDetailsRequest(manga: SManga): Request {
        val titleId = manga.url.substringAfterLast("/")

        return GET("$apiUrl/comics/$titleId", headers)
    }

    override fun getMangaUrl(manga: SManga): String {
        val titleId = manga.url.substringAfterLast("/")

        return "$baseUrl/comics/$titleId"
    }

    // Chapters
    override fun chapterListRequest(manga: SManga): Request {
        return mangaDetailsRequest(manga)
    }

    override fun chapterListParse(response: Response): List<SChapter> {
        val data = json.decodeFromString<MangaDetails>(response.body.string())

        val chapters: MutableList<SChapter> = mutableListOf()

        data.comic.volumes.forEach { volume ->
            volume.chapters.forEach { chapter ->
                chapters.add(
                    SChapter.create().apply {
                        url = "/comics/${data.comic.titleSlug}/volume/${volume.number}/chapter/${chapter.number}"
                        name = volume.name + " " + (if (!chapter.name.contains("chapter", true)) "Chapter ${chapter.number} " else "") + chapter.name
                    },
                )
            }
        }

        return chapters
    }

    override fun getChapterUrl(chapter: SChapter): String {
        val chapterPath = chapter.url.substringAfter("/")

        return "$baseUrl/$chapterPath"
    }

    // Page
    override fun pageListRequest(chapter: SChapter): Request {
        val chapterPath = chapter.url.substringAfter("/")

        return GET("$apiUrl/$chapterPath", headers)
    }

    override fun pageListParse(response: Response): List<Page> {
        val data = json.decodeFromString<PageList>(response.body.string())

        return data.pages.mapIndexed { idx, it ->
            Page(idx, imageUrl = it.thumb)
        }
    }

    // Unused
    override fun imageUrlParse(response: Response): String = throw UnsupportedOperationException("Not Used")
}
