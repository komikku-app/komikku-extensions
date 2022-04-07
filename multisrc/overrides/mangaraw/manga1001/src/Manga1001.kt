package eu.kanade.tachiyomi.extension.ja.manga1001

import eu.kanade.tachiyomi.multisrc.mangaraw.MangaRaw
import eu.kanade.tachiyomi.network.GET

class Manga1001 : MangaRaw("Manga1001", "https://manga1001.top/") {
    override fun latestUpdatesNextPageSelector(): String? {
        return null
    }
    override fun latestUpdatesRequest(page: Int) = GET("$baseUrl/page/$page", headers)
}
