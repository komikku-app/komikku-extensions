package eu.kanade.tachiyomi.extension.pt.animaregia

import eu.kanade.tachiyomi.lib.ratelimit.RateLimitInterceptor
import eu.kanade.tachiyomi.multisrc.mmrcms.MMRCMS
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class AnimaRegia : MMRCMS("AnimaRegia", "https://animaregia.net", "pt-BR") {
    override val id: Long = 4378659695320121364

    override val client: OkHttpClient = super.client.newBuilder()
        .addInterceptor(RateLimitInterceptor(1, 2, TimeUnit.SECONDS))
        .build()
}
