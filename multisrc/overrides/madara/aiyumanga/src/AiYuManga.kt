package eu.kanade.tachiyomi.extension.es.aiyumanga

import android.util.Base64
import eu.kanade.tachiyomi.lib.cryptoaes.CryptoAES
import eu.kanade.tachiyomi.multisrc.madara.Madara
import eu.kanade.tachiyomi.source.model.Page
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jsoup.nodes.Document
import java.text.SimpleDateFormat
import java.util.Locale

class AiYuManga : Madara(
    "AiYuManga",
    "https://aiyumangascanlation.com",
    "es",
    SimpleDateFormat("MM/dd/yyyy", Locale("es")),
) {
    override val useNewChapterEndpoint = true
    override val useLoadMoreSearch = true
    override val chapterUrlSuffix = ""

    override val mangaDetailsSelectorStatus = "div.post-content_item:contains(Status) > div.summary-content"

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
