package eu.kanade.tachiyomi.extension.ar.galaxymanga

import eu.kanade.tachiyomi.multisrc.flixscans.Chapter
import eu.kanade.tachiyomi.multisrc.flixscans.FlixScans
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import okhttp3.Request
import okhttp3.Response

class GalaxyManga : FlixScans("جالاكسي مانجا", "https://flixscans.com", "ar") {
    override val versionId = 2

    override fun chapterListRequest(manga: SManga): Request {
        val id = manga.url.split("-")[1]

        return GET("$apiUrl/webtoon/chapters/$id-desc", headers)
    }

    override fun chapterListParse(response: Response): List<SChapter> {
        val chapters = response.parseAs<List<Chapter>>()

        return chapters.map(Chapter::toSChapter)
    }
}
