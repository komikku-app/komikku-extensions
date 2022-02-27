plugins {
    id("com.android.library")
    kotlin("android")
}

android {
    compileSdkVersion(AndroidConfig.compileSdk)

    defaultConfig {
        minSdkVersion(AndroidConfig.minSdk)
        targetSdkVersion(AndroidConfig.targetSdk)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(Dependencies.kotlin.stdlib)
    compileOnly(Dependencies.okhttp)
    compileOnly(Dependencies.jsoup)
}
