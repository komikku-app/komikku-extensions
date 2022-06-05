package eu.kanade.tachiyomi.extension.en.mangahereonl

import eu.kanade.tachiyomi.multisrc.mangahub.MangaHub
import eu.kanade.tachiyomi.network.interceptor.rateLimitHost
import okhttp3.OkHttpClient

class MangaHereOnl : MangaHub(
    "MangaHere.onl",
    "https://mangahere.onl",
    "en"
) {
    override val client: OkHttpClient = super.client.newBuilder()
        .rateLimitHost(cdnHost, 1, 2)
        .build()

    override val serverId = "mh01"
}
