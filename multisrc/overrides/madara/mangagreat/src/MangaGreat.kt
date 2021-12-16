package eu.kanade.tachiyomi.extension.en.mangagreat

import eu.kanade.tachiyomi.multisrc.madara.Madara

class MangaGreat : Madara("MangaGreat", "https://mangagreat.com", "en") {
    override val pageListParseSelector = "div.page-break, li.blocks-gallery-item, .reading-content .text-left:not(:has(.blocks-gallery-item)) img"
}
