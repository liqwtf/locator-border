pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/") { name = "Fabric"}
        maven("https://maven.kikugie.dev/releases") { name = "KikuGie Releases" }
        maven("https://maven.kikugie.dev/snapshots") { name = "KikuGie Snapshots" }
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.9.1-beta.4"
    id("dev.kikugie.loom-back-compat") version "0.2"
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

stonecutter {
    create(rootProject) {
        fun match(version: String, loaders: List<String>) = loaders
            .forEach { version("$version-$it", version).buildscript("build.$it.gradle.kts") }

        match("26.1", loaders = listOf("fabric"))
        match("1.21.11", loaders = listOf("fabric"))
        match("1.21.9", loaders = listOf("fabric"))
        match("1.21.6", loaders = listOf("fabric"))

        vcsVersion = "26.1-fabric"
    }
}

rootProject.name = "locator-border"