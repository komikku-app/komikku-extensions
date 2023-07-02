include(":core")

listOf("dataimage", "unpacker", "cryptoaes", "textinterceptor", "synchrony").forEach {
    include(":lib-$it")
    project(":lib-$it").projectDir = File("lib/$it")
}

if (System.getenv("CI") == null || System.getenv("CI_MODULE_GEN") == "true") {
    // Local development (full project build)

    include(":multisrc")
    project(":multisrc").projectDir = File("multisrc")

    // Loads all extensions
    File(rootDir, "src").eachDir { dir ->
        dir.eachDir { subdir ->
            val name = ":extensions:individual:${dir.name}:${subdir.name}"
            include(name)
            project(name).projectDir = File("src/${dir.name}/${subdir.name}")
        }
    }
    // Loads all generated extensions from multisrc
    File(rootDir, "generated-src").eachDir { dir ->
        dir.eachDir { subdir ->
            val name = ":extensions:multisrc:${dir.name}:${subdir.name}"
            include(name)
            project(name).projectDir = File("generated-src/${dir.name}/${subdir.name}")
        }
    }

    /**
     * If you're developing locally and only want to work with a single module,
     * comment out the parts above and uncomment below.
     */
//    val lang = "all"
//    val name = "mangadex"
//    val projectName = ":extensions:individual:$lang:$name"
//    val projectName = ":extensions:multisrc:$lang:$name"
//    include(projectName)
//    project(projectName).projectDir = File("src/${lang}/${name}")
//    project(projectName).projectDir = File("generated-src/${lang}/${name}")
} else {
    // Running in CI (GitHub Actions)

    val isMultisrc = System.getenv("CI_MULTISRC") == "true"
    val chunkSize = System.getenv("CI_CHUNK_SIZE").toInt()
    val chunk = System.getenv("CI_CHUNK_NUM").toInt()

    if (isMultisrc) {
        include(":multisrc")
        project(":multisrc").projectDir = File("multisrc")

        // Loads generated extensions from multisrc
        File(rootDir, "generated-src").getChunk(chunk, chunkSize)?.forEach {
            val name = ":extensions:multisrc:${it.parentFile.name}:${it.name}"
            println(name)
            include(name)
            project(name).projectDir = File("generated-src/${it.parentFile.name}/${it.name}")
        }
    } else {
        // Loads individual extensions
        File(rootDir, "src").getChunk(chunk, chunkSize)?.forEach {
            val name = ":extensions:individual:${it.parentFile.name}:${it.name}"
            println(name)
            include(name)
            project(name).projectDir = File("src/${it.parentFile.name}/${it.name}")
        }
    }
}

fun File.getChunk(chunk: Int, chunkSize: Int): List<File>? {
    return listFiles()
        // Lang folder
        ?.filter { it.isDirectory }
        // Extension subfolders
        ?.mapNotNull { dir -> dir.listFiles()?.filter { it.isDirectory } }
        ?.flatten()
        ?.sortedBy { it.name }
        ?.chunked(chunkSize)
        ?.get(chunk)
}

fun File.eachDir(block: (File) -> Unit) {
    listFiles()?.filter { it.isDirectory }?.forEach { block(it) }
}
