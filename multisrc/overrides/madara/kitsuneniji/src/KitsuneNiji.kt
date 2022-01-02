package eu.kanade.tachiyomi.extension.pt.kitsuneniji

import eu.kanade.tachiyomi.lib.ratelimit.RateLimitInterceptor
import eu.kanade.tachiyomi.multisrc.madara.Madara
import okhttp3.OkHttpClient
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class KitsuneNiji : Madara(
    "Kitsune Niji",
    "https://kitsuneniji.online",
    "pt-BR",
    SimpleDateFormat("MMMMM d, yyyy", Locale("pt", "BR"))
) {

    override val client: OkHttpClient = super.client.newBuilder()
        .addInterceptor(RateLimitInterceptor(1, 2, TimeUnit.SECONDS))
        .build()
}
