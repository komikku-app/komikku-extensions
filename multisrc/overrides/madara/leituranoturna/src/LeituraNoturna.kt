package eu.kanade.tachiyomi.extension.pt.leituranoturna

import eu.kanade.tachiyomi.multisrc.madara.Madara
import eu.kanade.tachiyomi.network.interceptor.rateLimit
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.internal.closeQuietly
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class LeituraNoturna : Madara(
    "Leitura Noturna",
    "https://leituranoturna.com",
    "pt-BR",
    SimpleDateFormat("dd 'de' MMMMM 'de' yyyy", Locale("pt", "BR"))
) {

    override val client: OkHttpClient = super.client.newBuilder()
        .rateLimit(1, 2, TimeUnit.SECONDS)
        .addInterceptor(::loginCheckIntercept)
        .build()

    // Page has custom link to scan website.
    override val popularMangaUrlSelector = "div.post-title a:not([target])"

    private fun loginCheckIntercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())

        if (response.request.url.toString().contains("wp-login")) {
            response.closeQuietly()
            throw IOException("Faça login pela WebView para utilizar a extensão.")
        }

        return response
    }
}
