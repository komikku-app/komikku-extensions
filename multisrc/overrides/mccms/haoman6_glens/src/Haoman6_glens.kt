package eu.kanade.tachiyomi.extension.zh.haoman6_glens

import eu.kanade.tachiyomi.multisrc.mccms.MCCMS
import eu.kanade.tachiyomi.source.model.SManga

class Haoman6_glens : MCCMS("好漫6 (g-lens)", "https://www.g-lens.com") {
    override fun SManga.cleanup() = apply {
        title = title.removeSuffix("_").removeSuffix("-")
    }

    override val lazyLoadImageAttr = "pc-ec"
}
