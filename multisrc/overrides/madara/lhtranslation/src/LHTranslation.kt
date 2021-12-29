package eu.kanade.tachiyomi.extension.en.lhtranslation

import eu.kanade.tachiyomi.multisrc.madara.Madara

class LHTranslation : Madara("LHTranslation", "https://lhtranslation.net", "en") {
    override val useNewChapterEndpoint = true
}
