package eu.kanade.tachiyomi.extension.zh.haoman8

import eu.kanade.tachiyomi.multisrc.mccms.MCCMS

class Haoman8 : MCCMS("好漫8", "https://www.haoman8.com") {
    override val lazyLoadImageAttr = "data-echo"
}
