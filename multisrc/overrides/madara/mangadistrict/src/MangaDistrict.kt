package eu.kanade.tachiyomi.extension.en.mangadistrict

import eu.kanade.tachiyomi.annotations.Nsfw
import eu.kanade.tachiyomi.multisrc.madara.Madara
import java.text.SimpleDateFormat
import java.util.Locale

@Nsfw
class MangaDistrict : Madara(
    "Manga District",
    "https://mangadistrict.com",
    "en",
    dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.US)
)
