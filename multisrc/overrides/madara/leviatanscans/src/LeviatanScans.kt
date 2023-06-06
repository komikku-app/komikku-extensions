package eu.kanade.tachiyomi.extension.all.leviatanscans

import android.util.Base64
import eu.kanade.tachiyomi.lib.cryptoaes.CryptoAES
import eu.kanade.tachiyomi.multisrc.madara.Madara
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.text.SimpleDateFormat

abstract class LeviatanScans(
    baseUrl: String,
    lang: String,
    dateFormat: SimpleDateFormat,
) : Madara(
    "Leviatan Scans",
    baseUrl,
    lang,
    dateFormat,
) {
    override val useNewChapterEndpoint: Boolean = true

    override fun chapterListSelector() = "li.wp-manga-chapter:not(.premium-block)"

    override fun popularMangaFromElement(element: Element) =
        replaceRandomUrlPartInManga(super.popularMangaFromElement(element))

    override fun latestUpdatesFromElement(element: Element) =
        replaceRandomUrlPartInManga(super.latestUpdatesFromElement(element))

    override fun searchMangaFromElement(element: Element) =
        replaceRandomUrlPartInManga(super.searchMangaFromElement(element))

    override fun chapterFromElement(element: Element): SChapter {
        val chapter = replaceRandomUrlPartInChapter(super.chapterFromElement(element))

        with(element) {
            selectFirst(chapterUrlSelector)?.let { urlElement ->
                chapter.name = urlElement.ownText()
            }
        }

        return chapter
    }

    private fun replaceRandomUrlPartInManga(manga: SManga): SManga {
        val split = manga.url.split("/")
        manga.url = split.slice(split.indexOf("manga") until split.size).joinToString("/", "/")
        return manga
    }

    private fun replaceRandomUrlPartInChapter(chapter: SChapter): SChapter {
        val split = chapter.url.split("/")
        chapter.url = baseUrl + split.slice(split.indexOf("manga") until split.size).joinToString("/", "/")
        return chapter
    }

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
