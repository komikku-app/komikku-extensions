package eu.kanade.tachiyomi.extension.en.mangatownhub

import eu.kanade.tachiyomi.multisrc.mangahub.MangaHub
import eu.kanade.tachiyomi.network.interceptor.rateLimitHost
import okhttp3.OkHttpClient

class MangaTownHub : MangaHub(
    "MangaTown (unoriginal)",
    "https://manga.town",
    "en"
) {
    override val client: OkHttpClient = super.client.newBuilder()
        .rateLimitHost(cdnHost, 1, 2)
        .build()

    override val serverId = "mt01"
}
