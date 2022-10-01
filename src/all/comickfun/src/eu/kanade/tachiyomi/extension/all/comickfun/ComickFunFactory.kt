package eu.kanade.tachiyomi.extension.all.comickfun

import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.SourceFactory

// A legacy mapping  of language codes to ensure that source IDs don't change
val legacyLanguageMappings = mapOf(
    "pt-br" to "pt-BR", // Brazilian Portuguese
    "zh-hk" to "zh-Hant", // Traditional Chinese,
    "zh" to "zh-Hans", // Simplified Chinese
).withDefault { it } // country code matches language code

class ComickFunFactory : SourceFactory {
    override fun createSources(): List<Source> = listOf(
        "all",
        "en",
        "pt-br",
        "ru",
        "fr",
        "es-419",
        "pl",
        "tr",
        "it",
        "es",
        "id",
        "hu",
        "vi",
        "zh-hk",
        "ar",
        "de",
        "zh",
        "ca",
        "bg",
        "th",
        "fa",
        "uk",
        "mn",
        "ro",
        "he",
        "ms",
        "tl",
        "ja",
        "hi",
        "my",
        "ko",
        "cs",
        "pt",
        "nl",
        "sv",
        "bn",
        "no",
        "lt",
        "el",
        "sr",
        "da"
    ).map { object : ComickFun(legacyLanguageMappings.getValue(it), it) {} }
}
