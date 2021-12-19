package eu.kanade.tachiyomi.extension.en.manga1st

import eu.kanade.tachiyomi.multisrc.madara.Madara

class Manga1st : Madara("Manga1st", "https://manga1st.com", "en") {
    override val pageListParseSelector = "div.page-break, li.blocks-gallery-item, .reading-content .text-left:not(:has(.blocks-gallery-item)) img"
}
