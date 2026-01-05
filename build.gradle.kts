plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.25"
    id("org.jetbrains.intellij") version "1.17.4"
}

group = "com.quickcommands"
version = providers.gradleProperty("pluginVersion").get()

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(21)
}

intellij {
    version.set("2024.2")
    type.set("IC")
    plugins.set(listOf("terminal"))
}

tasks {
    patchPluginXml {
        sinceBuild.set("242")
        untilBuild.set("253.*")
        version.set(providers.gradleProperty("pluginVersion"))
        changeNotes.set(provider {
            file("CHANGELOG.md").readText()
                .lines()
                .drop(1) // "# Changelog" başlığını atla
                .dropWhile { it.isBlank() }
                .takeWhile { it.isNotBlank() || !it.startsWith("## [") }
                .joinToString("<br>") { line ->
                    when {
                        line.startsWith("## [") -> "<b>${line.removePrefix("## ")}</b>"
                        line.startsWith("- ") -> "• ${line.removePrefix("- ")}"
                        else -> line
                    }
                }
                .trim()
        })
    }

    wrapper {
        gradleVersion = "8.10"
    }

    // Plugin imzalama yapılandırması
    // Ortam değişkenleri: CERTIFICATE_CHAIN, PRIVATE_KEY, PRIVATE_KEY_PASSWORD
    signPlugin {
        certificateChain.set(providers.environmentVariable("CERTIFICATE_CHAIN"))
        privateKey.set(providers.environmentVariable("PRIVATE_KEY"))
        password.set(providers.environmentVariable("PRIVATE_KEY_PASSWORD"))
    }

    // Marketplace'e yayınlama yapılandırması
    // Ortam değişkeni: PUBLISH_TOKEN
    publishPlugin {
        token.set(providers.environmentVariable("PUBLISH_TOKEN"))
        // channels.set(listOf("beta")) // Beta kanalı için açılabilir
    }
}
