plugins {
    id("dev.kikugie.stonecutter")
    id("me.modmuss50.mod-publish-plugin") version "1.0.+" apply false
}

stonecutter active "26.1-fabric"

stonecutter tasks {
    order("publishModrinth")
    order("publishCurseforge")
}

stonecutter parameters {
    swaps["mod_id"] = "\"${property("mod.id")}\";"
    dependencies["fapi"] = node.project.property("deps.fabric_api") as String

    replacements {
        string(current.parsed < "26.0") {
            replace("GuiGraphicsExtractor", "GuiGraphics")
            replace("PlayerFaceExtractor", "PlayerFaceRenderer")
        }

        string(current.parsed <= "1.21.10", "!skip_replace") {
            replace("AutoConfigClient", "AutoConfig")
        }
    }
}