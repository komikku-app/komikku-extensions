package eu.kanade.tachiyomi.extension.zh.haoman6

import eu.kanade.tachiyomi.multisrc.mccms.MCCMS

class Haoman6 : MCCMS("好漫6", "https://www.haoman6.com") {
    override fun transformTitle(title: String) = title.removeSuffix("(最新在线)").removeSuffix("-")
    override val lazyLoadImageAttr = "echo-pc"
}
