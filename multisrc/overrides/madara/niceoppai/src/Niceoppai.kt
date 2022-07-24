package eu.kanade.tachiyomi.extension.th.niceoppai

import eu.kanade.tachiyomi.multisrc.madara.Madara
import eu.kanade.tachiyomi.network.GET
import okhttp3.Request

class Niceoppai : Madara("Niceoppai", "https://www.niceoppai.net", "th") {
    private fun pagePath(page: Int) = if (page > 1) "$page/" else ""
    override fun popularMangaRequest(page: Int): Request = GET("$baseUrl/manga_list/all/any/most-popular/${pagePath(page)}", headers)
    override fun latestUpdatesRequest(page: Int): Request = GET("$baseUrl/manga_list/all/any/last-updated/${pagePath(page)}", headers)
}
