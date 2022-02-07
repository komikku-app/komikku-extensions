package eu.kanade.tachiyomi.extension.en.manhuanow

import eu.kanade.tachiyomi.lib.ratelimit.RateLimitInterceptor
import eu.kanade.tachiyomi.multisrc.madtheme.MadTheme
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class ManhuaNow : MadTheme(
    "ManhuaNow",
    "https://manhuanow.com",
    "en"
) {
    override val client: OkHttpClient = network.cloudflareClient.newBuilder()
        .addInterceptor(RateLimitInterceptor(1, 2, TimeUnit.SECONDS))
        .build()
}
