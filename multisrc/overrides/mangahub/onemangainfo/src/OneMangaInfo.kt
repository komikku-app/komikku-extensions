package eu.kanade.tachiyomi.extension.en.onemangainfo

import eu.kanade.tachiyomi.multisrc.mangahub.MangaHub

class OneMangaInfo : MangaHub(
    "OneManga.info",
    "https://onemanga.info",
    "en"
) {
    override val serverId = "mn02"
}
