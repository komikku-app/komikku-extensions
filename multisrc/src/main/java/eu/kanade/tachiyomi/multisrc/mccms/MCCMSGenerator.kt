package eu.kanade.tachiyomi.multisrc.mccms

import generator.ThemeSourceData.SingleLang
import generator.ThemeSourceGenerator

class MCCMSGenerator : ThemeSourceGenerator {
    override val themeClass = "MCCMS"
    override val themePkg = "mccms"
    override val baseVersionCode = 5
    override val sources = listOf(
        SingleLang(
            name = "Haoman6",
            baseUrl = "https://www.haoman6.com",
            lang = "zh",
            className = "Haoman6",
            sourceName = "好漫6",
            overrideVersionCode = 3,
        ),
        SingleLang( // previously: app2.haoman6.com, app2.haomanwu.com
            name = "Haomanwu",
            baseUrl = "https://move.bookcomic.org",
            lang = "zh",
            className = "Haomanwu",
            sourceName = "好漫屋",
            overrideVersionCode = 3,
        ),
        SingleLang( // same as: www.haoman6.cc
            name = "Haoman6 (g-lens)",
            baseUrl = "https://www.g-lens.com",
            lang = "zh",
            className = "Haoman6glens",
            sourceName = "好漫6 (g-lens)",
            overrideVersionCode = 0,
        ),
        SingleLang( // same as: www.haoman8.com
            name = "Haoman8",
            baseUrl = "https://caiji.haoman8.com",
            lang = "zh",
            className = "Haoman8",
            sourceName = "好漫8",
            overrideVersionCode = 0,
        ),
        SingleLang(
            name = "Kuaikuai Manhua",
            baseUrl = "https://mobile.manhuaorg.com",
            lang = "zh",
            className = "Kuaikuai",
            sourceName = "快快漫画",
            overrideVersionCode = 0,
        ),
        SingleLang(
            name = "bz Manhua",
            baseUrl = "https://www2.pupumanhua.com",
            lang = "zh",
            className = "bzManhua",
            sourceName = "包子漫画搬运",
            overrideVersionCode = 0,
        ),
        // The following sources are from https://www.yy123.cyou/ and are configured to use MCCMSNsfw
        SingleLang( // 103=寄宿日记, same as: www.hanman.top (different URL format)
            name = "Damao Manhua",
            baseUrl = "https://www.hanman.cyou",
            lang = "zh",
            isNsfw = true,
            className = "DamaoManhua",
            sourceName = "大猫漫画",
            overrideVersionCode = 0,
        ),
        SingleLang( // 103=诡秘的姐妹
            name = "Heihei Manhua",
            baseUrl = "https://www.hhmh.cyou",
            lang = "zh",
            isNsfw = true,
            className = "HHMH",
            sourceName = "嘿嘿漫画",
            overrideVersionCode = 0,
        ),
        SingleLang( // 103=望月仙女傳說, same as: www.hanman.men
            name = "Tudou Manhua",
            baseUrl = "https://www.ptcomic.com",
            lang = "zh",
            isNsfw = true,
            className = "PtComic",
            sourceName = "土豆漫画",
            overrideVersionCode = 0,
        ),
        SingleLang( // 103=校园梦精记, same as: www.hmanwang.com, www.quanman8.com, www.lmmh.cc, www.xinmanba.com
            name = "Dida Manhua",
            baseUrl = "https://www.didamanhua.com",
            lang = "zh",
            isNsfw = true,
            className = "DidaManhua",
            sourceName = "嘀嗒漫画",
            overrideVersionCode = 0,
        ),
        SingleLang( // 103=脱身之法, same as: www.quanmanba.com, www.999mh.net
            name = "Dimanba",
            baseUrl = "https://www.dimanba.com",
            lang = "zh",
            isNsfw = true,
            className = "Dimanba",
            sourceName = "滴漫吧",
            overrideVersionCode = 0,
        ),
    )

    override fun createAll() {
        val userDir = System.getProperty("user.dir")!!
        sources.forEach {
            val themeClass = if (it.isNsfw) "MCCMSNsfw" else themeClass
            ThemeSourceGenerator.createGradleProject(it, themePkg, themeClass, baseVersionCode, userDir)
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            MCCMSGenerator().createAll()
        }
    }
}
