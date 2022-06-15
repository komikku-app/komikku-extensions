package eu.kanade.tachiyomi.extension.zh.gufengmh

import eu.kanade.tachiyomi.multisrc.sinmh.SinMH

class Gufengmh : SinMH("古风漫画网", "https://www.gufengmh9.com") {

    override val dateSelector = ".pic_zi:nth-of-type(4) > dd"

    override fun chapterListSelector() = ".list li > a"
}
