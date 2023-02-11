package eu.kanade.tachiyomi.extension.ru.rumix

import eu.kanade.tachiyomi.multisrc.grouple.GroupLe
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request

class RuMIX : GroupLe("RuMIX", "https://rumix.me", "ru") {

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        val url = super.searchMangaRequest(page, query, filters).url.newBuilder()
        (if (filters.isEmpty()) getFilterList().reversed() else filters.reversed()).forEach { filter ->
            when (filter) {
                is OrderBy -> {
                    if (url.toString().contains("&") && filter.state < 6) {
                        url.addQueryParameter("sortType", arrayOf("RATING", "POPULARITY", "YEAR", "NAME", "DATE_CREATE", "DATE_UPDATE")[filter.state])
                    } else {
                        val ord = arrayOf("rate", "popularity", "year", "name", "created", "updated", "votes")[filter.state]
                        val ordUrl = "$baseUrl/list?sortType=$ord&offset=${70 * (page - 1)}".toHttpUrlOrNull()!!.newBuilder()
                        return GET(ordUrl.toString(), headers)
                    }
                }
                else -> return@forEach
            }
        }
        return if (url.toString().contains("&")) {
            GET(url.toString().replace("=%3D", "="), headers)
        } else {
            popularMangaRequest(page)
        }
    }
    private class OrderBy : Filter.Select<String>(
        "Сортировка",
        arrayOf("По популярности", "Популярно сейчас", "По году", "По имени", "Новинки", "По дате обновления", "По рейтингу"),
    )

    override fun getFilterList() = FilterList(
        OrderBy(),
    )
}
