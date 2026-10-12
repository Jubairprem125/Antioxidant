plugins {
    id("antioxidant.base-conventions")
    id("antioxidant.publish-conventions")
    id("antioxidant.modrinth-publish-conventions")
}

val antioxidantJarName = "Antioxidant-26.2.jar"

dependencies {
    implementation(project(":core"))
    include(project(":core"))
}

loom {
    accessWidenerPath = file("src/main/resources/antioxidant-client.accesswidener")
}

tasks {
    val copyJarTask = register<Copy>("copyAntioxidantClientJar") {
        group = "build"

        val jarTask = getByName<Jar>("jar")
        dependsOn(jarTask)

        from(jarTask.archiveFile)
        rename { antioxidantJarName }
        into(project.layout.buildDirectory.file("libs"))
    }

    named("build") {
        dependsOn(copyJarTask)
    }

    withType<Jar> {
        archiveAppendix = "client"
    }
}

modrinth {
    uploadFile.set(project.layout.buildDirectory.file("libs/$antioxidantJarName"))
}
