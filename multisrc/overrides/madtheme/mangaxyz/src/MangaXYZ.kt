package eu.kanade.tachiyomi.extension.en.mangaxyz

import eu.kanade.tachiyomi.lib.ratelimit.SpecificHostRateLimitInterceptor
import eu.kanade.tachiyomi.multisrc.madtheme.MadTheme
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class MangaXYZ : MadTheme(
    "MangaXYZ",
    "https://mangaxyz.com",
    "en"
) {
    override val client: OkHttpClient = network.cloudflareClient.newBuilder()
        .addInterceptor(
            SpecificHostRateLimitInterceptor(
                "https://s1.mbcdnv1.xyz".toHttpUrl(),
                5,
                1,
                TimeUnit.SECONDS
            )
        )
        .addInterceptor(
            SpecificHostRateLimitInterceptor(
                "https://s1.mbcdnv2.xyz".toHttpUrl(),
                1,
                2,
                TimeUnit.SECONDS
            )
        )
        .addInterceptor(
            SpecificHostRateLimitInterceptor(
                "https://s1.mbcdnv3.xyz".toHttpUrl(),
                1,
                2,
                TimeUnit.SECONDS
            )
        )
        .addInterceptor(
            SpecificHostRateLimitInterceptor(
                "https://s1.mbcdnv4.xyz".toHttpUrl(),
                1,
                2,
                TimeUnit.SECONDS
            )
        )
        .addInterceptor(
            SpecificHostRateLimitInterceptor(
                "https://s1.mbcdnv5.xyz".toHttpUrl(),
                1,
                2,
                TimeUnit.SECONDS
            )
        )
        .build()
}
