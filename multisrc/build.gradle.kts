import java.io.BufferedReader
import java.io.InputStreamReader

plugins {
    id("com.android.library")
    kotlin("android")
    id("kotlinx-serialization")
}

android {
    compileSdk = AndroidConfig.compileSdk

    defaultConfig {
        minSdk = 29
        targetSdk = AndroidConfig.targetSdk
    }

    kotlinOptions {
        freeCompilerArgs += "-opt-in=kotlinx.serialization.ExperimentalSerializationApi"
    }
}

repositories {
    mavenCentral()
}

configurations {
    compileOnly {
        isCanBeResolved = true
    }
}

dependencies {
    compileOnly(libs.bundles.common)
}

tasks {
    val generateExtensions by registering {
        doLast {
            val isWindows = System.getProperty("os.name").toString().toLowerCase().contains("win")
            var classPath = (
                    configurations.compileOnly.get().asFileTree.toList() +
                    listOf(
                        configurations.androidApis.get().asFileTree.first().absolutePath, // android.jar path
                        "$projectDir/build/intermediates/aar_main_jar/debug/classes.jar" // jar made from this module
                    ))
                .joinToString(if (isWindows) ";" else ":")

            var javaPath = "${System.getProperty("java.home")}/bin/java"

            val mainClass = "generator.GeneratorMainKt" // Main class we want to execute

            if (isWindows) {
                classPath = classPath.replace("/", "\\")
                javaPath = javaPath.replace("/", "\\")
            }

            val javaProcess = ProcessBuilder()
                .directory(null).command(javaPath, "-classpath", classPath, mainClass)
                .redirectErrorStream(true).start()

            val inputStreamReader = InputStreamReader(javaProcess.inputStream)
            val bufferedReader = BufferedReader(inputStreamReader)

            var s: String?
            while (bufferedReader.readLine().also { s = it } != null) {
                logger.info(s)
            }

            bufferedReader.close()
            inputStreamReader.close()

            val exitCode = javaProcess.waitFor()
            if (exitCode != 0) {
                throw Exception("Java process failed with exit code: $exitCode")
            }
        }
        dependsOn("ktLint", "assembleDebug")
    }

    register<org.jmailen.gradle.kotlinter.tasks.LintTask>("ktLint") {
        if (project.hasProperty("theme")) {
            val theme = project.property("theme")
            source(files("src/main/java/eu/kanade/tachiyomi/multisrc/$theme", "overrides/$theme"))
            return@register
        }
        source(files("src", "overrides"))
    }

    register<org.jmailen.gradle.kotlinter.tasks.FormatTask>("ktFormat") {
        if (project.hasProperty("theme")) {
            val theme = project.property("theme")
            source(files("src/main/java/eu/kanade/tachiyomi/multisrc/$theme", "overrides/$theme"))
            return@register
        }
        source(files("src", "overrides"))
    }
}
