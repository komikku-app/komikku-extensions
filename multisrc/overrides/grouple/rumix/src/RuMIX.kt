package eu.kanade.tachiyomi.extension.ru.rumix

import eu.kanade.tachiyomi.multisrc.grouple.GroupLe
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.FilterList
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request

class RuMIX : GroupLe("RuMIX", "https://rumix.me", "ru") {

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        val url = "$baseUrl/search/advanced?offset=${70 * (page - 1)}".toHttpUrlOrNull()!!.newBuilder()
        if (query.isNotEmpty()) {
            url.addQueryParameter("q", query)
        }
        return if (url.toString().contains("&"))
            GET(url.toString().replace("=%3D", "="), headers)
        else popularMangaRequest(page)
    }
}
