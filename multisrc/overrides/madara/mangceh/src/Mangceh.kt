package eu.kanade.tachiyomi.extension.id.mangceh

import eu.kanade.tachiyomi.annotations.Nsfw
import eu.kanade.tachiyomi.multisrc.madara.Madara

@Nsfw
class Mangceh : Madara("Mangceh", "https://mangceh.me", "id") {
    override val useNewChapterEndpoint = true
}
