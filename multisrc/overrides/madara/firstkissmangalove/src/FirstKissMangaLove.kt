package eu.kanade.tachiyomi.extension.en.firstkissmangalove

import eu.kanade.tachiyomi.lib.ratelimit.RateLimitInterceptor
import eu.kanade.tachiyomi.multisrc.madara.Madara
import java.util.concurrent.TimeUnit

class FirstKissMangaLove : Madara(
    "1st Kiss Manga.love",
    "https://1stkissmanga.love",
    "en"
) {
    private val rateLimitInterceptor = RateLimitInterceptor(1, 2, TimeUnit.SECONDS)

    override val client = network.cloudflareClient.newBuilder()
        .addNetworkInterceptor(rateLimitInterceptor)
        .build()
}
