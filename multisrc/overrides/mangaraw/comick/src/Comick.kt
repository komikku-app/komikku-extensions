package eu.kanade.tachiyomi.extension.ja.comick

import eu.kanade.tachiyomi.multisrc.mangaraw.MangaRaw
import eu.kanade.tachiyomi.network.GET
import okhttp3.Request

// Comick has a slightly different layout in html, even though it looks exactly the same to MangaRaw visually
class Comick : MangaRaw("Comick", "https://comick.top") {

    override val imageSelector = ".entry-content img"

    // comick.top doesn't have a popular manga page
    // redirect to latest manga request
    override fun popularMangaRequest(page: Int): Request = latestUpdatesRequest(page)

    override fun latestUpdatesRequest(page: Int) = GET("$baseUrl/page/$page", headers)
}
