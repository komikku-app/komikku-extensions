package eu.kanade.tachiyomi.extension.zh.haoman6glens

import eu.kanade.tachiyomi.multisrc.mccms.MCCMSWeb
import eu.kanade.tachiyomi.source.model.SManga

class Haoman6glens : MCCMSWeb("好漫6 (g-lens)", "https://www.g-lens.com") {
    override fun SManga.cleanup() = apply {
        title = title.removeSuffix("_").removeSuffix("-").removeSuffix("漫画")
    }

    override val lazyLoadImageAttr = "pc-ec"
}
