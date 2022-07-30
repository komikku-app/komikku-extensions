package eu.kanade.tachiyomi.extension.en.mangatoday

import eu.kanade.tachiyomi.multisrc.mangahub.MangaHub

class MangaToday : MangaHub(
    "MangaToday",
    "https://mangatoday.fun",
    "en"
) {
    override val serverId = "m03"
}
