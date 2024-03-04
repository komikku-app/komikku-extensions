package eu.kanade.tachiyomi.extension.all.elitebabes

import eu.kanade.tachiyomi.multisrc.masonry.Masonry
import eu.kanade.tachiyomi.multisrc.masonry.SortFilter
import eu.kanade.tachiyomi.multisrc.masonry.Tag
import eu.kanade.tachiyomi.multisrc.masonry.TagsFilter
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.util.asJsoup
import okhttp3.Request
import okhttp3.Response

class EliteBabes : Masonry("Elite Babes", "https://www.elitebabes.com", "all") {
    private val listOfHubs: List<Tag> = listOf(
        Tag("Sex Art Hub", "https://www.sexarthub.com"),
        Tag("Fem Angels", "https://www.femangels.com"),
        Tag("Centerfold Hunter", "https://www.centerfoldhunter.com"),
    )

    override fun getFilterList(): FilterList {
        getTags()
        val filters = mutableListOf(
            Filter.Header("Filters ignored with text search"),
            Filter.Separator(),
            SortFilter(),
        )

        if (tags.isEmpty()) {
            filters.add(
                Filter.Header("Press 'reset' to attempt to load tags"),
            )
        } else {
            (listOfHubs + tags)
                .let {
                    filters.add(
                        TagsFilter(it),
                    )
                }
        }

        return FilterList(filters)
    }

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        val tagsFilter = filters.filterIsInstance<TagsFilter>().first()
        return if (tagsFilter.state.subList(0, listOfHubs.size).any { it.state }) {
            GET(tagsFilter.state.first { it.state }.uriPart, headers)
        } else {
            super.searchMangaRequest(page, query, filters)
        }
    }

    override fun searchMangaParse(response: Response): MangasPage {
        return if (listOfHubs.map { it.uriPart }.any { response.request.url.toString().contains(it) }) {
            MangasPage(
                mangas = response.asJsoup().select("div.item a[href]:has(img)")
                    .map { element ->
                        SManga.create().apply {
                            setUrlWithoutDomain(element.absUrl("href"))
                            title = element.select("img").attr("alt")
                            thumbnail_url = element.select("img").first()?.let { imgElmAttr(it) }
                        }
                    },
                hasNextPage = false,
            )
        } else {
            super.searchMangaParse(response)
        }
    }
}
