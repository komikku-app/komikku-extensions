package eu.kanade.tachiyomi.extension.pt.hentaitokyo

import eu.kanade.tachiyomi.lib.ratelimit.RateLimitInterceptor
import eu.kanade.tachiyomi.multisrc.gattsu.Gattsu
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class HentaiTokyo : Gattsu(
    "Hentai Tokyo",
    "https://hentaitokyo.net",
    "pt-BR"
) {

    override val client: OkHttpClient = super.client.newBuilder()
        .addInterceptor(RateLimitInterceptor(1, 2, TimeUnit.SECONDS))
        .build()
}
