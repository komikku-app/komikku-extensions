package eu.kanade.tachiyomi.extension.en.mangareadersite

import eu.kanade.tachiyomi.multisrc.mangahub.MangaHub
import eu.kanade.tachiyomi.network.interceptor.rateLimitHost
import okhttp3.OkHttpClient

class MangaReaderSite : MangaHub(
    "MangaReader.site",
    "https://mangareader.site",
    "en"
) {
    override val client: OkHttpClient = super.client.newBuilder()
        .rateLimitHost(cdnHost, 1, 2)
        .build()

    override val serverId = "mr01"
}
