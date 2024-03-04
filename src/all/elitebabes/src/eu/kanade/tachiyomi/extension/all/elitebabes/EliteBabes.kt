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
            (listOf(Tag("Sex Art Hub", "https://www.sexarthub.com")) + tags)
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
        return if (tagsFilter.state[0].state) {
            GET("https://www.sexarthub.com", headers)
        } else {
            super.searchMangaRequest(page, query, filters)
        }
    }

    override fun searchMangaParse(response: Response): MangasPage {
        return if (response.request.url.toString().contains("^https://www\\.sexarthub\\.com".toRegex())) {
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
