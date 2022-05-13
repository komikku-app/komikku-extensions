package eu.kanade.tachiyomi.multisrc.mccms

import generator.ThemeSourceData.SingleLang
import generator.ThemeSourceGenerator

class MCCMSGenerator : ThemeSourceGenerator {
    override val themeClass = "MCCMS"
    override val themePkg = "mccms"
    override val baseVersionCode = 1
    override val sources = listOf(
        SingleLang("Haoman6", "https://www.haoman6.com", "zh", className = "Haoman6", overrideVersionCode = 1),
        SingleLang("Haomanwu", "https://app2.haomanwu.com", "zh", className = "Haomanwu", overrideVersionCode = 2),
        SingleLang("Haoman6 (g-lens)", "https://www.g-lens.com", "zh", className = "Haoman6_glens", overrideVersionCode = 0),
        SingleLang("Haoman8", "https://caiji.haoman8.com", "zh", className = "Haoman8", overrideVersionCode = 0),
    )

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            MCCMSGenerator().createAll()
        }
    }
}
