package eu.kanade.tachiyomi.extension.es.scambertraslator

import eu.kanade.tachiyomi.multisrc.madara.Madara
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import org.jsoup.nodes.Element
import java.text.SimpleDateFormat
import java.util.Locale

class ScamberTraslator : Madara(
    "ScamberTraslator",
    "https://scambertraslator.com",
    "es",
    dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale("es")),
) {
    override val useNewChapterEndpoint = true

    override val popularMangaUrlSelector = "div.item-thumb-slime a"
    override fun chapterListSelector() = "li.wp-manga-chapter:has(a[href*=$baseUrl])" // The source has hidden links to external sites by default

    override val mangaDetailsSelectorGenre = "div.genres-container-slime-slime a"
    override val mangaDetailsSelectorThumbnail = "div.thumb-half-slime img"
    override val mangaDetailsSelectorStatus = "div.status-slime"

    override fun popularMangaFromElement(element: Element): SManga {
        val manga = SManga.create()

        with(element) {
            select(popularMangaUrlSelector).first()?.let {
                manga.setUrlWithoutDomain(it.attr("abs:href"))
                manga.title = it.attr("title")
            }

            select("img").first()?.let {
                manga.thumbnail_url = imageFromElement(it)
            }
        }

        return manga
    }

    override fun chapterFromElement(element: Element): SChapter {
        val chapter = SChapter.create()

        with(element) {
            select(chapterUrlSelector).first()?.let { urlElement ->
                chapter.url = urlElement.attr("abs:href").let {
                    it.substringBefore("?style=paged") + if (!it.endsWith(chapterUrlSuffix)) chapterUrlSuffix else ""
                }
                chapter.name = urlElement.select("span.chapternum").text()
                chapter.date_upload = parseChapterDate(select("span.chapterdate").text())
            }
        }

        return chapter
    }
}
