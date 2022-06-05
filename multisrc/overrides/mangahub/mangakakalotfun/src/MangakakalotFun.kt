package eu.kanade.tachiyomi.extension.en.mangakakalotfun

import eu.kanade.tachiyomi.multisrc.mangahub.MangaHub
import eu.kanade.tachiyomi.network.interceptor.rateLimitHost
import okhttp3.OkHttpClient

class MangakakalotFun : MangaHub(
    "Mangakakalot.fun",
    "https://mangakakalot.fun",
    "en"
) {
    override val client: OkHttpClient = super.client.newBuilder()
        .rateLimitHost(cdnHost, 1, 2)
        .build()

    override val serverId = "mn01"
}
