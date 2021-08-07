package eu.kanade.tachiyomi.extension.en.hm2d

import eu.kanade.tachiyomi.multisrc.madara.Madara
import eu.kanade.tachiyomi.annotations.Nsfw
import java.text.SimpleDateFormat
import java.util.Locale

@Nsfw
class HM2D : Madara(
    "HM2D",
    "https://mangadistrict.com/hdoujin/",
    "en",
    dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.US)
)
