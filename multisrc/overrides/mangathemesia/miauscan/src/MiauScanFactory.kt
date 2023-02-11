package eu.kanade.tachiyomi.extension.all.miauscan

import eu.kanade.tachiyomi.multisrc.mangathemesia.MangaThemesia
import eu.kanade.tachiyomi.source.SourceFactory
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.SManga
import okhttp3.Request
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.text.SimpleDateFormat
import java.util.Locale

class MiauScanFactory : SourceFactory {
    override fun createSources() = listOf(
        MiauScan("es", Filter.TriState.STATE_EXCLUDE),
        MiauScan("pt-BR", Filter.TriState.STATE_INCLUDE),
    )
}

open class MiauScan(lang: String, private val portugueseMode: Int) : MangaThemesia(
    name = "Miau Scan",
    baseUrl = "https://miauscan.com",
    lang = lang,
    dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale("es")),
) {

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        val genreFilterIndex = filters.indexOfFirst { it is GenreListFilter }
        val genreFilter = filters.getOrNull(genreFilterIndex) as? GenreListFilter
            ?: GenreListFilter(emptyList())

        val overloadedGenreFilter = GenreListFilter(
            genres = genreFilter.state + listOf(
                Genre("", PORTUGUESE_GENRE, portugueseMode),
            ),
        )

        val overloadedFilters = filters.toMutableList().apply {
            if (genreFilterIndex != -1) {
                removeAt(genreFilterIndex)
            }

            add(overloadedGenreFilter)
        }

        return super.searchMangaRequest(page, query, FilterList(overloadedFilters))
    }

    override fun searchMangaFromElement(element: Element): SManga {
        return super.searchMangaFromElement(element).apply {
            title = title.replace(PORTUGUESE_SUFFIX, "")
        }
    }

    override fun mangaDetailsParse(document: Document): SManga {
        return super.mangaDetailsParse(document).apply {
            title = title.replace(PORTUGUESE_SUFFIX, "")
        }
    }

    override fun getGenreList(): List<Genre> {
        return super.getGenreList().filter { it.value != PORTUGUESE_GENRE }
    }

    companion object {
        const val PORTUGUESE_GENRE = "307"

        val PORTUGUESE_SUFFIX = "^\\(\\s*Portugu[êe]s\\s*\\)\\s*".toRegex(RegexOption.IGNORE_CASE)
    }
}
