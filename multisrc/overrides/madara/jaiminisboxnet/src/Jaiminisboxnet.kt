package eu.kanade.tachiyomi.extension.en.jaiminisboxnet

import eu.kanade.tachiyomi.multisrc.madara.Madara
import java.text.SimpleDateFormat
import java.util.Locale

class Jaiminisboxnet : Madara(
    "JaiminisBox.net",
    "https://jaiminisbox.net",
    "en",
    dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.US),
)
