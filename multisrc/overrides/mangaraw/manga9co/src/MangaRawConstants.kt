package eu.kanade.tachiyomi.extension.ja.manga9co

/**
 * https://syosetu.me/ is not added because of different HTML structure
 */

internal const val MIRROR_PREF = "MIRROR"
internal val MIRRORS get() = arrayOf("manga9.co", "mangaraw.to", "mangaraw.io", "mangarawjp.io")

internal fun getSelectors(mirrorIndex: Int) = when (mirrorIndex) {
    0, 1, 2 -> Selectors(
        listMangaSelector = ".card",
        detailsSelector = "div:has(> main)",
        recommendClass = "container"
    )
    else -> Selectors(
        listMangaSelector = ".post-list:not(.last-hidden) > .item",
        detailsSelector = "#post-data",
        recommendClass = "post-list"
    )
}

internal fun needUrlSanitize(mirrorIndex: Int) = mirrorIndex == 2

internal val mangaSlugRegex = Regex("""^/mz[a-z]{4}-""")

internal class Selectors(
    val listMangaSelector: String,
    val detailsSelector: String,
    val recommendClass: String,
)
