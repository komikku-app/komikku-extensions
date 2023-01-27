package eu.kanade.tachiyomi.extension.en.aquamanga

import eu.kanade.tachiyomi.multisrc.madara.Madara
import eu.kanade.tachiyomi.network.interceptor.rateLimit

class AquaManga : Madara("Aqua Manga", "https://aquamanga.com", "en") {

    override val client = super.client.newBuilder()
        .rateLimit(1, 3) // 1 request per 3 seconds
        .build()

    override fun headersBuilder(): Headers.Builder = super.headersBuilder()
        .set("Origin", baseUrl)
        .set("Sec-Fetch-Dest", "empty")
        .set("Sec-Fetch-Mode", "cors")
        .set("Sec-Fetch-Site", "same-origin")

    override val chapterUrlSuffix = ""
}
