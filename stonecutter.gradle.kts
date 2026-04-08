plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter active "26.1-fabric"

// See https://stonecutter.kikugie.dev/wiki/config/params
stonecutter parameters {
    swaps["mod_id"] = "\"${property("mod.id")}\";"
    dependencies["fapi"] = node.project.property("deps.fabric_api") as String

    replacements {
        string(current.parsed < "26.1") {
            replace("GuiGraphicsExtractor", "GuiGraphics")
            replace("PlayerFaceExtractor", "PlayerFaceRenderer")
        }

        string(current.parsed <= "1.21.10", "!skip_replace") {
            replace("AutoConfigClient", "AutoConfig")
        }
    }
}
