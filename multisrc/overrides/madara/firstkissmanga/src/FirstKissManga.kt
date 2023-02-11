package eu.kanade.tachiyomi.extension.en.firstkissmanga

import eu.kanade.tachiyomi.multisrc.madara.Madara
import eu.kanade.tachiyomi.network.interceptor.rateLimit
import okhttp3.Headers
import java.util.concurrent.TimeUnit

class FirstKissManga : Madara(
    "1st Kiss",
    "https://1stkissmanga.me",
    "en",
) {
    override fun headersBuilder(): Headers.Builder = super.headersBuilder().add("Referer", baseUrl)

    override val client = network.cloudflareClient.newBuilder()
        .rateLimit(1, 3, TimeUnit.SECONDS)
        .build()
}
