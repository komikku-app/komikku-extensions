package eu.kanade.tachiyomi.extension.en.immortalupdates

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Rect
import app.cash.quickjs.QuickJs
import eu.kanade.tachiyomi.multisrc.madara.Madara
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.interceptor.rateLimit
import eu.kanade.tachiyomi.source.model.Page
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.jsoup.nodes.Document
import java.io.ByteArrayOutputStream
import java.io.InputStream

class ImmortalUpdates : Madara("Immortal Updates", "https://immortalupdates.com", "en") {

    override val useNewChapterEndpoint: Boolean = true

    override val client = super.client.newBuilder()
        .rateLimit(1, 2)
        .addInterceptor { chain ->
            val response = chain.proceed(chain.request())

            if (response.request.url.fragment?.contains(DESCRAMBLE) != true) {
                return@addInterceptor response
            }
            val fragment = response.request.url.fragment!!
            val args = fragment.substringAfter("$DESCRAMBLE=").split(",")

            val image = unscrambleImage(response.body!!.byteStream(), args)
            val body = image.toResponseBody("image/jpeg".toMediaTypeOrNull())
            return@addInterceptor response.newBuilder()
                .body(body)
                .build()
        }.build()

    override fun pageListParse(document: Document): List<Page> {
        val pageList = super.pageListParse(document).toMutableList()

        val unscramblingCallsPage = pageList.firstOrNull { it.imageUrl!!.contains("00-call") }
            ?: return pageList

        val unscramblingCalls = client.newCall(GET(unscramblingCallsPage.imageUrl!!, headers))
            .execute()
            .use { it.body!!.string() }

        unscramblingCalls.replace("\r", "").split("\n").forEach {
            val args = unfuckJs(it)
                .substringAfter("get_img(")
                .substringBefore(")")

            val filenameFragment = args.split(",")[0].removeSurrounding("'")
            val page = pageList.firstOrNull { page -> page.imageUrl!!.contains(filenameFragment, ignoreCase = true) }
                ?: return@forEach
            val newPageUrl = page.imageUrl!!.toHttpUrl().newBuilder()
                .fragment("$DESCRAMBLE=$args")
                .build()
                .toString()
            pageList[page.index] = Page(page.index, document.location(), newPageUrl)
        }
        pageList.remove(unscramblingCallsPage)
        return pageList
    }

    // Converted from _0x3bc005: Find the CanvasRenderingContext2D.drawImage call basically
    //
    // `args` is the arguments of the original get_img call:
    //     get_img(file_to_match, indexer, iterations, sectionWidth, sectionHeight, isBackgroundBlack, shouldFillColor, ???, key, keyAddition, ???)
    //
    // The boolean after shouldFillColor seems to always be `false` so I have optimized it out for now.
    // If it fucks up someone will make an issue anyways /shrug
    //
    // I assumed the last argument was to check if versions match or something (since it was 1.0.1)
    // but it was used in some canvas thingy that I didn't bother to check
    private fun unscrambleImage(image: InputStream, args: List<String>): ByteArray {
        val indexer = args[1].toInt()
        val iterations = args[2].toInt()
        val sectionWidth = args[3].toInt()
        val sectionHeight = args[4].toInt()
        val isBackgroundBlack = args[5] == "true"
        val shouldFillColor = args[6] == "true"
        val key = args[8].toInt()
        val keyAddition = args[9].toInt()

        val bitmap = BitmapFactory.decodeStream(image)

        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        val heightSectionCount = bitmap.height / sectionHeight
        val widthSectionCount = bitmap.width / sectionWidth
        val sectionCount = heightSectionCount * widthSectionCount
        val descramblingArray = createDescramblingArray(indexer, sectionCount, key, keyAddition, iterations)

        if (shouldFillColor) {
            val backgroundColor = if (isBackgroundBlack) Color.BLACK else Color.WHITE
            canvas.drawColor(backgroundColor)
        }

        var i = 0
        for (vertical in 0 until heightSectionCount) {
            for (horizontal in 0 until widthSectionCount) {
                val swap = descramblingArray[i]

                val baseHeight = swap.floorDiv(widthSectionCount)
                val baseWidth = swap - baseHeight * widthSectionCount

                val dx = baseWidth * sectionWidth
                val dy = baseHeight * sectionHeight

                val sx = horizontal * sectionWidth
                val sy = vertical * sectionHeight

                val srcRect = Rect(sx, sy, sx + sectionWidth, sy + sectionHeight)
                val dstRect = Rect(dx, dy, dx + sectionWidth, dy + sectionHeight)

                canvas.drawBitmap(bitmap, srcRect, dstRect, null)
                i += 1
            }
        }

        if (isBackgroundBlack) {
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
            canvas.drawBitmap(result, 0f, 0f, invertingPaint)
        }

        val output = ByteArrayOutputStream()
        result.compress(Bitmap.CompressFormat.JPEG, 90, output)

        return output.toByteArray()
    }

    // Converted from _0x144afb
    // This should be called a little bit before the drawImage calls
    private fun createDescramblingArray(indexer: Int, size: Int, key: Int, keyAddition: Int, iterations: Int = 2): List<Int> {
        var indexerMut = indexer
        val returnArray = mutableListOf<Int>()

        for (i in 0 until size) {
            returnArray.add(i)
        }

        for (i in 0 until size) {
            for (o in 0 until iterations) {
                indexerMut = (indexerMut * key + keyAddition) % size

                val tmp = returnArray[indexerMut]
                returnArray[indexerMut] = returnArray[i]
                returnArray[i] = tmp
            }
        }

        return returnArray
    }

    private fun unfuckJs(jsf: String): String {
        // String: ([]+[])
        // fontcolor: (![]+[])[+[]]+({}+[])[+!![]]+([][[]]+[])[+!![]]+(!![]+[])[+[]]+({}+[])[!![]+!![]+!![]+!![]+!![]]+({}+[])[+!![]]+(![]+[])[!![]+!![]]+({}+[])[+!![]]+(!![]+[])[+!![]]
        // "undefined": []+[][[]]
        // Quick hack so QuickJS doesn't complain about function being called with no args
        val input = jsf.replace(
            "([]+[])[(![]+[])[+[]]+({}+[])[+!![]]+([][[]]+[])[+!![]]+(!![]+[])[+[]]+({}+[])[!![]+!![]+!![]+!![]+!![]]+({}+[])[+!![]]+(![]+[])[!![]+!![]]+({}+[])[+!![]]+(!![]+[])[+!![]]]()",
            "([]+[])[(![]+[])[+[]]+({}+[])[+!![]]+([][[]]+[])[+!![]]+(!![]+[])[+[]]+({}+[])[!![]+!![]+!![]+!![]+!![]]+({}+[])[+!![]]+(![]+[])[!![]+!![]]+({}+[])[+!![]]+(!![]+[])[+!![]]]([]+[][[]])",
        )
        return QuickJs.create().use {
            it.execute(jsfBoilerplate)
            it.evaluate(input.removePrefix("[]").removeSuffix("()") + "[0]").toString()
        }
    }

    private val jsfBoilerplate: ByteArray by lazy {
        QuickJs.create().use {
            it.compile(
                """
                class Location {
                    constructor(href) {
                        this.href = href
                    }

                    toString() {
                        return this.href
                    }
                }
                this.location = new Location("https://");
                """.trimIndent(),
                "?"
            )
        }
    }

    companion object {
        const val DESCRAMBLE = "descramble"
    }
}
