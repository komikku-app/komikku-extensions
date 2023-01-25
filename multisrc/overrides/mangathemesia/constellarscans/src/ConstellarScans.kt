package eu.kanade.tachiyomi.extension.en.constellarscans

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Rect
import android.util.Log
import eu.kanade.tachiyomi.multisrc.mangathemesia.MangaThemesia
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.interceptor.rateLimit
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.CacheControl
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.ResponseBody.Companion.toResponseBody
import org.jsoup.nodes.Document
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.lang.IllegalArgumentException
import java.security.MessageDigest

class ConstellarScans : MangaThemesia("Constellar Scans", "https://constellarscans.com", "en") {

    override val client = super.client.newBuilder()
        .rateLimit(1, 3)
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
        }
        .build()

    override fun headersBuilder(): Headers.Builder = Headers.Builder()
        .add("Referer", "$baseUrl/")
        .add("Accept-Language", "en-US,en;q=0.9")
        .add("DNT", "1")
        .add("User-Agent", mobileUserAgent)
        .add("Upgrade-Insecure-Requests", "1")

    override val seriesStatusSelector = ".status"

    private val mobileUserAgent by lazy {
        val req = GET(UA_DB_URL)
        val data = client.newCall(req).execute().body!!.use {
            json.parseToJsonElement(it.string()).jsonArray
        }.mapNotNull {
            it.jsonObject["user-agent"]?.jsonPrimitive?.content?.takeIf { ua ->
                ua.startsWith("Mozilla/5.0") &&
                    (
                        ua.contains("iPhone") &&
                            (ua.contains("FxiOS") || ua.contains("CriOS")) ||
                            ua.contains("Android") &&
                            (ua.contains("EdgA") || ua.contains("Chrome") || ua.contains("Firefox"))
                        )
            }
        }
        data.random()
    }

    override fun pageListRequest(chapter: SChapter): Request =
        super.pageListRequest(chapter).newBuilder()
            .header(
                "Accept",
                "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9"
            )
            .header("Sec-Fetch-Site", "same-origin")
            .header("Sec-Fetch-Mode", "navigate")
            .header("Sec-Fetch-Dest", "document")
            .header("Sec-Fetch-User", "?1")
            .cacheControl(CacheControl.FORCE_NETWORK)
            .build()

    override fun pageListParse(document: Document): List<Page> {
        val obfuscatedCode = document.select("script:containsData(_0x)").html()
        val tsDataEncrypted = TS_DATA_RE.find(obfuscatedCode)?.groupValues?.get(1)
        if (tsDataEncrypted != null) {
            val descrambledData = descrambleString(tsDataEncrypted).trim()
            val match = DESCRAMBLING_KEY_RE.find(descrambledData)?.value
                ?: throw Exception("Did not receive valid decryption key. Try opening the chapter again.")
            Log.d("constellarscans", "device-limited chapter: $match")
            return decodeDeviceLimitedChapter(match)
        }

        val scripts = document.select("script").html()
        val tsData = JS_FUNC_RE.findAll(obfuscatedCode).firstNotNullOf {
            val func = it.groupValues[1]
            val tsDataFuncRe = Regex("""$func\s*\(\s*['"]([\da-z]+?)['"]\s*\)""", RegexOption.IGNORE_CASE)
            val match = tsDataFuncRe.find(scripts)?.groupValues?.get(1)
                ?: return@firstNotNullOf null
            descrambleString(match).trim()
        }
        val tsDataObject = json.parseToJsonElement(tsData).jsonObject
        return tsDataObject["sources"]!!.jsonArray[0].jsonObject["images"]!!.jsonArray.mapIndexed { index, jsonElement ->
            Page(index, imageUrl = jsonElement.jsonPrimitive.content.replace("http://", "https://"))
        }
    }

    override fun imageRequest(page: Page): Request = super.imageRequest(page).newBuilder()
        .header("Accept", "image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8")
        .header("Sec-Fetch-Dest", "image")
        .header("Sec-Fetch-Mode", "no-cors")
        .header("Sec-Fetch-Site", "same-origin")
        .build()

    private fun sumOfDigits(input: String): Int = input.sumOf { it.toString().toInt() }

    private fun descrambleString(input: String): String =
        input.replace(NOT_DIGIT_RE, "")
            .chunked(6)
            .map {
                val charCode = sumOfDigits(it.substring(0..2)) * 10 + sumOfDigits(it.substring(3)) + 32
                charCode.toChar()
            }
            .joinToString("")

    private fun decodeDeviceLimitedChapter(fullKey: String): List<Page> {
        if (!DESCRAMBLING_KEY_RE.matches(fullKey)) {
            throw IllegalArgumentException("Did not receive suitable decryption key. Try opening the chapter again.")
        }

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
            val filename = i.toString().padStart(5, '0')
            pages.add(
                Page(
                    i,
                    imageUrl = "$encodedUploadsPath/$fragment/$filename.webp#$DESCRAMBLE"
                )
            )
        }
        return pages
    }

    private fun descrambleImage(image: InputStream, key: String): ByteArray {
        val bitmap = BitmapFactory.decodeStream(image)
        val invertingPaint = Paint().apply {
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
        val md = MessageDigest.getInstance("MD5")
        return md.digest(input.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }

    private val encodedUploadsPath = "$baseUrl/wp-content/uploads/encoded"

    companion object {
        const val DESCRAMBLE = "descramble"
        const val UA_DB_URL =
            "https://cdn.jsdelivr.net/gh/mimmi20/browscap-helper@30a83c095688f40b9eaca0165a479c661e5a7fbe/tests/0002999.json"
        const val LOOKUP_STRING_ALNUM =
            "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
        val NOT_DIGIT_RE = Regex("""\D""")
        val JS_FUNC_RE = Regex("""function (.+?)\s*\(""")

        val TS_DATA_RE = Regex("""\(\s*['"]([\da-z]+?)['"]\s*\)""", RegexOption.IGNORE_CASE)

        // The decoding algorithm looks for a hex number in 32..33, so we write our regex accordingly
        val DESCRAMBLING_KEY_RE =
            Regex("""[\da-z]{32}[\da-f]{2}[\da-z]+""", RegexOption.IGNORE_CASE)
    }
}
