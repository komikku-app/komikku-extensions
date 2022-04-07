package eu.kanade.tachiyomi.extension.ja.mangapro

import eu.kanade.tachiyomi.multisrc.mangaraw.MangaRaw
import eu.kanade.tachiyomi.network.GET

class MangaPro : MangaRaw("MangaPro", "https://mangapro.top") {
    override fun latestUpdatesNextPageSelector(): String? {
        return null
    }

    override fun latestUpdatesRequest(page: Int) = GET("$baseUrl/page/$page", headers)
}
