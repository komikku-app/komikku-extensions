package eu.kanade.tachiyomi.extension.pt.sinensis

import android.util.Base64
import eu.kanade.tachiyomi.lib.cryptoaes.CryptoAES
import eu.kanade.tachiyomi.multisrc.madara.Madara
import eu.kanade.tachiyomi.network.interceptor.rateLimit
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class SinensisScan : Madara(
    "Sinensis Scan",
    "https://sinensisscans.com",
    "pt-BR",
    SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")),
) {

    // Name changed from Sinensis to Sinensis Scan
    override val id: Long = 3891513807564817914

    override val client: OkHttpClient = super.client.newBuilder()
        .rateLimit(1, 2, TimeUnit.SECONDS)
        .build()

    override fun popularMangaFromElement(element: Element): SManga {
        return super.popularMangaFromElement(element).apply {
            setUrlWithoutDomain(url.removeBadPath("manga"))
        }
    }

    override fun searchMangaFromElement(element: Element): SManga {
        return super.searchMangaFromElement(element).apply {
            setUrlWithoutDomain(url.removeBadPath("manga"))
        }
    }

    override fun chapterFromElement(element: Element): SChapter {
        return super.chapterFromElement(element).apply {
            setUrlWithoutDomain(url.removeBadPath("manga"))
        }
    }

    override fun pageListParse(document: Document): List<Page> {
        val chapterProtector = document.selectFirst("script#chapter-protector-data")?.data()
            ?: return super.pageListParse(document)

        val password = chapterProtector
            .substringAfter("wpmangaprotectornonce='")
            .substringBefore("';")
        val chapterData = chapterProtector
            .substringAfter("chapter_data='")
            .substringBefore("';")
            .replace("\\/", "/")
            .let { json.decodeFromString<Map<String, String>>(it) }

        val unsaltedCipherText = Base64.decode(chapterData["ct"]!!, Base64.DEFAULT)
        val salt = chapterData["s"]!!.decodeHex()
        val cipherText = SALTED + salt + unsaltedCipherText

        val rawImageArray = CryptoAES.decrypt(Base64.encodeToString(cipherText, Base64.DEFAULT), password)
        val imageArrayString = json.parseToJsonElement(rawImageArray).jsonPrimitive.content
        val imageArray = json.parseToJsonElement(imageArrayString).jsonArray

        return imageArray.mapIndexed { i, jsonElement ->
            Page(i, document.location(), jsonElement.jsonPrimitive.content)
        }
    }

    private fun String.decodeHex(): ByteArray {
        check(length % 2 == 0) { "Must have an even length" }

        return chunked(2)
            .map { it.toInt(16).toByte() }
            .toByteArray()
    }

    private fun String.removeBadPath(expectedFirstPath: String): String {
        val fullUrl = if (contains(baseUrl)) this else (baseUrl + this)
        val url = fullUrl.toHttpUrl()

        if (url.pathSegments.firstOrNull() != expectedFirstPath) {
            return url.newBuilder().removePathSegment(0).toString()
        }

        return url.toString()
    }

    companion object {
        val SALTED = "Salted__".toByteArray(Charsets.UTF_8)
    }
}
