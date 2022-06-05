package eu.kanade.tachiyomi.extension.en.mangapandaonl

import eu.kanade.tachiyomi.multisrc.mangahub.MangaHub
import eu.kanade.tachiyomi.network.interceptor.rateLimitHost
import okhttp3.OkHttpClient

class MangaPandaOnl : MangaHub(
    "MangaPanda.onl",
    "https://mangapanda.onl",
    "en"
) {
    override val client: OkHttpClient = super.client.newBuilder()
        .rateLimitHost(cdnHost, 1, 2)
        .build()

    override val serverId = "mr02"
}
