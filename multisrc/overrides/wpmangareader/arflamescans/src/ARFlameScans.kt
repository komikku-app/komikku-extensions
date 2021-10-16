package eu.kanade.tachiyomi.extension.ar.arflamescans

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Rect
import eu.kanade.tachiyomi.lib.ratelimit.RateLimitInterceptor
import eu.kanade.tachiyomi.multisrc.wpmangareader.WPMangaReader
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SManga
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.jsoup.nodes.Document
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class ARFlameScans : WPMangaReader(
    "AR FlameScans",
    "https://ar.flamescans.org",
    "ar",
    mangaUrlDirectory = "/series",
    dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.US)
) {

    override val client: OkHttpClient = network.cloudflareClient.newBuilder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(::composedImageIntercept)
        .addInterceptor(RateLimitInterceptor(1, 1, TimeUnit.SECONDS))
        .build()

    override fun parseStatus(status: String) = when {
        status.contains("مستمر") -> SManga.ONGOING
        status.contains("مكتمل") -> SManga.COMPLETED
        else -> SManga.UNKNOWN
    }

    private val composedSelector: String = "#readerarea div.figure_container div.composed_figure"

    override fun pageListParse(document: Document): List<Page> {
        val hasSplitImages = document
            .select(composedSelector)
            .firstOrNull() != null

        if (!hasSplitImages) {
            return super.pageListParse(document)
        }

        return document.select("#readerarea p:has(img), $composedSelector")
            .filter {
                it.select("img").all { imgEl ->
                    imgEl.attr("abs:src").isNullOrEmpty().not()
                }
            }
            .mapIndexed { i, el ->
                if (el.tagName() == "p") {
                    Page(i, "", el.select("img").attr("abs:src"))
                } else {
                    val imageUrls = el.select("img")
                        .joinToString("|") { it.attr("abs:src") }

                    Page(i, "", imageUrls + COMPOSED_SUFFIX)
                }
            }
    }

    private fun composedImageIntercept(chain: Interceptor.Chain): Response {
        if (!chain.request().url.toString().endsWith(COMPOSED_SUFFIX)) {
            return chain.proceed(chain.request())
        }

        val imageUrls = chain.request().url.toString()
            .removeSuffix(COMPOSED_SUFFIX)
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

        val responseBody = output.toByteArray().toResponseBody(MEDIA_TYPE)

        return Response.Builder()
            .code(200)
            .protocol(Protocol.HTTP_1_1)
            .request(chain.request())
            .message("OK")
            .body(responseBody)
            .build()
    }

    companion object {

        private const val COMPOSED_SUFFIX = "?comp"
        private val MEDIA_TYPE = "image/png".toMediaType()
    }
}
