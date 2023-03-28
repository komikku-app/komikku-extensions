package eu.kanade.tachiyomi.extension.es.olympusscanlation

import eu.kanade.tachiyomi.extension.es.olympusscanlation.dto.OlympusScanlationDto.ChapterDto
import eu.kanade.tachiyomi.extension.es.olympusscanlation.dto.OlympusScanlationDto.MangaDetailDto
import eu.kanade.tachiyomi.extension.es.olympusscanlation.dto.OlympusScanlationDto.MangaDto
import eu.kanade.tachiyomi.extension.es.olympusscanlation.dto.OlympusScanlationDto.PayloadChapterDto
import eu.kanade.tachiyomi.extension.es.olympusscanlation.dto.OlympusScanlationDto.PayloadHomeDto
import eu.kanade.tachiyomi.extension.es.olympusscanlation.dto.OlympusScanlationDto.PayloadMangaDto
import eu.kanade.tachiyomi.extension.es.olympusscanlation.dto.OlympusScanlationDto.PayloadPagesDto
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import okhttp3.Response
import uy.kohesive.injekt.injectLazy
class OlympusScanlation : HttpSource() {

    override val baseUrl: String = "https://olympusscans.com"
    private val apiBaseUrl: String = "https://dashboard.olympusscans.com"
    override val lang: String = "es"
    override val name: String = "Olympus Scanlation"
    override val versionId = 2
    override val supportsLatest: Boolean = true
    private val json: Json by injectLazy()

    // Search
    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        val apiUrl = "$apiBaseUrl/api/search".toHttpUrl().newBuilder()
            .addQueryParameter("name", query)
            .build()
        return GET(apiUrl, headers)
    }

    override fun searchMangaParse(response: Response): MangasPage {
        val result = json.decodeFromString<PayloadMangaDto>(response.body.string())
        val mangaList = result.data.map {
            SManga.create().apply {
                url = "/series/comic-${it.slug}"
                title = it.name
                thumbnail_url = it.cover
            }
        }
        return MangasPage(mangaList, hasNextPage = false)
    }

    // Latest
    override fun latestUpdatesRequest(page: Int): Request = popularMangaRequest(1)
    override fun latestUpdatesParse(response: Response): MangasPage {
        val result = json.decodeFromString<PayloadHomeDto>(response.body.string())
        val mangaList = result.data.new_chapters.map {
            SManga.create().apply {
                url = "/series/comic-${it.slug}"
                title = it.name
                thumbnail_url = it.cover
            }
        }
        return MangasPage(mangaList, hasNextPage = false)
    }

    // Details
    override fun mangaDetailsParse(response: Response): SManga {
        val slug = response.request.url
            .toString()
            .substringAfter("/series/comic-")
            .substringBefore("/chapters")
        val urla = "$apiBaseUrl/api/series/$slug?type=comic"
        val newRequest = GET(url = urla, headers = headers)
        val newResponse = client.newCall(newRequest).execute()
        val result = json.decodeFromString<MangaDetailDto>(newResponse.body.string())
        return SManga.create().apply {
            url = "/series/comic-$slug"
            title = result.data.name
            thumbnail_url = result.data.cover
            description = result.data.summary
        }
    }
    override fun imageUrlParse(response: Response): String = throw Exception("Not used")

    // Chapters

    override fun chapterListRequest(manga: SManga): Request {
        return paginatedChapterListRequest(
            manga.url
                .substringAfter("/series/comic-")
                .substringBefore("/chapters"),
            1,
        )
    }

    private fun paginatedChapterListRequest(mangaUrl: String, page: Int): Request {
        return GET(
            url = "$apiBaseUrl/api/series/$mangaUrl/chapters?page=$page&direction=desc&type=comic",
            headers = headers,
        )
    }

    override fun chapterListParse(response: Response): List<SChapter> {
        val slug = response.request.url
            .toString()
            .substringAfter("/series/")
            .substringBefore("/chapters")
        var data = json.decodeFromString<PayloadChapterDto>(response.body.string())
        var resultSize = data.data.size
        var page = 2
        while (data.meta.total > resultSize) {
            val newRequest = paginatedChapterListRequest("$slug", page)
            val newResponse = client.newCall(newRequest).execute()
            var newData = json.decodeFromString<PayloadChapterDto>(newResponse.body.string())
            data.data += newData.data
            resultSize += newData.data.size
            page += 1
        }
        return data.data.map { chap -> chapterFromObject(chap, slug) }
    }

    private fun chapterFromObject(chapter: ChapterDto, slug: String) = SChapter.create().apply {
        url = "/capitulo/${chapter.id}/comic-$slug"
        name = "Capitulo ${chapter.name}"
        chapter_number = chapter.name.toFloatOrNull() ?: -1f
    }
    // Pages

    override fun pageListRequest(chapter: SChapter): Request {
        var id = chapter.url
            .substringAfter("/capitulo/")
            .substringBefore("/chapters")
            .substringBefore("/comic")
        val slug = chapter.url
            .substringAfter("comic-")
            .substringBefore("/chapters")
            .substringBefore("/comic")
        return GET("$apiBaseUrl/api/series/$slug/chapters/$id?type=comic")
    }

    override fun pageListParse(response: Response): List<Page> =
        json.decodeFromString<PayloadPagesDto>(response.body.string()).chapter.pages.mapIndexed { i, img ->
            Page(i, "", img)
        }

    // Popular
    override fun popularMangaParse(response: Response): MangasPage {
        val result = json.decodeFromString<PayloadHomeDto>(response.body.string())
        val resultMangaList = json.decodeFromString<List<MangaDto>>(result.data.popular_comics)
        val mangaList = resultMangaList.map {
            SManga.create().apply {
                url = "/series/comic-${it.slug}"
                title = it.name
                thumbnail_url = it.cover
            }
        }
        return MangasPage(mangaList, hasNextPage = false)
    }
    override fun popularMangaRequest(page: Int): Request {
        val apiUrl = "$apiBaseUrl/api/home".toHttpUrl().newBuilder()
            .build()
        return GET(apiUrl, headers)
    }
}
