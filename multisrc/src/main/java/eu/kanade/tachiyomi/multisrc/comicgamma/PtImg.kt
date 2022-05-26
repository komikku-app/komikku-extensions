package eu.kanade.tachiyomi.multisrc.comicgamma

import kotlinx.serialization.Serializable

val COORD_REGEX = Regex("""^i:(\d+),(\d+)\+(\d+),(\d+)>(\d+),(\d+)$""")

@Serializable
data class PtImg(val resources: Resource, val views: List<View>) {
    fun getFilename() = resources.i.src
    fun getViewSize() = Pair(views[0].width, views[0].height)
    fun getTranslations() = views[0].coords.map(::toTranslation)

    private fun toTranslation(coord: String): Translation {
        val v = COORD_REGEX.matchEntire(coord)!!.destructured.toList().map(String::toInt)
        return Translation(v[0], v[1], v[2], v[3], v[4], v[5])
    }
}

@Serializable
data class Resource(val i: Image)

@Serializable
data class Image(val src: String, val width: Int, val height: Int)

@Serializable
data class View(val width: Int, val height: Int, val coords: List<String>)

data class Translation(val ix: Int, val iy: Int, val w: Int, val h: Int, val vx: Int, val vy: Int)
