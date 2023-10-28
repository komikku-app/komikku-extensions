package eu.kanade.tachiyomi.extension.vi.ocumeo

import eu.kanade.tachiyomi.multisrc.a3manga.A3Manga
import eu.kanade.tachiyomi.source.model.Page
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

class OCuMeo : A3Manga("Ổ Cú Mèo", "https://www.ocumoe.com", "vi") {

    override fun pageListParse(document: Document): List<Page> {
        val imgListHtml = decodeImgList(document)

        return Jsoup.parseBodyFragment(imgListHtml).select("img").mapIndexed { idx, element ->
            val encryptedUrl = element.attributes().find { it.key.startsWith("data") }?.value
            Page(idx, imageUrl = encryptedUrl?.decodeUrl())
        }
    }

    private fun String.decodeUrl(): String {
        // We expect the URL to start with `https://`, where the last 3 characters are encoded.
        // The length of the encoded character is not known, but it is the same across all.
        // Essentially we are looking for the two encoded slashes, which tells us the length.
        val patternIdx = patternsLengthCheck.indexOfFirst { pattern ->
            val matchResult = pattern.find(this)
            val g1 = matchResult?.groupValues?.get(1)
            val g2 = matchResult?.groupValues?.get(2)
            g1 == g2 && g1 != null
        }
        if (patternIdx == -1) {
            throw Exception("Failed to decrypt URL")
        }

        // With a known length we can predict all the encoded characters.
        // This is a slightly more expensive pattern, hence the separation.
        val matchResult = patternsSubstitution[patternIdx].find(this)
        return matchResult?.destructured?.let { (colon, slash, period) ->
            this
                .replace(colon, ":")
                .replace(slash, "/")
                .replace(period, ".")
        } ?: throw Exception("Failed to reconstruct URL")
    }

    companion object {
        private val patternsLengthCheck: List<Regex> = (20 downTo 1).map { i ->
            """^https.{$i}(.{$i})(.{$i})""".toRegex()
        }
        private val patternsSubstitution: List<Regex> = (20 downTo 1).map { i ->
            """^https(.{$i})(.{$i}).*(.{$i})(?:webp|jpeg|tiff|.{3})$""".toRegex()
        }
    }
}
