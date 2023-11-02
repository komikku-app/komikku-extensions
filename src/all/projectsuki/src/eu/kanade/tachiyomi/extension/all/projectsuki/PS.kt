@file:Suppress("MayBeConstant", "unused")

package eu.kanade.tachiyomi.extension.all.projectsuki

import org.jsoup.nodes.Element
import java.util.Calendar
import java.util.Locale
import kotlin.concurrent.getOrSet

@Suppress("MemberVisibilityCanBePrivate")
internal object PS {
    const val identifier: String = "projectsuki"
    const val identifierShort: String = "ps"

    val bookPath = listOf("book")
    val pagePath = listOf("images", "gallery")
    val chapterPath = listOf("read")

    const val SEARCH_INTENT_PREFIX: String = "$identifierShort:"

    const val PREFERENCE_WHITELIST_LANGUAGES = "$identifier-languages-whitelist"
    const val PREFERENCE_WHITELIST_LANGUAGES_TITLE = "Whitelist the following languages:"
    const val PREFERENCE_WHITELIST_LANGUAGES_SUMMARY =
        "Will keep project chapters in the following languages." +
            " Takes precedence over blacklisted languages." +
            " It will match the string present in the \"Language\" column of the chapter." +
            " Whitespaces will be trimmed." +
            " Leave empty to allow all languages." +
            " Separate each entry with a comma ','"

    const val PREFERENCE_BLACKLIST_LANGUAGES = "$identifier-languages-blacklist"
    const val PREFERENCE_BLACKLIST_LANGUAGES_TITLE = "Blacklist the following languages:"
    const val PREFERENCE_BLACKLIST_LANGUAGES_SUMMARY =
        "Will hide project chapters in the following languages." +
            " Works identically to whitelisting."
}

fun Element.containsBookLinks(): Boolean = select("a").any {
    it.attrNormalizedUrl("href")?.isBookURL() == true
}

fun Element.containsReadLinks(): Boolean = select("a").any {
    it.attrNormalizedUrl("href")?.isReadURL() == true
}

fun Element.containsImageGalleryLinks(): Boolean = select("a").any {
    it.attrNormalizedUrl("href")?.isImagesGalleryURL() == true
}

fun Element.getAllUrlElements(selector: String, attrName: String, predicate: (NormalizedURL) -> Boolean): Map<Element, NormalizedURL> {
    return select(selector)
        .mapNotNull { element -> element.attrNormalizedUrl(attrName)?.let { element to it } }
        .filter { (_, url) -> predicate(url) }
        .toMap()
}

fun Element.getAllBooks(): Map<String, PSBook> {
    val bookUrls = getAllUrlElements("a", "href") { it.isBookURL() }
    val byID: Map<String, Map<Element, NormalizedURL>> = bookUrls.groupBy { (_, url) -> url.pathSegments[1] /* /book/<bookid> */ }

    @Suppress("UNCHECKED_CAST")
    return byID.mapValues { (bookid, elements) ->
        val thumb: Element? = elements.entries.firstNotNullOfOrNull { (element, _) ->
            element.select("img").firstOrNull()
        }
        val title = elements.entries.firstOrNull { (element, _) ->
            element.select("img").isEmpty() && element.text().let {
                it.isNotBlank() && it.lowercase(Locale.US) != "show more"
            }
        }

        if (thumb != null && title != null) {
            PSBook(thumb, title.key, title.key.text(), bookid, title.value)
        } else {
            null
        }
    }.filterValues { it != null } as Map<String, PSBook>
}

inline fun <SK, K, V> Map<K, V>.groupBy(keySelector: (Map.Entry<K, V>) -> SK): Map<SK, Map<K, V>> = buildMap<_, MutableMap<K, V>> {
    this@groupBy.entries.forEach { entry ->
        getOrPut(keySelector(entry)) { HashMap() }[entry.key] = entry.value
    }
}

private val absoluteDateFormat: ThreadLocal<java.text.SimpleDateFormat> = ThreadLocal()
fun String.parseDate(ifFailed: Long = 0L): Long {
    return when {
        endsWith("ago") -> {
            // relative
            val number = takeWhile { it.isDigit() }.toInt()
            val cal = Calendar.getInstance()

            when {
                contains("day") -> cal.apply { add(Calendar.DAY_OF_MONTH, -number) }
                contains("hour") -> cal.apply { add(Calendar.HOUR, -number) }
                contains("minute") -> cal.apply { add(Calendar.MINUTE, -number) }
                contains("second") -> cal.apply { add(Calendar.SECOND, -number) }
                contains("week") -> cal.apply { add(Calendar.DAY_OF_MONTH, -number * 7) }
                contains("month") -> cal.apply { add(Calendar.MONTH, -number) }
                contains("year") -> cal.apply { add(Calendar.YEAR, -number) }
                else -> null
            }?.timeInMillis ?: ifFailed
        }

        else -> {
            // absolute?
            absoluteDateFormat.getOrSet { java.text.SimpleDateFormat("MMMM dd, yyyy", Locale.US) }.parse(this)?.time ?: ifFailed
        }
    }
}

private val imageExtensions = setOf(".jpg", ".png", ".jpeg", ".webp", ".gif", ".avif", ".tiff")
private val simpleSrcVariants = listOf("src", "data-src", "data-lazy-src")
fun Element.imgNormalizedURL(): NormalizedURL? {
    simpleSrcVariants.forEach { variant ->
        if (hasAttr(variant)) {
            return attrNormalizedUrl(variant)
        }
    }

    if (hasAttr("srcset")) {
        return attr("abs:srcset").substringBefore(" ").toNormalURL()
    }

    return attributes().firstOrNull {
        it.key.contains("src") && imageExtensions.any { ext -> it.value.contains(ext) }
    }?.value?.substringBefore(" ")?.toNormalURL()
}
