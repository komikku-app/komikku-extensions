package eu.kanade.tachiyomi.extension.en.constellarscans

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Rect
import eu.kanade.tachiyomi.multisrc.mangathemesia.MangaThemesia
import eu.kanade.tachiyomi.network.interceptor.rateLimit
import eu.kanade.tachiyomi.source.model.Page
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.jsoup.nodes.Document
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.security.MessageDigest

class ConstellarScans : MangaThemesia("Constellar Scans", "https://constellarscans.com", "en") {

    override val client = super.client.newBuilder()
        .rateLimit(1, 2)
        .addInterceptor { chain ->
            val response = chain.proceed(chain.request())

            val url = response.request.url
            if (url.fragment?.contains(DESCRAMBLE) != true) {
                return@addInterceptor response
            }

            val segments = url.pathSegments
            val filenameWithoutExtension = segments.last().split(".")[0]
            val fragment = segments[segments.lastIndex - 1]
            val key = md5sum(fragment + filenameWithoutExtension)

            val image = descrambleImage(response.body!!.byteStream(), key)
            val body = image.toResponseBody("image/jpeg".toMediaTypeOrNull())
            response.newBuilder()
                .body(body)
                .build()
        }.build()

    override val seriesStatusSelector = ".status"

    override fun pageListParse(document: Document): List<Page> {
        val pageList = super.pageListParse(document)
        return when {
            document.selectFirst("script:containsData(_code)") != null -> descramblePageUrls(
                pageList
            )
            document.selectFirst("script:containsData(ts_reader[_)") != null -> decodeDeviceLimitedChapter(
                document
            )
            else -> pageList
        }
    }

    private fun descramblePageUrls(pages: List<Page>): List<Page> {
        return pages.map {
            val lastSegment = it.imageUrl!!.split('/').last()
            val filename = lastSegment
                .replace(NOT_DIGIT_RE, "")
                .chunked(2)
                .joinToString("") { LOOKUP_STRING[it.toInt()].toString() }
            Page(it.index, imageUrl = it.imageUrl!!.replace(lastSegment, filename))
        }
    }

    private fun decodeDeviceLimitedChapter(document: Document): List<Page> {
        val script = document.selectFirst("script:containsData(ts_reader[_)").data()
        val fullKey = DESCRAMBLING_KEY_RE.find(script)?.groupValues?.get(1)
            ?: throw Exception("Could not find suitable decryption key. Try opening the chapter again.")

        val shiftBy = fullKey.substring(32..33).toInt(16)
        val key = fullKey.substring(0..31) + fullKey.substring(34)

        val fragmentAndImageCount = key.map {
            var idx = LOOKUP_STRING_ALNUM.indexOf(it) - shiftBy
            if (idx < 0) {
                idx += LOOKUP_STRING_ALNUM.length
            }
            LOOKUP_STRING_ALNUM[idx]
        }.joinToString("")
        val fragment = fragmentAndImageCount.substring(0..31)
        val imageCount = fragmentAndImageCount.substring(32).toInt()

        val pages = mutableListOf<Page>()
        for (i in 1..imageCount) {
            pages.add(
                Page(
                    i,
                    imageUrl = "$encodedUploadsPath/$fragment/${i.toString().padStart(5, '0')}.webp#$DESCRAMBLE"
                )
            )
        }
        return pages
    }

    private fun descrambleImage(image: InputStream, key: String): ByteArray {
        val bitmap = BitmapFactory.decodeStream(image)

        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        val sectionCount = (key.last().code % 10) * 2 + 4
        val remainder = bitmap.height % sectionCount
        for (i in 0 until sectionCount) {
            var sectionHeight = bitmap.height / sectionCount
            var sy = bitmap.height - sectionHeight * (i + 1) - remainder
            val dy = sectionHeight * i

            if (i == sectionCount - 1) {
                sectionHeight += remainder
            } else {
                sy += remainder
            }

            val sRect = Rect(0, sy, bitmap.width, sy + sectionHeight)
            val dRect = Rect(0, dy, bitmap.width, dy + sectionHeight)
            canvas.drawBitmap(bitmap, sRect, dRect, invertingPaint)
        }

        val output = ByteArrayOutputStream()
        result.compress(Bitmap.CompressFormat.JPEG, 90, output)

        return output.toByteArray()
    }

    private fun md5sum(input: String): String {
        val digest = MessageDigest.getInstance("MD5")
        digest.update(input.toByteArray())
        return digest.digest()
            .asUByteArray()
            .joinToString("") { it.toString(16).padStart(2, '0') }
    }

    private val invertingPaint = Paint().apply {
        colorFilter = ColorMatrixColorFilter(
            ColorMatrix(
                floatArrayOf(
                    -1.0f, 0.0f, 0.0f, 0.0f, 255.0f,
                    0.0f, -1.0f, 0.0f, 0.0f, 255.0f,
                    0.0f, 0.0f, -1.0f, 0.0f, 255.0f,
                    0.0f, 0.0f, 0.0f, 1.0f, 0.0f
                )
            )
        )
    }

    private val encodedUploadsPath = "$baseUrl/wp-content/uploads/encoded"

    companion object {
        const val DESCRAMBLE = "descramble"
        const val LOOKUP_STRING =
            " !\"#${'$'}%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}"
        const val LOOKUP_STRING_ALNUM =
            "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
        val NOT_DIGIT_RE = Regex("""\D""")

        // The decoding algorithm looks for a hex number in 32..33, so we write our regex accordingly
        val DESCRAMBLING_KEY_RE = Regex("""'([\da-z]{32}[\da-f]{2}[\da-z]+)'""", RegexOption.IGNORE_CASE)
    }
}
