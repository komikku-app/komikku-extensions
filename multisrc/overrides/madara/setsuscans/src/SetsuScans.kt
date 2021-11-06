package eu.kanade.tachiyomi.extension.en.setsuscans

import eu.kanade.tachiyomi.lib.ratelimit.RateLimitInterceptor
import eu.kanade.tachiyomi.multisrc.madara.Madara

class SetsuScans : Madara("Setsu Scans", "https://setsuscans.com", "en") {
    override val client = super.client.newBuilder()
        .addInterceptor(RateLimitInterceptor(1, 2))
        .build()
}
