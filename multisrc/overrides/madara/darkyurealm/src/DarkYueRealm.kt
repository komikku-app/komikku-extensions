package eu.kanade.tachiyomi.extension.pt.darkyurealm

import eu.kanade.tachiyomi.lib.ratelimit.RateLimitInterceptor
import eu.kanade.tachiyomi.multisrc.madara.Madara
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.SManga
import okhttp3.OkHttpClient
import okhttp3.Request
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class DarkYueRealm : Madara(
    "DarkYue Realm",
    "https://darkyuerealm.site/web",
    "pt-BR",
    SimpleDateFormat("dd 'de' MMMMM, yyyy", Locale("pt", "BR"))
) {

    // Override the id because the name was wrong.
    override val id: Long = 593455310609863709

    override val client: OkHttpClient = super.client.newBuilder()
        .addInterceptor(RateLimitInterceptor(1, 2, TimeUnit.SECONDS))
        .build()

    override fun mangaDetailsRequest(manga: SManga): Request {
        return GET(baseUrl + manga.url.removePrefix("/web"), headers)
    }

    override fun chapterListRequest(manga: SManga): Request {
        return GET(baseUrl + manga.url.removePrefix("/web"), headers)
    }
}
