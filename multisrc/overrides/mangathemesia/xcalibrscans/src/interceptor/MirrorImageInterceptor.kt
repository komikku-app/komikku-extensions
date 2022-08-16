package eu.kanade.tachiyomi.extension.en.xcalibrscans.interceptor

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.io.ByteArrayOutputStream

class MirrorImageInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        if (!chain.request().url.toString().endsWith(MIRRORED_IMAGE_SUFFIX)) {
            return chain.proceed(chain.request())
        }

        val imageUrl = chain.request().url.toString()
            .removeSuffix(MIRRORED_IMAGE_SUFFIX)

        val request = chain.request().newBuilder().url(imageUrl).build()
        val response = chain.proceed(request)

        val bitmap = BitmapFactory.decodeStream(response.body!!.byteStream())

        val result = bitmap.flipHorizontally()

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

    private fun Bitmap.flipHorizontally(): Bitmap {
        val matrix = Matrix().apply {
            postScale(
                -1F,
                1F,
                this@flipHorizontally.width / 2F,
                this@flipHorizontally.height / 2F
            )
        }
        return Bitmap.createBitmap(this, 0, 0, this.width, this.height, matrix, true)
    }
}

const val MIRRORED_IMAGE_SUFFIX = "?mirrored"

fun String.prepareMirrorImageForInterceptor(): String {
    return "$this$MIRRORED_IMAGE_SUFFIX"
}
