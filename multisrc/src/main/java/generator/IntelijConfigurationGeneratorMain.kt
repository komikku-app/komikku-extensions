package generator

import java.io.File

/**
 * Finds all themes and creates an Intellij Idea run configuration for their generators
 * Should be run after creation/deletion of each theme
 */
fun main(args: Array<String>) {
    val userDir = System.getProperty("user.dir")!!
    val sourcesDirPath = "$userDir/multisrc/src/main/java/eu/kanade/tachiyomi/multisrc"
    val sourcesDir = File(sourcesDirPath)

    // cleanup from past runs
    File("$userDir/.run").apply {
        if (exists())
            deleteRecursively()
        mkdirs()
    }

    // find all theme packages
    sourcesDir.list()!!
        .filter { File(sourcesDir, it).isDirectory }
        .forEach { themeSource ->
            // Find all XxxGenerator.kt files
            File("$sourcesDirPath/$themeSource").list()!!
                .filter { it.endsWith("Generator.kt") }
                .map { it.substringBefore(".kt") }
                .forEach { generatorClass ->
                    val file = File("$userDir/.run/$generatorClass.run.xml")
                    val intellijConfStr = """
                        <component name="ProjectRunConfigurationManager">
                          <configuration default="false" name="$generatorClass" type="JetRunConfigurationType" nameIsGenerated="true">
                            <module name="tachiyomi-extensions.multisrc" />
                            <option name="VM_PARAMETERS" value="" />
                            <option name="PROGRAM_PARAMETERS" value="" />
                            <option name="ALTERNATIVE_JRE_PATH_ENABLED" value="false" />
                            <option name="ALTERNATIVE_JRE_PATH" />
                            <option name="PASS_PARENT_ENVS" value="true" />
                            <option name="MAIN_CLASS_NAME" value="eu.kanade.tachiyomi.multisrc.$themeSource.$generatorClass" />
                            <option name="WORKING_DIRECTORY" value="" />
                            <method v="2">
                              <option name="Make" enabled="true" />
                              <option name="Gradle.BeforeRunTask" enabled="true" tasks="ktFormat" externalProjectPath="${'$'}PROJECT_DIR${'$'}/multisrc" vmOptions="" scriptParameters="" />
                              <option name="Gradle.BeforeRunTask" enabled="true" tasks="ktLint" externalProjectPath="${'$'}PROJECT_DIR${'$'}/multisrc" vmOptions="" scriptParameters="" />
                            </method>
                          </configuration>
                        </component>
                    """.trimIndent()
                    file.writeText(intellijConfStr)

                    // Find Java class and extract method lists
                    Class.forName("eu/kanade/tachiyomi/multisrc/$themeSource/$generatorClass".replace("/", ".").substringBefore(".kt"))
                        .methods
                        .find { it.name == "main" }
                }
        }
}
