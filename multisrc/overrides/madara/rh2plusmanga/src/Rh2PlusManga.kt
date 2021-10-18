package eu.kanade.tachiyomi.extension.th.rh2plusmanga

import eu.kanade.tachiyomi.multisrc.madara.Madara
import eu.kanade.tachiyomi.source.model.Page
import org.jsoup.nodes.Document
import java.text.SimpleDateFormat
import java.util.Locale

class Rh2PlusManga : Madara("Rh2PlusManga", "https://www.rh2plusmanga.com", "th", SimpleDateFormat("d MMMM yyyy", Locale("th"))) {
    override val useNewChapterEndpoint = true
    override fun getGenreList() = listOf(
        Genre("เหนือธรรมชาติ", "supernatural"),
        Genre("ทำอาหาร", "cooking"),
        Genre("สยองขวัญ", "horror"),
        Genre("ยูริ", "yuri"),
        Genre("จิตวิทยา", "psychological"),
        Genre("วัยรุ่น", "seinen"),
        Genre("ชีวิตประจำวัน", "slice-of-life"),
        Genre("เค-เว็บตูน", "เค-เว็บตูน"),
        Genre("ต่างโลก", "ต่างโลก"),
        Genre("แฟนตาซี", "fantasy"),
        Genre("ไซ-ไฟ", "sci-fi"),
        Genre("คอมเมดี้", "comedy"),
        Genre("โรแมนติก", "romance"),
        Genre("สำหรับผู้ใหญ่", "adult"),
        Genre("ยาโอย", "yaoi"),
        Genre("ศิลปะการต่อสู้", "martial-arts"),
        Genre("โชเน็น", "shounen"),
        Genre("ดราม่า", "drama"),
        Genre("เกิดใหม่", "เกิดใหม่"),
        Genre("ปริศนา", "mystery"),
        Genre("ประวัติศาสตร์", "historical"),
        Genre("มันฮวา", "มันฮวา"),
        Genre("ผจญภัย", "adventure"),
        Genre("กีฬา", "sports"),
        Genre("มังงะ", "manga"),
        Genre("One shot", "one-shot"),
        Genre("โชโจ", "shoujo"),
        Genre("หุ่นยนต์", "mecha"),
        Genre("แอคชั่น", "action"),
        Genre("ชีวิตในโรงเรียน", "school-life"),
        Genre("ฮาเร็ม", "harem"),
        Genre("ลามก", "ecchi")
    )

    override val pageListParseSelector = "div.reading-content p code img"

    override fun pageListParse(document: Document): List<Page> {
        countViews(document)

        return document.select(pageListParseSelector).mapIndexed { index, element ->
            Page(
                index,
                document.location(),
                element.let {
                    it.absUrl(if (it.hasAttr("data-src")) "data-src" else "src")
                }
            )
        }
    }
}
