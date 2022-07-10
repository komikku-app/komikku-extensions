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
            className = "Haoman6", sourceName = "好漫6", overrideVersionCode = 2
        ),
        SingleLang( // 与 app2.haomanwu.com 相同
            name = "Haomanwu", baseUrl = "https://app2.haoman6.com", lang = "zh",
            className = "Haomanwu", sourceName = "好漫屋", overrideVersionCode = 3
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
            name = "Haomanwu (www)", baseUrl = "https://www.haomanwu.com", lang = "zh",
            className = "Haomanwu_www", sourceName = "好漫屋 (网页)", overrideVersionCode = 0
        ),
        SingleLang( // 与 app.manhuaorg.com 相同（部分渠道记为“好漫2”）
            name = "Pupu Manhua", baseUrl = "https://www.manhuaorg.com", lang = "zh",
            className = "Manhuaorg", sourceName = "朴朴漫画", overrideVersionCode = 0
        ),
    )

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            MCCMSGenerator().createAll()
        }
    }
}
