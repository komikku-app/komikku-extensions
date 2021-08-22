package eu.kanade.tachiyomi.extension.en.hentaixcomic

import eu.kanade.tachiyomi.annotations.Nsfw
import eu.kanade.tachiyomi.multisrc.madara.Madara
import java.text.SimpleDateFormat
import java.util.Locale

@Nsfw
class HentaiXComic : Madara(
    "HentaiXComic",
    "https://hentaixcomic.com",
    "en",
    dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.US)
)
