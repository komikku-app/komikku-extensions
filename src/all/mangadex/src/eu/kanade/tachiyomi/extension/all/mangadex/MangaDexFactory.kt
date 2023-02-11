package eu.kanade.tachiyomi.extension.all.mangadex

import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.SourceFactory

class MangaDexFactory : SourceFactory {
    override fun createSources(): List<Source> = listOf(
        MangaDexEnglish(),
        MangaDexArabic(),
        MangaDexBengali(),
        MangaDexBulgarian(),
        MangaDexBurmese(),
        MangaDexCatalan(),
        MangaDexChineseSimplified(),
        MangaDexChineseTraditional(),
        MangaDexCzech(),
        MangaDexDanish(),
        MangaDexDutch(),
        MangaDexFilipino(),
        MangaDexFinnish(),
        MangaDexFrench(),
        MangaDexGerman(),
        MangaDexGreek(),
        MangaDexHebrew(),
        MangaDexHindi(),
        MangaDexHungarian(),
        MangaDexIndonesian(),
        MangaDexItalian(),
        MangaDexJapanese(),
        MangaDexKazakh(),
        MangaDexKorean(),
        MangaDexLatin(),
        MangaDexLithuanian(),
        MangaDexMalay(),
        MangaDexMongolian(),
        MangaDexNepali(),
        MangaDexNorwegian(),
        MangaDexPersian(),
        MangaDexPolish(),
        MangaDexPortugueseBrazil(),
        MangaDexPortuguesePortugal(),
        MangaDexRomanian(),
        MangaDexRussian(),
        MangaDexSerboCroatian(),
        MangaDexSpanishLatinAmerica(),
        MangaDexSpanishSpain(),
        MangaDexSwedish(),
        MangaDexTamil(),
        MangaDexThai(),
        MangaDexTurkish(),
        MangaDexUkrainian(),
        MangaDexVietnamese(),
    )
}

class MangaDexArabic : MangaDex("ar", "ar")
class MangaDexBengali : MangaDex("bn", "bn")
class MangaDexBulgarian : MangaDex("bg", "bg")
class MangaDexBurmese : MangaDex("my", "my")
class MangaDexCatalan : MangaDex("ca", "ca")
class MangaDexChineseSimplified : MangaDex("zh-Hans", "zh")
class MangaDexChineseTraditional : MangaDex("zh-Hant", "zh-hk")
class MangaDexCzech : MangaDex("cs", "cs")
class MangaDexDanish : MangaDex("da", "da")
class MangaDexDutch : MangaDex("nl", "nl")
class MangaDexEnglish : MangaDex("en", "en")
class MangaDexFilipino : MangaDex("fil", "tl")
class MangaDexFinnish : MangaDex("fi", "fi")
class MangaDexFrench : MangaDex("fr", "fr")
class MangaDexGerman : MangaDex("de", "de")
class MangaDexGreek : MangaDex("el", "el")
class MangaDexHebrew : MangaDex("he", "he")
class MangaDexHindi : MangaDex("hi", "hi")
class MangaDexHungarian : MangaDex("hu", "hu")
class MangaDexIndonesian : MangaDex("id", "id")
class MangaDexItalian : MangaDex("it", "it")
class MangaDexJapanese : MangaDex("ja", "ja")
class MangaDexKazakh : MangaDex("kk", "kk")
class MangaDexKorean : MangaDex("ko", "ko")
class MangaDexLatin : MangaDex("la", "la")
class MangaDexLithuanian : MangaDex("lt", "lt")
class MangaDexMalay : MangaDex("ms", "ms")
class MangaDexMongolian : MangaDex("mn", "mn")
class MangaDexNepali : MangaDex("ne", "ne")
class MangaDexNorwegian : MangaDex("no", "no")
class MangaDexPersian : MangaDex("fa", "fa")
class MangaDexPolish : MangaDex("pl", "pl")
class MangaDexPortugueseBrazil : MangaDex("pt-BR", "pt-br")
class MangaDexPortuguesePortugal : MangaDex("pt", "pt")
class MangaDexRomanian : MangaDex("ro", "ro")
class MangaDexRussian : MangaDex("ru", "ru")
class MangaDexSerboCroatian : MangaDex("sh", "sh")
class MangaDexSpanishLatinAmerica : MangaDex("es-419", "es-la")
class MangaDexSpanishSpain : MangaDex("es", "es")
class MangaDexSwedish : MangaDex("sv", "sv")
class MangaDexTamil : MangaDex("ta", "ta")
class MangaDexThai : MangaDex("th", "th")
class MangaDexTurkish : MangaDex("tr", "tr")
class MangaDexUkrainian : MangaDex("uk", "uk")
class MangaDexVietnamese : MangaDex("vi", "vi")
