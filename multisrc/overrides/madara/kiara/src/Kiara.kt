package eu.kanade.tachiyomi.extension.en.kiara

import eu.kanade.tachiyomi.multisrc.madara.Madara
import eu.kanade.tachiyomi.source.model.Page
import org.jsoup.nodes.Document

class Kiara : Madara("Kiara", "https://kiara.cool", "en") {
    override fun pageListParse(document: Document): List<Page> {
        return super.pageListParse(document).filterIndexed { index, _ -> index % 2 == 0 }
    }
}
