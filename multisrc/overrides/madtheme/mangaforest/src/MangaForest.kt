package eu.kanade.tachiyomi.extension.en.mangaforest

import eu.kanade.tachiyomi.multisrc.madtheme.MadTheme
import eu.kanade.tachiyomi.network.interceptor.rateLimitHost
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class MangaForest : MadTheme(
    "MangaForest",
    "https://mangaforest.com",
    "en"
) {
    override val client: OkHttpClient = network.cloudflareClient.newBuilder()
        .rateLimitHost(
            "https://s1.mbcdnv1.xyz".toHttpUrl(),
            5,
            1,
            TimeUnit.SECONDS
        )
        .rateLimitHost(
            "https://s1.mbcdnv2.xyz".toHttpUrl(),
            1,
            2,
            TimeUnit.SECONDS
        )
        .rateLimitHost(
            "https://s1.mbcdnv3.xyz".toHttpUrl(),
            1,
            2,
            TimeUnit.SECONDS
        )
        .rateLimitHost(
            "https://s1.mbcdnv4.xyz".toHttpUrl(),
            1,
            2,
            TimeUnit.SECONDS
        )
        .rateLimitHost(
            "https://s1.mbcdnv5.xyz".toHttpUrl(),
            1,
            2,
            TimeUnit.SECONDS
        )
        .build()
}
