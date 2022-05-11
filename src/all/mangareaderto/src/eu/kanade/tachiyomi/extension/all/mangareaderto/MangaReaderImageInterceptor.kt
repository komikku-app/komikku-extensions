package eu.kanade.tachiyomi.extension.all.mangareaderto

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Rect
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.io.ByteArrayOutputStream
import java.io.InputStream
import kotlin.math.ceil
import kotlin.math.floor

class MangaReaderImageInterceptor : Interceptor {

    private var s = IntArray(256)
    private var arc4i = 0
    private var arc4j = 0

    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())

        // shuffled page requests should have shuffled=true query parameter
        if (chain.request().url.queryParameter("shuffled") != "true")
            return response

        val image = unscrambleImage(response.body!!.byteStream())
        val body = image.toResponseBody("image/png".toMediaTypeOrNull())
        return response.newBuilder()
            .body(body)
            .build()
    }

    private fun unscrambleImage(image: InputStream): ByteArray {
        // obfuscated code (imgReverser function): https://mangareader.to/js/read.min.js
        // essentially, it shuffles arrays of the image slices using the key 'stay'

        val bitmap = BitmapFactory.decodeStream(image)

        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        val horizontalParts = ceil(bitmap.width / SLICE_SIZE.toDouble()).toInt()
        val totalParts = horizontalParts * ceil(bitmap.height / SLICE_SIZE.toDouble()).toInt()

        // calculate slices
        val slices: HashMap<Int, MutableList<Rect>> = hashMapOf()

        for (i in 0 until totalParts) {
            val row = floor(i / horizontalParts.toDouble()).toInt()

            val x = (i - row * horizontalParts) * SLICE_SIZE
            val y = row * SLICE_SIZE
            val width = if (x + SLICE_SIZE <= bitmap.width) SLICE_SIZE else bitmap.width - x
            val height = if (y + SLICE_SIZE <= bitmap.height) SLICE_SIZE else bitmap.height - y

            val srcRect = Rect(x, y, width, height)
            val key = width - height
            if (!slices.containsKey(key)) {
                slices[key] = mutableListOf()
            }
            slices[key]?.add(srcRect)
        }

        // handle groups of slices
        for (sliceEntry in slices) {
            // reset random number generator for every un-shuffle
            resetRng()

            val currentSlices = sliceEntry.value
            val sliceCount = currentSlices.count()

            // un-shuffle slice indices
            val orderedSlices = IntArray(sliceCount)
            val keys = MutableList(sliceCount) { it }

            for (i in currentSlices.indices) {
                val r = floor(prng() * keys.count()).toInt()
                val g = keys[r]
                keys.removeAt(r)
                orderedSlices[g] = i
            }

            // draw slices
            val cols = getColumnCount(currentSlices)

            val groupX = currentSlices[0].left
            val groupY = currentSlices[0].top

            for ((i, orderedIndex) in orderedSlices.withIndex()) {
                val slice = currentSlices[i]

                val row = floor((orderedIndex / cols).toDouble()).toInt()
                val col = orderedIndex - row * cols

                val width = slice.right
                val height = slice.bottom

                val x = groupX + col * width
                val y = groupY + row * height

                val srcRect = Rect(x, y, x + width, y + height)
                val dstRect = Rect(
                    slice.left,
                    slice.top,
                    slice.left + width,
                    slice.top + height
                )

                canvas.drawBitmap(bitmap, srcRect, dstRect, null)
            }
        }

        val output = ByteArrayOutputStream()
        result.compress(Bitmap.CompressFormat.PNG, 100, output)
        return output.toByteArray()
    }

    private fun getColumnCount(slices: List<Rect>): Int {
        if (slices.count() == 1) return 1
        var t: Int? = null
        for (i in slices.indices) {
            if (t == null) t = slices[i].top
            if (t != slices[i].top) {
                return i
            }
        }
        return slices.count()
    }

    private fun resetRng() {
        arc4i = 0
        arc4j = 0
        initializeS()
        arc4(256) // RC4-drop[256]
    }

    private fun initializeS() {
        val t = IntArray(256)
        for (i in 0..255) {
            s[i] = i
            t[i] = KEY[i % KEY.size]
        }
        var j = 0
        var tmp: Int
        for (i in 0..255) {
            j = (j + s[i] + t[i]) and 0xFF
            tmp = s[j]
            s[j] = s[i]
            s[i] = tmp
        }
    }

    private fun prng(): Double {
        var n = arc4(6)
        var d = 281474976710656.0 // 256^6 (start with 6 chunks in n)
        var x = 0L
        while (n < 4503599627370496) { // 2^52 (52 significant digits in a double)
            n = (n + x) * 256
            d *= 256
            x = arc4(1)
            if (n < 0) break // overflow
        }
        return (n + x) / d
    }

    private fun arc4(count: Int): Long {
        var t: Int
        var tmp: Int
        var r: Long = 0

        repeat(count) {
            arc4i = (arc4i + 1) and 0xFF
            arc4j = (arc4j + s[arc4i]) and 0xFF
            tmp = s[arc4j]
            s[arc4j] = s[arc4i]
            s[arc4i] = tmp
            t = (s[arc4i] + s[arc4j]) and 0xFF

            r = r * 256 + s[t]
        }

        return r
    }

    companion object {
        private val KEY = "stay".map { it.toByte().toInt() }
        private const val SLICE_SIZE = 200
    }
}
