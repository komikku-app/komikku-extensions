package eu.kanade.tachiyomi.extension.en.manganel

import eu.kanade.tachiyomi.multisrc.mangahub.MangaHub

class MangaNel : MangaHub(
    "MangaNel",
    "https://manganel.me",
    "en"
) {
    override val serverId = "mn05"
}
