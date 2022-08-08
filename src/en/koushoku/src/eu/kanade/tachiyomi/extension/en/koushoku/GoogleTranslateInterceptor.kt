package eu.kanade.tachiyomi.extension.en.koushoku

import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Response

class GoogleTranslateInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        return if (request.url.host == "koushoku.org") {
            val newUrl = googlify(request.url)
            val newRequest = request.newBuilder()
                .url(newUrl)
                .build()

            chain.proceed(newRequest)
        } else {
            chain.proceed(request)
        }
    }

    private fun googlify(url: HttpUrl): HttpUrl {
        val newHost = url.host
            .replace("-", "--")
            .replace('.', '-')
            .plus(".translate.goog")

        return url.newBuilder()
            .host(newHost)
            // `_x_tr_sl` and `_x_tr_tl` must be different
            .addQueryParameter("_x_tr_sl", "en")
            .addQueryParameter("_x_tr_tl", "jp")
            .addQueryParameter("_x_tr_hl", "en")
            .build()
    }
}
