package eu.kanade.tachiyomi.extension.all.leviatanscans

import eu.kanade.tachiyomi.multisrc.madara.Madara
import eu.kanade.tachiyomi.network.interceptor.rateLimit
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.SourceFactory
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import okhttp3.OkHttpClient
import org.jsoup.nodes.Element
import java.text.SimpleDateFormat
import java.util.Locale

class LeviatanScansFactory : SourceFactory {
    override fun createSources(): List<Source> = listOf(
        LeviatanScansEN(),
        LeviatanScansES(),
    )
}

class LeviatanScansEN : Madara("Leviatan Scans", "https://en.leviatanscans.com", "en", SimpleDateFormat("MMM dd, yyyy", Locale.US)) {

    override val client: OkHttpClient = super.client.newBuilder()
        .rateLimit(1, 2)
        .build()

    override val useNewChapterEndpoint: Boolean = true

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

    override val mangaDetailsSelectorStatus = ".post-content_item:contains(Status) .summary-content"
}

class LeviatanScansES : Madara("Leviatan Scans", "https://es.leviatanscans.com", "es", SimpleDateFormat("MMM, yy", Locale("es"))) {
    override val useNewChapterEndpoint: Boolean = true

    override fun popularMangaFromElement(element: Element) =
        replaceRandomUrlPartInManga(super.popularMangaFromElement(element))

    override fun latestUpdatesFromElement(element: Element) =
        replaceRandomUrlPartInManga(super.latestUpdatesFromElement(element))

    override fun searchMangaFromElement(element: Element) =
        replaceRandomUrlPartInManga(super.searchMangaFromElement(element))

    override fun chapterFromElement(element: Element) =
        replaceRandomUrlPartInChapter(super.chapterFromElement(element))

    override val mangaDetailsSelectorStatus = ".post-content_item:contains(Status) .summary-content"
}

fun Madara.replaceRandomUrlPartInManga(manga: SManga): SManga {
    val split = manga.url.split("/")
    manga.url = split.slice(split.indexOf("manga") until split.size).joinToString("/", "/")
    return manga
}

fun Madara.replaceRandomUrlPartInChapter(chapter: SChapter): SChapter {
    val split = chapter.url.split("/")
    chapter.url = baseUrl + split.slice(split.indexOf("manga") until split.size).joinToString("/", "/")
    return chapter
}
