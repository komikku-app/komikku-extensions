package eu.kanade.tachiyomi.extension.zh.bainianmanga

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Response
import okhttp3.ResponseBody.Companion.asResponseBody

object MimeInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)
        if (response.header("Content-Type") == "application/octet-stream" &&
            request.url.toString().contains(".jpg")
        ) {
            val body = response.body!!.source().asResponseBody(jpegMime)
            return response.newBuilder().body(body).build()
        }
        return response
    }

    private val jpegMime = "image/jpeg".toMediaType()
}
