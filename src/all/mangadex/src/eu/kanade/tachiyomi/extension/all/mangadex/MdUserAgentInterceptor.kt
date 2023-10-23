package eu.kanade.tachiyomi.extension.all.mangadex

import android.content.SharedPreferences
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * Interceptor to set custom useragent for MangaDex
 */
class MdUserAgentInterceptor(
    private val preferences: SharedPreferences,
    private val dexLang: String,
) : Interceptor {

    private val SharedPreferences.customUserAgent
        get() = getString(
            MDConstants.getCustomUserAgentPrefKey(dexLang),
            MDConstants.defaultUserAgent,
        )

    private fun getUserAgent(): String? {
        return preferences.customUserAgent
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        try {
            val originalRequest = chain.request()

            val newUserAgent = getUserAgent() ?: return chain.proceed(originalRequest)

            val originalHeaders = originalRequest.headers

            val modifiedHeaders = originalHeaders.newBuilder()
                .set("User-Agent", newUserAgent)
                .build()

            return chain.proceed(
                originalRequest.newBuilder()
                    .headers(modifiedHeaders)
                    .build(),
            )
        } catch (e: Exception) {
            throw IOException("MdUserAgentInterceptor failed with error: ${e.message}")
        }
    }
}
