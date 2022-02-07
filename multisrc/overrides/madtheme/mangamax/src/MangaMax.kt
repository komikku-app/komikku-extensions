package eu.kanade.tachiyomi.extension.en.mangamax

import eu.kanade.tachiyomi.lib.ratelimit.RateLimitInterceptor
import eu.kanade.tachiyomi.multisrc.madtheme.MadTheme
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class MangaMax : MadTheme(
    "MangaMax",
    "https://mangamax.net",
    "en"
) {
    override val client: OkHttpClient = network.cloudflareClient.newBuilder()
        .addInterceptor(RateLimitInterceptor(1, 2, TimeUnit.SECONDS))
        .build()
}
