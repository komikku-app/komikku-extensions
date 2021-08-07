package eu.kanade.tachiyomi.extension.en.mangamoli

import eu.kanade.tachiyomi.multisrc.madara.Madara
import java.text.SimpleDateFormat
import java.util.Locale

class MangaMoli : Madara(
    "MangaMoli",
    "https://mangamoli.com",
    "en",
    dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.US)
)
