package eu.kanade.tachiyomi.extension.en.mangatownhub

import eu.kanade.tachiyomi.multisrc.mangahub.MangaHub

class MangaTownHub : MangaHub(
    "MangaTown (unoriginal)",
    "https://manga.town",
    "en"
) {
    override val serverId = "mt01"
}
