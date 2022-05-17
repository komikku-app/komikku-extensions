package eu.kanade.tachiyomi.extension.zh.baozimanhua

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.io.ByteArrayOutputStream
import kotlin.math.abs

class BannerInterceptor : Interceptor {
    private val banner: Bitmap by lazy {
        val buffer = Base64.decode(BANNER_BASE64, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(buffer, 0, buffer.size)
    }
    private val w by lazy { banner.width }
    private val h by lazy { banner.height }
    private val size by lazy { w * h }
    private val bannerBuffer by lazy {
        val buffer = IntArray(size)
        banner.getPixels(buffer, 0, w, 0, 0, w, h)
        banner.recycle()
        buffer
    }
    private val threshold by lazy { w * h * 3 } // 1 per pixel per channel

    override fun intercept(chain: Interceptor.Chain): Response {
        val url = chain.request().url.toString()
        val response = chain.proceed(chain.request())
        if (!url.endsWith(COMIC_IMAGE_SUFFIX)) return response
        val body = response.body!!
        val contentType = body.contentType()
        val content = body.bytes()
        val bitmap = BitmapFactory.decodeByteArray(content, 0, content.size)
        val position = checkBanner(bitmap)
        return if (position == null) {
            response.newBuilder().body(content.toResponseBody(contentType)).build()
        } else {
            val result = Bitmap.createBitmap(
                bitmap, 0,
                when (position) {
                    BannerPosition.TOP -> h
                    BannerPosition.BOTTOM -> 0
                },
                bitmap.width, bitmap.height - h
            )
            val output = ByteArrayOutputStream()
            result.compress(Bitmap.CompressFormat.JPEG, 90, output)
            val responseBody = output.toByteArray().toResponseBody("image/jpeg".toMediaType())
            response.newBuilder().body(responseBody).build()
        }
    }

    private fun checkBanner(image: Bitmap): BannerPosition? {
        if (image.width < w || image.height < h) return null
        if ((image.width - w) % 2 != 0) return null
        val pad = (image.width - w) / 2
        val buf = IntArray(size)
        image.getPixels(buf, 0, w, pad, 0, w, h) // top
        if (isIdentical(bannerBuffer, buf)) return BannerPosition.TOP
        image.getPixels(buf, 0, w, pad, image.height - h, w, h) // bottom
        if (isIdentical(bannerBuffer, buf)) return BannerPosition.BOTTOM
        return null
    }

    private fun isIdentical(a: IntArray, b: IntArray): Boolean {
        var diff = 0
        for (i in 0 until size) {
            val pixel0 = a[i]
            val pixel1 = b[i]
            diff += abs((pixel0 and 0xFF) - (pixel1 and 0xFF))
            diff += abs((pixel0 shr 8 and 0xFF) - (pixel1 shr 8 and 0xFF))
            diff += abs((pixel0 shr 16 and 0xFF) - (pixel1 shr 16 and 0xFF))
            if (diff > threshold) return false
        }
        return true
    }

    private enum class BannerPosition { TOP, BOTTOM }
}

const val COMIC_IMAGE_SUFFIX = "#baozi"
