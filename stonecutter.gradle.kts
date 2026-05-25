plugins {
    id("dev.kikugie.stonecutter")
    id("net.neoforged.moddev") version "2.0.140" apply false
    id("me.modmuss50.mod-publish-plugin") version "1.0.+" apply false
}

stonecutter active "26.1-fabric"

stonecutter tasks {
    val ordering = versionComparator
        .thenComparingInt { if (it.metadata.project.endsWith("fabric")) 1 else 0 }

    order("publishModrinth", ordering)
    order("publishCurseforge", ordering)

}

stonecutter parameters {
    val (version, loader) = current.project.split('-', limit = 2)

    constants.match(loader, "fabric", "neoforge")
    properties.tags(version, loader)

    replacements {
        string(current.parsed < "26.0") {
            replace("GuiGraphicsExtractor", "GuiGraphics")
            replace("PlayerFaceExtractor", "PlayerFaceRenderer")
        }

        string(current.parsed < "1.21.11") {
            replace("Identifier", "ResourceLocation")
        }

        string(current.parsed <= "1.21.10", "!skip_replace") {
            replace("AutoConfigClient", "AutoConfig")
        }
    }
}