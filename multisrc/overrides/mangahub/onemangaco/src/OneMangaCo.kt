package eu.kanade.tachiyomi.extension.en.onemangaco

import eu.kanade.tachiyomi.multisrc.mangahub.MangaHub
import eu.kanade.tachiyomi.network.interceptor.rateLimitHost
import okhttp3.OkHttpClient

class OneMangaCo : MangaHub(
    "1Manga.co",
    "https://1manga.co",
    "en"
) {
    override val client: OkHttpClient = super.client.newBuilder()
        .rateLimitHost(cdnHost, 1, 2)
        .build()

    override val serverId = "mn03"
}
