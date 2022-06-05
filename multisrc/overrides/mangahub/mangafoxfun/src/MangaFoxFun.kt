package eu.kanade.tachiyomi.extension.en.mangafoxfun

import eu.kanade.tachiyomi.multisrc.mangahub.MangaHub
import eu.kanade.tachiyomi.network.interceptor.rateLimitHost
import okhttp3.OkHttpClient

class MangaFoxFun : MangaHub(
    "MangaFox.fun",
    "https://mangafox.fun",
    "en"
) {
    override val client: OkHttpClient = super.client.newBuilder()
        .rateLimitHost(cdnHost, 1, 2)
        .build()

    override val serverId = "mf01"
}
