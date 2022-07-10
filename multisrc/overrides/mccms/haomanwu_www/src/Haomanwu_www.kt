package eu.kanade.tachiyomi.extension.zh.haomanwu_www

import eu.kanade.tachiyomi.multisrc.mccms.MCCMS

class Haomanwu_www : MCCMS("好漫屋 (网页)", "https://www.haomanwu.com") {
    override val lazyLoadImageAttr = "data-echo"
}
