package eu.kanade.tachiyomi.extension.en.firstkissmanga

import eu.kanade.tachiyomi.multisrc.madara.Madara
import eu.kanade.tachiyomi.network.interceptor.rateLimit
import java.util.concurrent.TimeUnit

class FirstKissManga : Madara(
    "1st Kiss",
    "https://1stkissmanga.me",
    "en",
) {
    override val client = super.client.newBuilder()
        .rateLimit(1, 3, TimeUnit.SECONDS)
        .build()
}
