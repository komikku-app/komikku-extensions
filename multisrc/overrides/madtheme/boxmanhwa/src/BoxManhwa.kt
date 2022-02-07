package eu.kanade.tachiyomi.extension.en.boxmanhwa

import eu.kanade.tachiyomi.lib.ratelimit.RateLimitInterceptor
import eu.kanade.tachiyomi.multisrc.madtheme.MadTheme
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class BoxManhwa : MadTheme(
    "BoxManhwa",
    "https://boxmanhwa.com",
    "en"
) {
    override val client: OkHttpClient = network.cloudflareClient.newBuilder()
        .addInterceptor(RateLimitInterceptor(1, 2, TimeUnit.SECONDS))
        .build()
}
