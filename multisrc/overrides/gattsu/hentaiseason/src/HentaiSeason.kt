package eu.kanade.tachiyomi.extension.pt.hentaiseason

import eu.kanade.tachiyomi.lib.ratelimit.RateLimitInterceptor
import eu.kanade.tachiyomi.multisrc.gattsu.Gattsu
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class HentaiSeason : Gattsu(
    "Hentai Season",
    "https://hentaiseason.com",
    "pt-BR"
) {

    override val client: OkHttpClient = super.client.newBuilder()
        .addInterceptor(RateLimitInterceptor(1, 2, TimeUnit.SECONDS))
        .build()
}
