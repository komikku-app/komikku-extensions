package eu.kanade.tachiyomi.extension.ar.manga3asq

import eu.kanade.tachiyomi.multisrc.madara.Madara

class Manga3asq : Madara("مانجا العاشق", "https://3asq.org", "ar") {
    override val useNewChapterEndpoint: Boolean = true
}
