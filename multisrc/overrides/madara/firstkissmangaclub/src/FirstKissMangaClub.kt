package eu.kanade.tachiyomi.extension.en.firstkissmangaclub

import eu.kanade.tachiyomi.lib.ratelimit.RateLimitInterceptor
import eu.kanade.tachiyomi.multisrc.madara.Madara
import java.util.concurrent.TimeUnit

class FirstKissMangaClub : Madara(
    "1stKissManga.Club",
    "https://1stkissmanga.club",
    "en"
) {
    private val rateLimitInterceptor = RateLimitInterceptor(1, 2, TimeUnit.SECONDS)

    override val client = network.cloudflareClient.newBuilder()
        .addNetworkInterceptor(rateLimitInterceptor)
        .build()
}
