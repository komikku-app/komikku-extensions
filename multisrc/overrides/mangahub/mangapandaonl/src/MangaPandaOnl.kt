package eu.kanade.tachiyomi.extension.en.mangapandaonl

import eu.kanade.tachiyomi.lib.ratelimit.RateLimitInterceptor
import eu.kanade.tachiyomi.multisrc.mangahub.MangaHub
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class MangaPandaOnl : MangaHub(
    "MangaPanda.onl",
    "https://mangapanda.onl",
    "en"
) {
    override val client: OkHttpClient = super.client.newBuilder()
        .addInterceptor(RateLimitInterceptor(1, 2, TimeUnit.SECONDS))
        .build()

    override val serverId = "mr02"
}
