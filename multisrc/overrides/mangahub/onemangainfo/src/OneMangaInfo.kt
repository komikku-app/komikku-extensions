package eu.kanade.tachiyomi.extension.en.onemangainfo

import eu.kanade.tachiyomi.multisrc.mangahub.MangaHub
import eu.kanade.tachiyomi.network.interceptor.rateLimitHost
import okhttp3.OkHttpClient

class OneMangaInfo : MangaHub(
    "OneManga.info",
    "https://onemanga.info",
    "en"
) {
    override val client: OkHttpClient = super.client.newBuilder()
        .rateLimitHost(cdnHost, 1, 2)
        .build()

    override val serverId = "mn02"
}
