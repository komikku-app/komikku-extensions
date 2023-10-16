package eu.kanade.tachiyomi.extension.en.aquamanga

import eu.kanade.tachiyomi.multisrc.madara.Madara
import eu.kanade.tachiyomi.network.interceptor.rateLimit
import okhttp3.Headers

class AquaManga : Madara("Aqua Manga", "https://aquamanga.com", "en") {

    override val client = super.client.newBuilder()
        .rateLimit(1, 3) // 1 request per 3 seconds
        .build()

    override fun headersBuilder(): Headers.Builder = super.headersBuilder()
        .add("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9")
        .add("Accept-Language", "en-US,en;q=0.5")
        .add("DNT", "1")
        .add("Referer", "$baseUrl/")
        .add("Sec-Fetch-Dest", "document")
        .add("Sec-Fetch-Mode", "navigate")
        .add("Sec-Fetch-Site", "same-origin")
        .add("Sec-Fetch-User", "?1")
        .add("Upgrade-Insecure-Requests", "1")
        .add("X-Requested-With", "XMLHttpRequest")

    override val chapterUrlSuffix = ""
}
