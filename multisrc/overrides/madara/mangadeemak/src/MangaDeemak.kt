package eu.kanade.tachiyomi.extension.th.mangadeemak

import eu.kanade.tachiyomi.multisrc.madara.Madara
import java.text.SimpleDateFormat
import java.util.Locale

class MangaDeemak : Madara("MangaDeemak", "https://mangadeemak.com", "th", SimpleDateFormat("d MMMM yyyy", Locale("th"))) {
    override fun getGenreList() = listOf(
        Genre("Action แอคชั่น", "action"),
        Genre("Adult ผู้ใหญ่", "adult"),
        Genre("Adventure ผจญภัย", "adventure"),
        Genre("Comedy ตลก", "comedy"),
        Genre("Crime อาชญากรรม", "อาชญากรรม"),
        Genre("Demon", "demon"),
        Genre("Detective สืบสวน", "detective"),
        Genre("Drama ดราม่า", "drama"),
        Genre("Ecchi เอดชิ ลามก", "ecchi"),
        Genre("Fantasy แฟนตาซี", "fantasy"),
        Genre("Gore", "gore"),
        Genre("Harem ฮาเร็ม", "harem"),
        Genre("Horror สยองขวัญ", "horror"),
        Genre("Isekai ต่างโลก", "isekai"),
        Genre("Loli", "loli"),
        Genre("Magic เวทย์มนต์", "magic"),
        Genre("Martial arts ศิลปะการต่อสู้", "martial-arts"),
        Genre("Mature", "mature"),
        Genre("Monster Girl", "monster-girl"),
        Genre("Moster", "moster"),
        Genre("Mystery ลึกลับ", "mystery"),
        Genre("One shot", "one-shot"),
        Genre("Romance โรแมนติก", "romance"),
        Genre("School โรงเรียน", "school"),
        Genre("Sci-fi ไซ-ไฟ", "sci-fi"),
        Genre("Second Life", "second-life"),
        Genre("Seinen", "seinen"),
        Genre("Shota", "shota"),
        Genre("Shoujo", "shoujo"),
        Genre("Shounen", "shounen"),
        Genre("Shounen Ai", "shounen-ai"),
        Genre("Smut", "smut"),
        Genre("Superhero ซุปเปอร์ฮีโร่", "superhero-ซุปเปอร์ฮีโร่"),
        Genre("Tragedy โศกนาฏกรรม", "tragedy-โศกนาฏกรรม"),
        Genre("Trap กับดัก", "trap-กับดัก"),
        Genre("VR Virtual Reality", "vr-virtual-reality"),
        Genre("Web Comic", "web-comic"),
        Genre("Webtoon", "webtoon"),
        Genre("Zombie ซอมบี้", "zombie"),
        Genre("การ์ตูน", "cartoon"),
        Genre("กีฬา", "sports"),
        Genre("คอมมิค", "comic"),
        Genre("จิตวิทยา", "จิตวิทยา"),
        Genre("ซุปเปอร์ พาวเวอร์", "super-power"),
        Genre("ทหาร", "military"),
        Genre("ทำอาหาร", "cooking"),
        Genre("ประวัติศาสตร์", "historical"),
        Genre("มังงะจบแล้ว", "manga-ending"),
        Genre("มังงะจีน", "manhua"),
        Genre("มังงะญีปุ่น", "manga-japan"),
        Genre("มังงะยอดนิยม", "manga-popular"),
        Genre("มังงะยังไม่จบ", "manga-updating"),
        Genre("มังงะเกาหลี", "manhwa"),
        Genre("ยาโอย Yaoi", "yaoi"),
        Genre("ยูริ Yuri", "yuri"),
        Genre("สลับเพศ", "gender-bender"),
        Genre("ส่วนหนึ่งของชีวิต", "ส่วนหนึ่งของชีวิต"),
        Genre("อนิเมะ", "anime"),
        Genre("เมือง", "เมือง"),
        Genre("เหนือธรรมชาติ", "supernatural"),
        Genre("เอาชีวิตรอด", "survival"),
        Genre("โดจิน", "doujinshi")
    )

    override fun popularMangaSelector() = "div.mangalist-content"

    override val popularMangaUrlSelector = "div.title a"
}
