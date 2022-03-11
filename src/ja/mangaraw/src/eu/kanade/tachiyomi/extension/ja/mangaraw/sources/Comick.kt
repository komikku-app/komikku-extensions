package eu.kanade.tachiyomi.extension.ja.mangaraw.sources

import eu.kanade.tachiyomi.extension.ja.mangaraw.MangaRaw
import okhttp3.Request

// Comick has a slightly different layout in html, even though it looks exactly the same to MangaRaw visually
class Comick : MangaRaw("Comick", "https://comick.top") {

    override val imageSelector =
        "#main > article > div > div > div.entry-content > center > p > img"

    // comick.top doesn't have a popular manga page
    // redirect to latest manga request
    override fun popularMangaRequest(page: Int): Request = latestUpdatesRequest(page)
}
