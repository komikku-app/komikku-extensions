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

object BannerInterceptor : Interceptor {
    private const val w = BANNER_WIDTH
    private const val h = BANNER_HEIGHT
    private const val size = w * h
    private const val threshold = w * h * 3 // 1 per pixel per channel
    private val bannerBuffer by lazy {
        val buffer = Base64.decode(BANNER_BASE64, Base64.DEFAULT)
        val banner = BitmapFactory.decodeByteArray(buffer, 0, buffer.size)
        val pixels = IntArray(size)
        banner.getPixels(pixels, 0, w, 0, 0, w, h)
        pixels
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val url = chain.request().url.toString()
        val response = chain.proceed(chain.request())
        if (!url.endsWith(COMIC_IMAGE_SUFFIX)) return response
        val body = response.body!!
        val contentType = body.contentType()
        val content = body.bytes()
        val bitmap = BitmapFactory.decodeByteArray(content, 0, content.size)
        val positions = checkBanner(bitmap)
        return if (positions == 0) {
            response.newBuilder().body(content.toResponseBody(contentType)).build()
        } else {
            val result = Bitmap.createBitmap(
                bitmap, 0, if (positions and TOP == TOP) h else 0,
                bitmap.width, bitmap.height - if (positions == BOTH) h * 2 else h
            )
            val output = ByteArrayOutputStream()
            result.compress(Bitmap.CompressFormat.JPEG, 90, output)
            val responseBody = output.toByteArray().toResponseBody("image/jpeg".toMediaType())
            response.newBuilder().body(responseBody).build()
        }
    }

    private fun checkBanner(image: Bitmap): Int {
        if (image.width < w || image.height < h) return 0
        if ((image.width - w) % 2 != 0) return 0
        val pad = (image.width - w) / 2
        val buf = IntArray(size)
        var result = 0
        image.getPixels(buf, 0, w, pad, 0, w, h) // top
        if (isIdentical(bannerBuffer, buf)) result = result or TOP
        image.getPixels(buf, 0, w, pad, image.height - h, w, h) // bottom
        if (isIdentical(bannerBuffer, buf)) result = result or BOTTOM
        return result
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

    private const val TOP = 0b01
    private const val BOTTOM = 0b10
    private const val BOTH = 0b11

    const val COMIC_IMAGE_SUFFIX = "#baozi"
}
