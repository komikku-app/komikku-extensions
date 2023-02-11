package eu.kanade.tachiyomi.extension.pt.amuy

import android.util.Base64
import eu.kanade.tachiyomi.lib.cryptoaes.CryptoAES
import eu.kanade.tachiyomi.multisrc.madara.Madara
import eu.kanade.tachiyomi.network.interceptor.rateLimit
import eu.kanade.tachiyomi.source.model.Page
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import org.jsoup.nodes.Document
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class Amuy : Madara(
    "Amuy",
    "https://amuyscans.com",
    "pt-BR",
    SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")),
) {

    override val client: OkHttpClient = super.client.newBuilder()
        .rateLimit(1, 2, TimeUnit.SECONDS)
        .build()

    override val useNewChapterEndpoint = true

    override fun pageListParse(document: Document): List<Page> {
        val chapterProtector = document.getElementById("chapter-protector-data")?.html()
            ?: return super.pageListParse(document)

        val password = chapterProtector
            .substringAfter("wpmangaprotectornonce='")
            .substringBefore("';")
        val chapterData = json.parseToJsonElement(
            chapterProtector
                .substringAfter("chapter_data='")
                .substringBefore("';")
                .replace("\\/", "/"),
        ).jsonObject

        val unsaltedCiphertext = Base64.decode(chapterData["ct"]!!.jsonPrimitive.content, Base64.DEFAULT)
        val salt = chapterData["s"]!!.jsonPrimitive.content.decodeHex()
        val ciphertext = SALTED + salt + unsaltedCiphertext

        val rawImgArray = CryptoAES.decrypt(Base64.encodeToString(ciphertext, Base64.DEFAULT), password)
        val imgArrayString = json.parseToJsonElement(rawImgArray).jsonPrimitive.content
        val imgArray = json.parseToJsonElement(imgArrayString).jsonArray

        return imgArray.mapIndexed { idx, it ->
            Page(idx, document.location(), it.jsonPrimitive.content)
        }
    }

    // https://stackoverflow.com/a/66614516
    private fun String.decodeHex(): ByteArray {
        check(length % 2 == 0) { "Must have an even length" }

        return chunked(2)
            .map { it.toInt(16).toByte() }
            .toByteArray()
    }

    companion object {
        val SALTED = "Salted__".toByteArray(Charsets.UTF_8)
    }
}
