package eu.kanade.tachiyomi.extension.en.mangareadersite

import eu.kanade.tachiyomi.lib.ratelimit.SpecificHostRateLimitInterceptor
import eu.kanade.tachiyomi.multisrc.mangahub.MangaHub
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class MangaReaderSite : MangaHub(
    "MangaReader.site",
    "https://mangareader.site",
    "en"
) {
    override val client: OkHttpClient = super.client.newBuilder()
        .addInterceptor(SpecificHostRateLimitInterceptor(cdnHost, 1, 2))
        .build()

    override val serverId = "mr01"
}
