package eu.kanade.tachiyomi.extension.ar.mangaswat

import eu.kanade.tachiyomi.multisrc.wpmangastream.WPMangaStream
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SManga
import okhttp3.Headers
import okhttp3.Request
import org.jsoup.nodes.Document
import java.text.SimpleDateFormat
import java.util.Locale

class MangaSwat : WPMangaStream("MangaSwat", "https://swatmanga.co", "ar", SimpleDateFormat("yyyy-MM-dd", Locale.US)) {

    override fun headersBuilder(): Headers.Builder = Headers.Builder()
        .add("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9")
        .add("Accept-language", "en-US,en;q=0.9")
        .add("Referer", baseUrl)

    override fun imageRequest(page: Page): Request {
        val newHeaders = headersBuilder()
            .set("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8")
            .set("Referer", baseUrl)
            .build()
        return GET(page.imageUrl!!, newHeaders)
    }

    override fun mangaDetailsParse(document: Document): SManga {
        return SManga.create().apply {
            document.select("div.bigcontent").firstOrNull()?.let { infoElement ->
                genre = infoElement.select("span:contains(التصنيف) a").joinToString { it.text() }
                status = parseStatus(infoElement.select("span:contains(الحالة)").firstOrNull()?.ownText())
                author = infoElement.select("span:contains(المؤلف)").firstOrNull()?.ownText()
                artist = infoElement.select("span:contains(الناشر) i").firstOrNull()?.ownText()
                description = infoElement.select("div.desc").text()
                thumbnail_url = infoElement.select("img").imgAttr()

                val genres = infoElement.select("span:contains(التصنيف) a, .mgen a")
                    .map { element -> element.text().toLowerCase() }
                    .toMutableSet()

                // add series type(manga/manhwa/manhua/other) thinggy to genre
                document.select(seriesTypeSelector).firstOrNull()?.ownText()?.let {
                    if (it.isEmpty().not() && genres.contains(it).not()) {
                        genres.add(it.toLowerCase())
                    }
                }

                genre = genres.toList().map { it.capitalize() }.joinToString(", ")
            }
        }
    }
    override val seriesTypeSelector = "span:contains(النوع) a"

    override val pageSelector = "div#readerarea img"

    override fun pageListParse(document: Document): List<Page> {
        return document.select(pageSelector)
            .filterNot { it.attr("src").isNullOrEmpty() }
            .mapIndexed { i, img -> Page(i, "", img.attr("src")) }
    }

    override fun getFilterList() = FilterList(
        SortByFilter(),
        Filter.Separator(),
        StatusFilter(),
        Filter.Separator(),
        TypeFilter(),
        Filter.Separator(),
        Filter.Header("Genre exclusion not available for all sources"),
        GenreListFilter(getGenreList()),
    )

    override fun getGenreList(): List<Genre> = listOf(
        Genre("<--->", ""),
        Genre("Adult", "adult"),
        Genre("آلات", "%d8%a2%d9%84%d8%a7%d8%aa"),
        Genre("أكشن", "%d8%a3%d9%83%d8%b4%d9%86"),
        Genre("إثارة", "%d8%a5%d8%ab%d8%a7%d8%b1%d8%a9"),
        Genre("إعادة إحياء", "%d8%a5%d8%b9%d8%a7%d8%af%d8%a9-%d8%a5%d8%ad%d9%8a%d8%a7%d8%a1"),
        Genre("الحياة المدرسية", "%d8%a7%d9%84%d8%ad%d9%8a%d8%a7%d8%a9-%d8%a7%d9%84%d9%85%d8%af%d8%b1%d8%b3%d9%8a%d8%a9"),
        Genre("الحياة اليومية", "%d8%a7%d9%84%d8%ad%d9%8a%d8%a7%d8%a9-%d8%a7%d9%84%d9%8a%d9%88%d9%85%d9%8a%d8%a9"),
        Genre("العاب فيديو", "%d8%a7%d9%84%d8%b9%d8%a7%d8%a8-%d9%81%d9%8a%d8%af%d9%8a%d9%88"),
        Genre("ايتشي", "%d8%a7%d9%8a%d8%aa%d8%b4%d9%8a"),
        Genre("ايسكاي", "%d8%a7%d9%8a%d8%b3%d9%83%d8%a7%d9%8a"),
        Genre("بالغ", "%d8%a8%d8%a7%d9%84%d8%ba"),
        Genre("تاريخي", "%d8%aa%d8%a7%d8%b1%d9%8a%d8%ae%d9%8a"),
        Genre("تراجيدي", "%d8%aa%d8%b1%d8%a7%d8%ac%d9%8a%d8%af%d9%8a"),
        Genre("تناسخ", "%d8%aa%d9%86%d8%a7%d8%b3%d8%ae"),
        Genre("جريمة", "%d8%ac%d8%b1%d9%8a%d9%85%d8%a9"),
        Genre("جوسيه", "%d8%ac%d9%88%d8%b3%d9%8a%d9%87"),
        Genre("جيندر بندر", "%d8%ac%d9%8a%d9%86%d8%af%d8%b1-%d8%a8%d9%86%d8%af%d8%b1"),
        Genre("حديث", "%d8%ad%d8%af%d9%8a%d8%ab"),
        Genre("حربي", "%d8%ad%d8%b1%d8%a8%d9%8a"),
        Genre("حريم", "%d8%ad%d8%b1%d9%8a%d9%85"),
        Genre("خارق للطبيعة", "%d8%ae%d8%a7%d8%b1%d9%82-%d9%84%d9%84%d8%b7%d8%a8%d9%8a%d8%b9%d8%a9"),
        Genre("خيال", "%d8%ae%d9%8a%d8%a7%d9%84"),
        Genre("خيال علمي", "%d8%ae%d9%8a%d8%a7%d9%84-%d8%b9%d9%84%d9%85%d9%8a"),
        Genre("دراما", "%d8%af%d8%b1%d8%a7%d9%85%d8%a7"),
        Genre("دموي", "%d8%af%d9%85%d9%88%d9%8a"),
        Genre("راشد", "%d8%af%d9%85%d9%88%d9%8a"),
        Genre("رعب", "%d8%b1%d8%b9%d8%a8"),
        Genre("رومانسي", "%d8%b1%d9%88%d9%85%d8%a7%d9%86%d8%b3%d9%8a"),
        Genre("رياضة", "%d8%b1%d9%8a%d8%a7%d8%b6%d8%a9"),
        Genre("زمكاني", "%d8%b2%d9%85%d9%83%d8%a7%d9%86%d9%8a"),
        Genre("زومبي", "%d8%b2%d9%88%d9%85%d8%a8%d9%8a"),
        Genre("سحر", "%d8%b3%d8%ad%d8%b1"),
        Genre("سينين", "%d8%b3%d9%8a%d9%86%d9%8a%d9%86"),
        Genre("شريحة من الحياة", "%d8%b4%d8%b1%d9%8a%d8%ad%d8%a9-%d9%85%d9%86-%d8%a7%d9%84%d8%ad%d9%8a%d8%a7%d8%a9"),
        Genre("شوجو", "%d8%b4%d9%88%d8%ac%d9%88"),
        Genre("شونين", "%d8%b4%d9%88%d9%86%d9%8a%d9%86"),
        Genre("شياطين", "%d8%b4%d9%8a%d8%a7%d8%b7%d9%8a%d9%86"),
        Genre("طبخ", "%d8%b7%d8%a8%d8%ae"),
        Genre("طبي", "%d8%b7%d8%a8%d9%8a"),
        Genre("غموض", "%d8%ba%d9%85%d9%88%d8%b6"),
        Genre("فانتازي", "%d9%81%d8%a7%d9%86%d8%aa%d8%a7%d8%b2%d9%8a"),
        Genre("فنون قتالية", "%d9%81%d9%86%d9%88%d9%86-%d9%82%d8%aa%d8%a7%d9%84%d9%8a%d8%a9"),
        Genre("فوق الطبيعة", "%d9%81%d9%88%d9%82-%d8%a7%d9%84%d8%b7%d8%a8%d9%8a%d8%b9%d8%a9"),
        Genre("قوى خارقة", "%d9%82%d9%88%d9%89-%d8%ae%d8%a7%d8%b1%d9%82%d8%a9"),
        Genre("كوميدي", "%d9%83%d9%88%d9%85%d9%8a%d8%af%d9%8a"),
        Genre("لعبة", "%d9%84%d8%b9%d8%a8%d8%a9"),
        Genre("مافيا", "%d9%85%d8%a7%d9%81%d9%8a%d8%a7"),
        Genre("مصاصى الدماء", "%d9%85%d8%b5%d8%a7%d8%b5%d9%89-%d8%a7%d9%84%d8%af%d9%85%d8%a7%d8%a1"),
        Genre("مغامرات", "%d9%85%d8%ba%d8%a7%d9%85%d8%b1%d8%a7%d8%aa"),
        Genre("موريم", "%d9%85%d9%88%d8%b1%d9%8a%d9%85"),
        Genre("موسيقي", "%d9%85%d9%88%d8%b3%d9%8a%d9%82%d9%89"),
        Genre("ميشا", "%d9%85%d9%8a%d8%b4%d8%a7"),
        Genre("ميكا", "%d9%85%d9%8a%d9%83%d8%a7"),
        Genre("نفسي", "%d9%86%d9%81%d8%b3%d9%8a"),
        Genre("وحوش", "%d9%88%d8%ad%d9%88%d8%b4"),
        Genre("ويب-تون", "%d9%88%d9%8a%d8%a8-%d8%aa%d9%88%d9%86")
    )
}
