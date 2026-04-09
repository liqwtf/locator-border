@file:OptIn(StonecutterExperimentalAPI::class)

import dev.kikugie.stonecutter.StonecutterExperimentalAPI

plugins {
    id("dev.kikugie.loom-back-compat")
    id("me.modmuss50.mod-publish-plugin")
}

version = "${property("mod.version")}+${sc.current.version}-fabric"
base.archivesName = property("mod.id") as String

val requiredJava: JavaVersion = when {
    sc.current.parsed >= "26.1" -> JavaVersion.VERSION_25
    sc.current.parsed >= "1.20.5" -> JavaVersion.VERSION_21
    sc.current.parsed >= "1.18" -> JavaVersion.VERSION_17
    sc.current.parsed >= "1.17" -> JavaVersion.VERSION_16
    else -> JavaVersion.VERSION_1_8
}

// This can be used for publishing on Modrinth and Curseforge
val compatibleVersions: List<String> = sc.properties.rawOrNull("publish_versions")
    ?.asList().orEmpty().map { it.toString() }

repositories {
    fun strictMaven(url: String, alias: String, vararg groups: String) = exclusiveContent {
        forRepository { maven(url) { name = alias } }
        filter { groups.forEach(::includeGroup) }
    }

    strictMaven("https://www.cursemaven.com", "CurseForge", "curse.maven")
    strictMaven("https://api.modrinth.com/maven", "Modrinth", "maven.modrinth")

    maven("https://maven.terraformersmc.com/releases") { name = "Terraformers" } // Mod Menu
    maven("https://maven.shedaniel.me") { name = "shedaniel" } // Cloth Config
}

dependencies {
    fun fapi(vararg modules: String) {
        for (it in modules) modImplementation(fabricApi.module(it, sc.properties["fabric_api_version"]))
    }

    minecraft("com.mojang:minecraft:${sc.current.version}")
    loomx.applyMojangMappings() // Applies mappings to obfuscated versions

    modImplementation("net.fabricmc:fabric-loader:${property("loader_version")}")
    fapi("fabric-lifecycle-events-v1", "fabric-resource-loader-v0", "fabric-content-registries-v0")

    modImplementation("me.shedaniel.cloth:cloth-config-fabric:${property("cloth_config_version")}")
    modImplementation("com.terraformersmc:modmenu:${property("mod_menu_version")}")
}

loom {
    fabricModJsonPath = rootProject.file("src/main/resources/fabric.mod.json") // Useful for interface injection

    decompilerOptions.named("vineflower") {
        options.put("mark-corresponding-synthetics", "1") // Adds names to lambdas - useful for mixins
    }

    runConfigs.all {
        vmArgs("-Dmixin.debug.export=true")
        runDir = "../../run"
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
        exclude("**/neoforge.mods.toml")

        fun MutableMap<String, String>.register(key: String, property: String) {
            val value: String = sc.properties[property]
            inputs.property(key, value)
            set(key, value)
        }

        val properties = buildMap {
            put("version", project.version.toString())
            register("description", "description")
            register("sources", "sources_url")
            register("issues", "issues_url")
            register("loader", "loader_version")
            register("minecraft", "minecraft_version")
        }

        filesMatching("fabric.mod.json") { expand(properties) }

        val mixinJava = "JAVA_${requiredJava.majorVersion}"
        filesMatching("*.mixins.json") { expand("java" to mixinJava) }
    }

    register<Copy>("buildAndCollect") {
        group = "build"

        from(loomx.modJar.map { it.archiveFile }, loomx.modSourcesJar.map { it.archiveFile })
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

    file = loomx.modJar.map { it.archiveFile.get() }
    additionalFiles.from(loomx.modSourcesJar.map { it.archiveFile.get() })
    displayName = "Locator Border ${property("mod.version")} for ${sc.current.version} Fabric"
    version = project.version.toString()
    changelog = rootProject.file("CHANGELOG.md").readText()
    type = STABLE
    modLoaders.add("fabric")

    modrinth {
        projectId = property("publish.modrinth") as String
//        projectDescription = rootProject.file("README.md").readText()
        minecraftVersions.addAll(compatibleVersions)
        requires("fabric-api", "cloth-config")
        optional("modmenu")

        accessToken = token.modrinth
    }

    curseforge {
        projectId = property("publish.curseforge") as String
        minecraftVersions.addAll(compatibleVersions)
        javaVersions.add(requiredJava)
        clientRequired = true
        requires("fabric-api", "cloth-config")
        optional("modmenu")

        accessToken = token.curseforge
    }
}