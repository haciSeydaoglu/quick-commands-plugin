plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.0.21"
    id("org.jetbrains.intellij.platform") version "2.13.1"
}

group = "com.quickcommands"
version = providers.gradleProperty("pluginVersion").get()

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity("2024.2")
        bundledPlugin("org.jetbrains.plugins.terminal")
        pluginVerifier()
        zipSigner()
    }
}

intellijPlatform {
    pluginConfiguration {
        version = providers.gradleProperty("pluginVersion")

        ideaVersion {
            sinceBuild = "242"
            untilBuild = "261.*"
        }

        changeNotes = provider {
            file("CHANGELOG.md").readText()
                .lines()
                .drop(1)
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
        }
    }

    signing {
        certificateChain = providers.environmentVariable("CERTIFICATE_CHAIN")
        privateKey = providers.environmentVariable("PRIVATE_KEY")
        password = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
    }

    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
    }
}
