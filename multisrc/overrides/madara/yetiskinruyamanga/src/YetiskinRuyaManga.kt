package eu.kanade.tachiyomi.extension.tr.yetiskinruyamanga

import eu.kanade.tachiyomi.multisrc.madara.Madara
import eu.kanade.tachiyomi.source.model.Page
import org.jsoup.nodes.Document
import java.text.SimpleDateFormat
import java.util.Locale

class YetiskinRuyaManga : Madara(
    "Yetişkin Rüya Manga",
    "https://yetiskin.ruyamanga.com",
    "tr",
    SimpleDateFormat("dd/MM/yyy", Locale("tr")),
) {
    override val useNewChapterEndpoint = true

    override fun pageListParse(document: Document): List<Page> {
        val blocked = document.select(".content-blocked").first()
        if (blocked != null) {
            /*
             * Bu bölümü okuyabilmek için GİRİŞ yapmak zorundasınız.
             * Eğer +18 seri'nin VIP bölümündeyseniz okuyabilmek için VIP satın almış olmak zorundasınız.
             * VIP SATIN ALMAK VEYA BİLGİ ALMAK İÇİN "VIP & DESTEK" SAYFAMIZI ZİYARET EDİNİZ.
             */
            throw Exception(blocked.text())
        }

        return super.pageListParse(document)
    }
}
