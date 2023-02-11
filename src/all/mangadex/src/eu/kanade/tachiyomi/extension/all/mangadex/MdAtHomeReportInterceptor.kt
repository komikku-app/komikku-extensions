package eu.kanade.tachiyomi.extension.all.mangadex

import android.util.Log
import eu.kanade.tachiyomi.extension.all.mangadex.dto.ImageReportDto
import eu.kanade.tachiyomi.network.POST
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import uy.kohesive.injekt.injectLazy
import java.io.IOException

/**
 * Interceptor to post to md@home for MangaDex Stats
 */
class MdAtHomeReportInterceptor(
    private val client: OkHttpClient,
    private val headers: Headers,
) : Interceptor {

    private val json: Json by injectLazy()

    private val mdAtHomeUrlRegex =
        Regex("""^https://[\w\d]+\.[\w\d]+\.mangadex(\b-test\b)?\.network.*${'$'}""")

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val response = chain.proceed(chain.request())
        val url = originalRequest.url.toString()

        if (!url.contains(mdAtHomeUrlRegex)) {
            return response
        }

        val result = ImageReportDto(
            url,
            success = response.isSuccessful,
            bytes = response.peekBody(Long.MAX_VALUE).bytes().size,
            cached = response.header("X-Cache", "") == "HIT",
            duration = response.receivedResponseAtMillis - response.sentRequestAtMillis,
        )

        val payload = json.encodeToString(result)

        val reportRequest = POST(
            url = MDConstants.atHomePostUrl,
            headers = headers,
            body = payload.toRequestBody(JSON_MEDIA_TYPE),
        )

        // Execute the report endpoint network call asynchronously to avoid blocking
        // the reader from showing the image once it's fully loaded if the report call
        // gets stuck, as it tend to happens sometimes.
        client.newCall(reportRequest).enqueue(
            object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e("MangaDex", "Error trying to POST report to MD@Home: ${e.message}")
                }

                override fun onResponse(call: Call, response: Response) {
                    response.close()
                }
            },
        )

        return response
    }

    companion object {
        private val JSON_MEDIA_TYPE = "application/json".toMediaType()
    }
}
