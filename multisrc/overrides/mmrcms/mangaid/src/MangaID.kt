package eu.kanade.tachiyomi.extension.id.mangaid

import eu.kanade.tachiyomi.multisrc.mmrcms.MMRCMS
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.util.asJsoup
import okhttp3.Response

class MangaID : MMRCMS("MangaID", "https://mangaid.click", "id") {
    override fun mangaDetailsParse(response: Response): SManga {
        val document = response.asJsoup()
        val genres = mutableListOf<String>()
        val type = document.select(".row .dl-horizontal dt:contains(Type) + dd").firstOrNull()?.text().orEmpty()
        if (type.isNotBlank()) {
            genres.add(type)
        }
        val categories = document.select(".row .dl-horizontal dt:contains(Categories) + dd a").eachText()
        genres.addAll(categories)

        val altName = document.select(".row .dl-horizontal dt:contains(Other names) + dd").text().trim()

        return SManga.create().apply {
            title = document.select(".row .dl-horizontal h1.widget-title").text()
            author = document.select(".row .dl-horizontal dt:contains(Author(s)) + dd").text().clean()
            artist = document.select(".row .dl-horizontal dt:contains(Author(s)) + dd").text().clean()
            genre = genres.joinToString(", ")
            status = parseStatus(document.select(".row .dl-horizontal dt:contains(Status) + dd").text())
            description = document.select("div.row div.well p").text().trim()
            if (altName.isNotBlank() && altName != "-") {
                description += "\n\nJudul Alternatif: $altName"
            }
        }
    }

    private fun parseStatus(status: String): Int {
        return when (status.lowercase()) {
            in setOf("complete") -> SManga.COMPLETED
            in setOf("ongoing") -> SManga.ONGOING
            else -> SManga.UNKNOWN
        }
    }

    private fun String.clean(): String {
        return this.trim().replace(" , ", ", ")
    }
}
