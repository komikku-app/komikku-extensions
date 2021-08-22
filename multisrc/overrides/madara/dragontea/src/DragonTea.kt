package eu.kanade.tachiyomi.extension.en.dragontea

import eu.kanade.tachiyomi.multisrc.madara.Madara
import java.text.SimpleDateFormat
import java.util.Locale

class DragonTea : Madara(
    "DragonTea",
    "https://dragontea.ink/",
    "en",
    dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.US)
)
