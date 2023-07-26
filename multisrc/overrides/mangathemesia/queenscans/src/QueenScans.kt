package eu.kanade.tachiyomi.extension.en.queenscans

import eu.kanade.tachiyomi.multisrc.mangathemesia.MangaThemesia
import eu.kanade.tachiyomi.network.interceptor.rateLimit

class QueenScans : MangaThemesia("Queen Scans", "https://queenscans.com", "en", "/comics") {
    override val client = super.client.newBuilder()
        .rateLimit(2)
        .build()
}
