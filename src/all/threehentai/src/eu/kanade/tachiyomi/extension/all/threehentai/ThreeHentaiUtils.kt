package eu.kanade.tachiyomi.extension.all.threehentai

import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.text.SimpleDateFormat
import java.util.Locale

object ThreeHentaiUtils {
    fun getArtists(document: Document): String {
        val artists = document.select("#main-info > div.tag-container:contains(Artists) > .filter-elem > a.name")
        return artists.joinToString(", ") { it.cleanTag() }
    }

    fun getGroups(document: Document): String? {
        val groups = document.select("#main-info > div.tag-container:contains(Groups) > .filter-elem > a.name")
        return if (groups.isNotEmpty()) {
            groups.joinToString(", ") { it.cleanTag() }
        } else {
            null
        }
    }

    fun getTagDescription(document: Document): String {
        val stringBuilder = StringBuilder()

        val categories = document.select("#main-info > div.tag-container:contains(Categories) > .filter-elem > a.name")
        if (categories.isNotEmpty()) {
            stringBuilder.append("Categories: ")
            stringBuilder.append(categories.joinToString(", ") { it.cleanTag() })
            stringBuilder.append("\n\n")
        }

        val series = document.select("#main-info > div.tag-container:contains(Series) > .filter-elem > a.name")
        if (series.isNotEmpty()) {
            stringBuilder.append("Series: ")
            stringBuilder.append(series.joinToString(", ") { it.cleanTag() })
            stringBuilder.append("\n")
        }

        val characters = document.select("#main-info > div.tag-container:contains(Characters) > .filter-elem > a.name")
        if (characters.isNotEmpty()) {
            stringBuilder.append("Characters: ")
            stringBuilder.append(characters.joinToString(", ") { it.cleanTag() })
            stringBuilder.append("\n")
        }
        stringBuilder.append("\n")

        val languages = document.select("#main-info > div.tag-container:contains(Languages) > .filter-elem > a.name")
        if (languages.isNotEmpty()) {
            stringBuilder.append("Languages: ")
            stringBuilder.append(languages.joinToString(", ") { it.cleanTag() })
        }

        return stringBuilder.toString()
    }

    fun getTags(document: Document): String {
        val tags = document.select("#main-info > div.tag-container:contains(Tags) > .filter-elem > a.name")
        return tags.map { it.cleanTag() }.sorted().joinToString(", ")
    }

    fun getNumPages(document: Document): String {
        return document.selectFirst("#main-info > div.tag-container:contains(Pages) > span")!!.cleanTag()
    }

    fun getTime(document: Document): Long {
        val timeString = document.selectFirst("#main-info > div.tag-container > time")
            ?.attr("datetime")
            ?.replace("T", " ")
            ?: ""

        return SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ", Locale.getDefault()).parse(timeString)?.time ?: 0L
    }

    fun getCodes(document: Document): String? {
        val codes = document.select("#main-info > h3 > strong")
        if (codes.isNotEmpty()) {
            return "Code: "
                .plus(
                    codes.eachText().filterNotNull().joinToString {
                        it.replace("d", "3hentai - #")
                            .replace("g", "nhentai - #")
                    },
                )
                .plus("\n\n")
        }
        return null
    }

    private fun Element.cleanTag(): String = text().replace(Regex("\\(.*\\)"), "").trim()
}
