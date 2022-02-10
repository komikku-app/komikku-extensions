package eu.kanade.tachiyomi.extension.en.mangakakalotfun

import eu.kanade.tachiyomi.lib.ratelimit.SpecificHostRateLimitInterceptor
import eu.kanade.tachiyomi.multisrc.mangahub.MangaHub
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class MangakakalotFun : MangaHub(
    "Mangakakalot.fun",
    "https://mangakakalot.fun",
    "en"
) {
    override val client: OkHttpClient = super.client.newBuilder()
        .addInterceptor(SpecificHostRateLimitInterceptor(cdnHost, 1, 2))
        .build()

    override val serverId = "mn01"
}
