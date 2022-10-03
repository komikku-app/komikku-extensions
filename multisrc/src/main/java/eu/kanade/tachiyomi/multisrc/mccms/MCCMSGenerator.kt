package eu.kanade.tachiyomi.multisrc.mccms

import generator.ThemeSourceData.SingleLang
import generator.ThemeSourceGenerator

class MCCMSGenerator : ThemeSourceGenerator {
    override val themeClass = "MCCMS"
    override val themePkg = "mccms"
    override val baseVersionCode = 3
    override val sources = listOf(
        SingleLang(
            name = "Haoman6", baseUrl = "https://www.haoman6.com", lang = "zh",
            className = "Haoman6", sourceName = "好漫6", overrideVersionCode = 3
        ),
        SingleLang(
            name = "Haoman6 (g-lens)", baseUrl = "https://www.g-lens.com", lang = "zh",
            className = "Haoman6_glens", sourceName = "好漫6 (g-lens)", overrideVersionCode = 0
        ),
        SingleLang( // 与 caiji.haoman8.com 相同
            name = "Haoman8", baseUrl = "https://www.haoman8.com", lang = "zh",
            className = "Haoman8", sourceName = "好漫8", overrideVersionCode = 0
        ),
        SingleLang(
            name = "Pupu Manhua", baseUrl = "https://app.manhuaorg.com", lang = "zh",
            className = "Manhuaorg", sourceName = "朴朴漫画", overrideVersionCode = 2
        ),
        SingleLang(
            name = "PPHanman", baseUrl = "https://pphm.xyz", lang = "zh", isNsfw = true,
            className = "PPHanman", sourceName = "PP韩漫", overrideVersionCode = 0
        ),
    )

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            MCCMSGenerator().createAll()
        }
    }
}
