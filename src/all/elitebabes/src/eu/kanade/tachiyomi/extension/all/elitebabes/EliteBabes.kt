package eu.kanade.tachiyomi.extension.all.elitebabes

import eu.kanade.tachiyomi.multisrc.masonry.Masonry
import eu.kanade.tachiyomi.multisrc.masonry.SelectFilter
import eu.kanade.tachiyomi.multisrc.masonry.SortFilter
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
    private val collections: List<Pair<String, String>> = listOf(
        Pair("SexArt Models", "https://www.sexarthub.com"),
        Pair("Femjoy Models", "https://www.femangels.com"),
        Pair("Playboy Centerfolds", "https://www.centerfoldhunter.com"),
    )

    private class CollectionsFilter(val collections: List<Pair<String, String>>) :
        SelectFilter("Collections", collections)

    override fun getFilterList(): FilterList {
        getTags()
        val filters = mutableListOf(
            Filter.Header("Filters ignored with text search"),
            Filter.Separator(),
            SortFilter(),
            Filter.Separator(),
            CollectionsFilter(collections),
        )

        if (tags.isEmpty()) {
            filters.add(
                Filter.Header("Press 'reset' to attempt to load tags"),
            )
        } else {
            tags.let {
                filters.add(
                    TagsFilter(it),
                )
            }
        }

        filters.add(Filter.Separator())

        return FilterList(filters)
    }

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        val collectionsFilter = filters.filterIsInstance<CollectionsFilter>().firstOrNull()
        return if (collectionsFilter == null || collectionsFilter.selected == "") {
            super.searchMangaRequest(page, query, filters)
        } else {
            GET(collectionsFilter.selected, headers)
        }
    }

    override fun searchMangaParse(response: Response): MangasPage {
        return if (collections.map { it.second }.any { response.request.url.toString().contains(it) }) {
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
