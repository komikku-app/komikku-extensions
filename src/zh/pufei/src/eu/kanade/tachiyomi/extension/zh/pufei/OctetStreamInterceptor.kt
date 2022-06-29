package eu.kanade.tachiyomi.extension.zh.pufei

import eu.kanade.tachiyomi.network.GET
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Response
import okhttp3.ResponseBody.Companion.asResponseBody

object OctetStreamInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        if (response.header("Content-Type") != "application/octet-stream") {
            return response
        }

        if (response.header("Content-Length")!!.toInt() < 100) { // usually 96
            // The actual URL is '/.../xxx.jpg/0'.
            val peek = response.peekBody(100).string()
            if (peek.startsWith("The actual URL")) {
                response.body!!.close()
                val actualPath = peek.substringAfter('\'').substringBeforeLast('\'')
                return chain.proceed(GET("https://manhua.acimg.cn$actualPath"))
            }
        }

        val url = request.url.encodedPath
        val mediaType = when {
            url.endsWith(".h") -> webpMediaType
            url.contains(".jpg") -> jpegMediaType
            else -> return response
        }
        val body = response.body!!.source().asResponseBody(mediaType)
        return response.newBuilder().body(body).build()
    }

    private val jpegMediaType = "image/jpeg".toMediaType()
    private val webpMediaType = "image/webp".toMediaType()
}
