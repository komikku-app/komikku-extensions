package eu.kanade.tachiyomi.extension.en.mangahereonl

import eu.kanade.tachiyomi.lib.ratelimit.RateLimitInterceptor
import eu.kanade.tachiyomi.multisrc.mangahub.MangaHub
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class MangaHereOnl : MangaHub(
    "MangaHere.onl",
    "https://mangahere.onl",
    "en"
) {
    override val client: OkHttpClient = super.client.newBuilder()
        .addInterceptor(RateLimitInterceptor(1, 2, TimeUnit.SECONDS))
        .build()

    override val serverId = "mh01"
}
