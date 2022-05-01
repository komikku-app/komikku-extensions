package eu.kanade.tachiyomi.extension.pt.hipercool

import eu.kanade.tachiyomi.lib.ratelimit.RateLimitInterceptor
import eu.kanade.tachiyomi.multisrc.madara.Madara
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class Hipercool : Madara("HipercooL", "https://hipercool.xyz", "pt-BR") {

    // Migrated from a custom CMS to Madara.
    override val versionId = 2

    override val client: OkHttpClient = super.client.newBuilder()
        .addInterceptor(RateLimitInterceptor(1, 2, TimeUnit.SECONDS))
        .build()
}
