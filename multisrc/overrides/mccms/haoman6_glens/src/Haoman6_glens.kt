package eu.kanade.tachiyomi.extension.zh.haoman6_glens

import eu.kanade.tachiyomi.multisrc.mccms.MCCMS

class Haoman6_glens : MCCMS("好漫6 (g-lens)", "https://www.g-lens.com") {
    override fun transformTitle(title: String) = title.removeSuffix("_").removeSuffix("-")
    override val lazyLoadImageAttr = "pc-ec"
}
