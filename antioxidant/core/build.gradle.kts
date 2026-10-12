plugins {
    id("antioxidant.base-conventions")
    id("antioxidant.publish-conventions")
}

loom {
    accessWidenerPath = file("src/main/resources/antioxidant.accesswidener")

    runs.clear()
}

tasks.withType<Jar> {
    archiveAppendix = "core"
}
