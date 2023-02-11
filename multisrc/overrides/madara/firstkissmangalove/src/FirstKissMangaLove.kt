package eu.kanade.tachiyomi.extension.en.firstkissmangalove

import eu.kanade.tachiyomi.multisrc.madara.Madara
import eu.kanade.tachiyomi.network.interceptor.rateLimit
import java.util.concurrent.TimeUnit

class FirstKissMangaLove : Madara(
    "1st Kiss Manga.love",
    "https://1stkissmanga.love",
    "en",
) {

    override val client = network.cloudflareClient.newBuilder()
        .rateLimit(1, 3, TimeUnit.SECONDS)
        .build()
}
