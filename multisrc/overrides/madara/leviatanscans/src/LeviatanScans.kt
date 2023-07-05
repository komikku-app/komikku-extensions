package eu.kanade.tachiyomi.extension.all.leviatanscans

import eu.kanade.tachiyomi.multisrc.madara.Madara
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
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
}
