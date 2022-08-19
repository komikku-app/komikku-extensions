package eu.kanade.tachiyomi.extension.ja.manga9co

internal const val MIRROR_PREF = "MIRROR"
internal val MIRRORS get() = arrayOf("manga9.co", "mangaraw.co", "mangaraw.lol", "mangarawjp.com")

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

internal class Selectors(
    val listMangaSelector: String,
    val detailsSelector: String,
    val recommendClass: String,
)
