package eu.kanade.tachiyomi.extension.en.imperfectcomics

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import eu.kanade.tachiyomi.multisrc.madara.Madara
import eu.kanade.tachiyomi.source.model.Page
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.jsoup.nodes.Document
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit

class ImperfectComics : Madara("Imperfect Comics", "https://imperfectcomic.com", "en", SimpleDateFormat("yyyy-MM-dd")) {

    override val useNewChapterEndpoint: Boolean = true

    override val client: OkHttpClient = network.cloudflareClient.newBuilder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(::imageIntercept)
        .build()

    override fun pageListParse(document: Document): List<Page> {
        val mangaId = document.select("#manga-reading-nav-head").attr("data-id")
        val chapterId = document.select("#wp-manga-current-chap").attr("data-id")
        val mangaRegex = """div\[data-id=\"$mangaId\"(?:.|\n)*?(?=\})""".toRegex()
        val chapterRegex = """input\[data-id=\"$chapterId\"(?:.|\n)*?(?=\})""".toRegex()
        val css = document.selectFirst("#wp-custom-css").html()

        //Checking for chapter first to handle mirrored manga with specific un-mirrored chapters
        val props = chapterRegex.find(css).let { cId ->
            cId?.value ?: mangaRegex.find(css).let { mId ->
                mId?.value ?: ""
            }
        }

        countViews(document)

        return document.select(pageListParseSelector).mapIndexed { index, element ->
            val imageUrl = element.selectFirst("img")?.let {
                it.absUrl(if (it.hasAttr("data-src")) "data-src" else "src")
            }!!.toHttpUrlOrNull()!!.newBuilder()

            if (props.contains("transform: scaleX(-1)"))
                imageUrl.addQueryParameter("mirror", "1")
            if (props.contains("invert"))
                imageUrl.addQueryParameter("invert", "1")

            Page(index, document.location(), imageUrl.toString())
        }
    }

    private fun imageIntercept(chain: Interceptor.Chain): Response {
        var request = chain.request()
        val mirror = request.url.queryParameter("mirror") != null
        val invert = request.url.queryParameter("invert") != null

        if (!(mirror || invert)) {
            return chain.proceed(request)
        }

        val newUrl = request.url.newBuilder()
            .removeAllQueryParameters("mirror")
            .removeAllQueryParameters("invert")
            .build()
        request = request.newBuilder().url(newUrl).build()

        val response = chain.proceed(request)
        val image = processImage(response.body!!.byteStream(), mirror, invert)
        val body = image.toResponseBody("image/png".toMediaTypeOrNull())

        response.close()

        return response.newBuilder().body(body).build()
    }

    private fun processImage(image: InputStream, mirror: Boolean, invert: Boolean): ByteArray {
        var input = BitmapFactory.decodeStream(image)
        val matrix = Matrix().apply { postScale(-1F, 1F, input.width / 2F, input.height / 2F) }
        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(
                ColorMatrix().apply {
                    set(
                        floatArrayOf(
                            -1f, 0f, 0f, 0f, 255f,
                            0f, -1f, 0f, 0f, 255f,
                            0f, 0f, -1f, 0f, 255f,
                            0f, 0f, 0f, 1f, 0f
                        )
                    )
                }
            )
        }

        if (mirror)
            input = Bitmap.createBitmap(input, 0, 0, input.width, input.height, matrix, true)
        if (invert)
            Canvas(input).drawBitmap(input, 0f, 0f, paint)

        val output = ByteArrayOutputStream()
        input.compress(Bitmap.CompressFormat.PNG, 100, output)
        return output.toByteArray()
    }
}
