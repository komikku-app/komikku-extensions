package eu.kanade.tachiyomi.extension.en.koushoku

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.Response

class GoogleTranslateNetworkInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        if (
            response.request.url.host == "koushoku-org.translate.goog" &&
            response.request.url.pathSegments.getOrNull(0) == "d" &&
            response.code in 300..399
        ) {
            val newUrl = degooglify(response.headers["Location"]!!.toHttpUrl())

            return response.newBuilder()
                .header("Location", newUrl.toString())
                .build()
        }

        return response
    }

    private fun degooglify(url: HttpUrl): HttpUrl {
        val newHost = url.host
            .substringBeforeLast(".translate.goog")
            .replace('-', '.')
            .replace("..", "-")

        return url.newBuilder()
            .host(newHost)
            .removeAllQueryParameters("_x_tr_sl")
            .removeAllQueryParameters("_x_tr_tl")
            .removeAllQueryParameters("_x_tr_hl")
            .build()
    }
}
