package eu.kanade.tachiyomi.extension.en.aquamanga

import eu.kanade.tachiyomi.multisrc.madara.Madara
import eu.kanade.tachiyomi.network.interceptor.rateLimit

class AquaManga : Madara("Aqua Manga", "https://aquamanga.com", "en") {

    override val client = super.client.newBuilder()
        .rateLimit(1, 2) // 1 request per 2 seconds
        .build()

    override val chapterUrlSuffix = ""
}
