@file:OptIn(StonecutterExperimentalAPI::class)

import dev.kikugie.stonecutter.StonecutterExperimentalAPI

plugins {
    id("net.neoforged.moddev")
    id("me.modmuss50.mod-publish-plugin")
}

version = "${property("mod.version")}+${sc.current.version}-neoforge"
base.archivesName = property("mod.id") as String

val requiredJava: JavaVersion = when {
    sc.current.parsed >= "26.1" -> JavaVersion.VERSION_25
    sc.current.parsed >= "1.20.5" -> JavaVersion.VERSION_21
    sc.current.parsed >= "1.18" -> JavaVersion.VERSION_17
    sc.current.parsed >= "1.17" -> JavaVersion.VERSION_16
    else -> JavaVersion.VERSION_1_8
}

// This can be used for publishing on Modrinth and Curseforge
val compatibleVersions: List<String> = sc.properties.rawOrNull("mod", "mc_releases")
    ?.asList().orEmpty().map { it.toString() }

repositories {
    fun strictMaven(url: String, alias: String, vararg groups: String) = exclusiveContent {
        forRepository { maven(url) { name = alias } }
        filter { groups.forEach(::includeGroup) }
    }

    strictMaven("https://www.cursemaven.com", "CurseForge", "curse.maven")
    strictMaven("https://api.modrinth.com/maven", "Modrinth", "maven.modrinth")

    maven("https://maven.shedaniel.me") { name = "shedaniel" } // Cloth Config
}

dependencies {
    implementation("me.shedaniel.cloth:cloth-config-neoforge:${property("deps.cloth_config")}")
}

neoForge {
    version = property("deps.neoforge") as String

    runs {
        register("client") {
            gameDirectory = file("../../run/")
            client()
        }
    }

    mods {
        register(property("mod.id") as String) {
            sourceSet(sourceSets["main"])
        }
    }
}

java {
    withSourcesJar()
    targetCompatibility = requiredJava
    sourceCompatibility = requiredJava

    toolchain {
        vendor = JvmVendorSpec.ADOPTIUM
        languageVersion = JavaLanguageVersion.of(requiredJava.majorVersion)
    }
}

tasks {
    processResources {
        exclude("**/fabric.mod.json")

        fun MutableMap<String, String>.register(key: String, property: String) {
            val value: String = sc.properties[property]
            inputs.property(key, value)
            set(key, value)
        }

        val props = buildMap {
            register("id", "mod.id")
            register("name", "mod.name")
            register("version", "mod.version")
            register("minecraft", "mod.minecraft")
        }

        filesMatching("META-INF/neoforge.mods.toml") { expand(props) }

        val mixinJava = "JAVA_${requiredJava.majorVersion}"
        filesMatching("*.mixins.json") { expand("java" to mixinJava) }
    }

//    named("createMinecraftArtifacts") {
//        dependsOn("stonecutterGenerate")
//    }

    // Builds the version into a shared folder in `build/libs/${mod version}/`
    register<Copy>("buildAndCollect") {
        group = "build"

        // loomx.mod(Sources)Jar returns the jar task for the applied loom variant
        from(jar.map { it.archiveFile })
        into(rootProject.layout.buildDirectory.file("libs/${project.property("mod.version")}"))
        dependsOn("build")
    }
}


publishMods {
    val token = object {
        val modrinth = findProperty("MODRINTH_TOKEN") as? String
        val curseforge = findProperty("CURSEFORGE_TOKEN") as? String
    }

    dryRun = token.modrinth == null || token.curseforge == null

    file = tasks.jar.map { it.archiveFile.get() }
    //additionalFiles.from(tasks.sourcesJar.map { it.archiveFile.get() })
    displayName = "${property("mod.name")} ${property("mod.version")} for ${sc.current.version} NeoForge"
    version = "${property("mod.version")}+${sc.current.version}-neoforge"
    changelog = rootProject.file("CHANGELOG.md").readText()
    type = STABLE
    modLoaders.add("neoforge")

    modrinth {
        projectId = property("publish.modrinth") as String
        minecraftVersions.addAll(compatibleVersions)
        requires("cloth-config")

        accessToken = token.modrinth
    }

    curseforge {
        projectId = property("publish.curseforge") as String
        minecraftVersions.addAll(compatibleVersions)
        javaVersions.add(requiredJava)
        clientRequired = true
        requires("cloth-config")

        accessToken = token.curseforge
    }
}