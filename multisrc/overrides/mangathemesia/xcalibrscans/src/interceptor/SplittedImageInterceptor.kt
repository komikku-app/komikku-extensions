package eu.kanade.tachiyomi.extension.en.xcalibrscans.interceptor

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Rect
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.io.ByteArrayOutputStream

class SplittedImageInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        if (!chain.request().url.toString().endsWith(SPLITTED_IMAGE_SUFFIX)) {
            return chain.proceed(chain.request())
        }

        val imageUrls = chain.request().url.toString()
            .removeSuffix(SPLITTED_IMAGE_SUFFIX)
            .split("%7C")

        var width = 0
        var height = 0

        val imageBitmaps = imageUrls.map { imageUrl ->
            val request = chain.request().newBuilder().url(imageUrl).build()
            val response = chain.proceed(request)

            val bitmap = BitmapFactory.decodeStream(response.body!!.byteStream())

            width += bitmap.width
            height = bitmap.height

            bitmap
        }

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        var left = 0

        imageBitmaps.forEach { bitmap ->
            val srcRect = Rect(0, 0, bitmap.width, bitmap.height)
            val dstRect = Rect(left, 0, left + bitmap.width, bitmap.height)

            canvas.drawBitmap(bitmap, srcRect, dstRect, null)

            left += bitmap.width
        }

        val output = ByteArrayOutputStream()
        result.compress(Bitmap.CompressFormat.PNG, 100, output)

        val responseBody = output.toByteArray().toResponseBody("image/png".toMediaType())

        return Response.Builder()
            .code(200)
            .protocol(Protocol.HTTP_1_1)
            .request(chain.request())
            .message("OK")
            .body(responseBody)
            .build()
    }
}

const val SPLITTED_IMAGE_SUFFIX = "?splitted"

fun List<String>.prepareSplittedImageForInterceptor(): String {
    return "${this.joinToString("|")}$SPLITTED_IMAGE_SUFFIX"
}
