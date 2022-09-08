package eu.kanade.tachiyomi.extension.all.mangadex

import android.util.Log
import eu.kanade.tachiyomi.extension.all.mangadex.dto.ImageReportDto
import eu.kanade.tachiyomi.network.POST
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import uy.kohesive.injekt.injectLazy

/**
 * Interceptor to post to md@home for MangaDex Stats
 */
class MdAtHomeReportInterceptor(
    private val client: OkHttpClient,
    private val headers: Headers
) : Interceptor {

    private val json: Json by injectLazy()

    private val mdAtHomeUrlRegex =
        Regex("""^https://[\w\d]+\.[\w\d]+\.mangadex(\b-test\b)?\.network.*${'$'}""")

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        return chain.proceed(chain.request()).let { response ->
            val url = originalRequest.url.toString()
            if (url.contains(mdAtHomeUrlRegex)) {
                val byteSize = response.peekBody(Long.MAX_VALUE).bytes().size
                val duration = response.receivedResponseAtMillis - response.sentRequestAtMillis
                val cache = response.header("X-Cache", "") == "HIT"
                val result = ImageReportDto(
                    url,
                    response.isSuccessful,
                    byteSize,
                    cache,
                    duration
                )

                val jsonString = json.encodeToString(result)

                try {
                    client.newCall(
                        POST(
                            MDConstants.atHomePostUrl,
                            headers,
                            jsonString.toRequestBody("application/json".toMediaType())
                        )
                    ).execute().close()
                } catch (e: Exception) {
                    Log.e("MangaDex", "Error trying to POST report to MD@Home: ${e.message}")
                }
            }

            response
        }
    }
}
