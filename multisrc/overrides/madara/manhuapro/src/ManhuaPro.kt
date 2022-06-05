package eu.kanade.tachiyomi.extension.en.manhuapro

import eu.kanade.tachiyomi.multisrc.madara.Madara
import eu.kanade.tachiyomi.network.interceptor.rateLimit
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class ManhuaPro : Madara(
    "ManhuaPro",
    "https://manhuapro.com",
    "en"
) {

    override val client: OkHttpClient = super.client.newBuilder()
        .rateLimit(1, 1, TimeUnit.SECONDS)
        .build()
}
