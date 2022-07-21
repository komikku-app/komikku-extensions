package eu.kanade.tachiyomi.extension.en.bsasmenucom

import eu.kanade.tachiyomi.multisrc.madara.Madara

class BsasmenuCom : Madara("bsasmenu.com", "https://bsasmenu.com", "en") {
    override val useNewChapterEndpoint = false

    override val popularMangaUrlSelector = "div.post-title a:not([target=_self])"
}
