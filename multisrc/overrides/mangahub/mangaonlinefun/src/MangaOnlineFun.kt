package eu.kanade.tachiyomi.extension.en.mangaonlinefun

import eu.kanade.tachiyomi.multisrc.mangahub.MangaHub
import eu.kanade.tachiyomi.network.interceptor.rateLimitHost
import okhttp3.OkHttpClient

class MangaOnlineFun : MangaHub(
    "MangaOnline.fun",
    "https://mangaonline.fun",
    "en"
) {
    override val client: OkHttpClient = super.client.newBuilder()
        .rateLimitHost(cdnHost, 1, 2)
        .build()

    override val serverId = "m02"
}
