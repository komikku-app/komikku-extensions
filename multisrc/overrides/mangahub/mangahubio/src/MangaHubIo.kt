package eu.kanade.tachiyomi.extension.en.mangahubio

import eu.kanade.tachiyomi.multisrc.mangahub.MangaHub
import eu.kanade.tachiyomi.network.interceptor.rateLimitHost
import okhttp3.OkHttpClient

class MangaHubIo : MangaHub(
    "MangaHub",
    "https://mangahub.io",
    "en"
) {
    override val client: OkHttpClient = super.client.newBuilder()
        .rateLimitHost(cdnHost, 1, 2)
        .build()

    override val serverId = "m01"
}
