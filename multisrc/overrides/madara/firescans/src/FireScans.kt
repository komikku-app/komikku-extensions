package eu.kanade.tachiyomi.extension.en.firescans

import eu.kanade.tachiyomi.multisrc.madara.Madara
import eu.kanade.tachiyomi.network.interceptor.rateLimit
import okhttp3.OkHttpClient

class FireScans : Madara("Fire Scans", "https://firescans.xyz", "en") {

    override val client: OkHttpClient = super.client.newBuilder()
        .rateLimit(20, 5)
        .build()

    override val useNewChapterEndpoint: Boolean = true
}
