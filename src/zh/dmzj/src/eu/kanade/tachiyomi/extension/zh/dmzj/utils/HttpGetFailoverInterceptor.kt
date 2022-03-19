package eu.kanade.tachiyomi.extension.zh.dmzj.utils

import android.util.Log
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response

/**
 * An OkHttp interceptor that will switch to a failover address and retry when an HTTP GET request
 * failed.
 *
 * Because failover addresses are provided per request, we use request headers to pass such info to
 * the interceptor. Headers used for indicating failover addresses will be deleted before the request
 * starts.
 */
class HttpGetFailoverInterceptor : Interceptor {
    companion object {
        const val RETRY_WITH_HEADER = "x-tachiyomi-retry-with"

        private const val LOG_TAG = "extension.zh.dmzj.utils"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()
        if (request.method != "GET") {
            return chain.proceed(request)
        }

        val retries = request.headers(RETRY_WITH_HEADER).mapNotNull { it.toHttpUrlOrNull() }.toList()
        if (retries.isNotEmpty()) {
            request = request.newBuilder().removeHeader(RETRY_WITH_HEADER).build()
        }

        for (retry in retries) {
            var response: Response? = null
            try {
                Log.d(LOG_TAG, "[HttpGetFailoverInterceptor] try for ${request.url}")
                response = chain.proceed(request)
                if (response.code < 400) {
                    return response
                }
                Log.d(LOG_TAG, "[HttpGetFailoverInterceptor] failed with http status ${response.code}, next: $retry")
            } catch (e: Exception) {
                Log.d(LOG_TAG, "[HttpGetFailoverInterceptor] failed with exception, next: $retry", e)
            }
            try {
                response?.close()
            } catch (_: Exception) {
                // Ignore exceptions
            }
            request = request.newBuilder().url(retry).build()
        }
        return chain.proceed(request)
    }
}
