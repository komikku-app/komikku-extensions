package eu.kanade.tachiyomi.extension.en.mangabuddy

import eu.kanade.tachiyomi.multisrc.madtheme.MadTheme
import eu.kanade.tachiyomi.network.interceptor.rateLimitHost
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient

class MangaBuddy : MadTheme(
    "MangaBuddy",
    "https://mangabuddy.com",
    "en"
) {
    override val client: OkHttpClient = network.cloudflareClient.newBuilder()
        .rateLimitHost("https://s1.mbcdnv1.xyz".toHttpUrl(), 1, 1)
        .rateLimitHost("https://s1.mbcdnv2.xyz".toHttpUrl(), 1, 2)
        .rateLimitHost("https://s1.mbcdnv3.xyz".toHttpUrl(), 1, 2)
        .rateLimitHost("https://s1.mbcdnv4.xyz".toHttpUrl(), 1, 2)
        .rateLimitHost("https://s1.mbcdnv5.xyz".toHttpUrl(), 1, 2)
        .build()
}
