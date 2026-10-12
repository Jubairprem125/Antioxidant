plugins {
    id("antioxidant.base-conventions")
    id("antioxidant.publish-conventions")
}

dependencies {
    implementation(project(":core"))
}

loom {
    accessWidenerPath = file("src/main/resources/antioxidant-datagen.accesswidener")
    runs.clear()
}

tasks.withType<Jar> {
    archiveAppendix = "datagen"
}
