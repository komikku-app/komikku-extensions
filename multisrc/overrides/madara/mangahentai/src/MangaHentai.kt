package eu.kanade.tachiyomi.extension.en.mangahentai

import eu.kanade.tachiyomi.annotations.Nsfw
import eu.kanade.tachiyomi.multisrc.madara.Madara

@Nsfw
class MangaHentai : Madara("Manga Hentai", "https://mangahentai.me", "en") {
    override val useNewChapterEndpoint: Boolean = true
}
