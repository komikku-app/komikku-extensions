package eu.kanade.tachiyomi.extension.pt.cervoscanlator

import eu.kanade.tachiyomi.multisrc.madara.Madara
import java.text.SimpleDateFormat
import java.util.Locale

class CervoScanlator : Madara(
    "Cervo Scanlator",
    "https://cervoscan.xyz",
    "pt-BR",
    SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
) {

    override val altName: String = "Nome alternativo: "

    override fun popularMangaSelector() = "div.page-item-detail.manga"
}
