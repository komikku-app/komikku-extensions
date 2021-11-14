package eu.kanade.tachiyomi.extension.en.imperfectcomics

import eu.kanade.tachiyomi.multisrc.madara.Madara
import java.text.SimpleDateFormat

class ImperfectComics : Madara("Imperfect Comics", "https://imperfectcomic.com", "en", SimpleDateFormat("yyyy-MM-dd")) {

    override val useNewChapterEndpoint: Boolean = true
}
