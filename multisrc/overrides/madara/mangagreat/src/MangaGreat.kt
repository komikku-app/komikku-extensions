package eu.kanade.tachiyomi.extension.en.mangagreat

import android.util.Base64
import app.cash.quickjs.QuickJs
import eu.kanade.tachiyomi.multisrc.madara.Madara
import eu.kanade.tachiyomi.util.asJsoup
import okhttp3.Cookie
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import java.io.IOException
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class MangaGreat : Madara("MangaGreat", "https://mangagreat.com", "en") {
    override val client: OkHttpClient = super.client.newBuilder()
        .addInterceptor(::JSChallengeInterceptor)
        .build()

    // The website does not flag the content.
    override val filterNonMangaItems = false

    // /manga/page/1/ redirects to /manga/
    override fun searchPage(page: Int): String = if (page == 1) "" else "page/$page/"

    //
    // JS Challenge logic start
    //
    @Suppress("FunctionName")
    private fun JSChallengeInterceptor(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        if (response.code != 202) return response
        val url = request.url

        Cookie.parse(url, getJSChallengeCookie(response))
            ?.let { client.cookieJar.saveFromResponse(url, listOf(it)) }
            ?: throw IOException("Failed JavaScript challenge. Check WebView.")

        return client.newCall(request).execute()
    }

    private fun getJSChallengeCookie(response: Response): String {
        val document = response.asJsoup()

        val jsPatch = ";function atob(a){return base64.atob(a)};document.cookie"
        val jsPayload = document.select("script")
            .joinToString("\n") { it.data() }
            .trimIndent() + jsPatch

        val fauxBase64 = FauxBase64()
        val fauxLocation = FauxLocation()
        val fauxDocument = FauxDocument()
        val slowAES = FauxSlowAES()

        QuickJs.create().use { context ->
            context.set("base64", FauxBase64Interface::class.java, fauxBase64)
            context.set("location", FauxLocationInterface::class.java, fauxLocation)
            context.set("document", FauxDocumentInterface::class.java, fauxDocument)
            context.set("slowAES", FauxSlowAESInterface::class.java, slowAES)

            return context.evaluate(jsPayload) as String
        }
    }

    class FauxBase64 : FauxBase64Interface {
        override fun atob(base64: String): String {
            return String(Base64.decode(base64, Base64.DEFAULT))
        }
    }

    class FauxLocation : FauxLocationInterface {
        override var href: String = ""
    }

    class FauxDocument : FauxDocumentInterface {
        override var cookie: String = ""
    }

    class FauxSlowAES : FauxSlowAESInterface {
        private fun Array<Int>.toByteArray(): ByteArray {
            return map { it.toByte() }.toByteArray()
        }

        private fun ByteArray.toTypedArray(): Array<Int> {
            return map { it.toInt() and 0xFF }.toTypedArray()
        }

        override fun decrypt(cipherIn: Array<Int>, mode: Int, key: Array<Int>, iv: Array<Int>): Array<Int> {
            val modeStr = when (mode) {
                0 -> "OFB"
                1 -> "CFB"
                else -> "CBC" // 2 = CBC, 3+ = unknown
            }

            val cipher = Cipher.getInstance("AES/$modeStr/NoPadding")
            val keyS = SecretKeySpec(key.toByteArray(), "AES")
            cipher.init(Cipher.DECRYPT_MODE, keyS, IvParameterSpec(iv.toByteArray()))

            return cipher.doFinal(cipherIn.toByteArray()).toTypedArray()
        }
    }

    interface FauxBase64Interface {
        fun atob(base64: String): String
    }

    interface FauxLocationInterface {
        val href: String
    }

    private interface FauxDocumentInterface {
        val cookie: String
    }

    @Suppress("unused")
    private interface FauxSlowAESInterface {
        fun decrypt(cipherIn: Array<Int>, mode: Int, key: Array<Int>, iv: Array<Int>): Array<Int>
    }

    //
    // JS Challenge logic end
    //
}
