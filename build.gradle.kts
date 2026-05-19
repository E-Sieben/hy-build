import org.gradle.plugin.compatibility.compatibility

plugins {
    id("java-gradle-plugin")
    id("com.gradle.plugin-publish").version("2.1.1")
    id("pl.allegro.tech.build.axion-release").version("1.21.1")
    id("org.gradle.plugin-compatibility").version("1.0.0")
    kotlin("jvm")
}

version = scmVersion.version
group = "net.esieben"

var packageName: String = "$group.${name.replace("-", "")}"
var mainClass: String =
    name.split("-").joinToString("") { it.replaceFirstChar { char -> char.uppercase() } }

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation("io.freefair.gradle:lombok-plugin:latest.release")
    testImplementation(gradleTestKit())
    testImplementation(kotlin("test"))
}

gradlePlugin {
    website = "https://github.com/E-Sieben/hy-build"
    vcsUrl = "https://github.com/E-Sieben/hy-build.git"
    plugins {
        register(name) {
            id = packageName
            implementationClass = "$packageName.$mainClass"
            // Plugin Portal Information
            displayName = mainClass
            description = "A Plugin made for Hytale Plugin creation, testing and releasing"
            tags = listOf("hytale", "kotlin", "tooling", "automation")

            compatibility {
                features {
                    configurationCache = true
                }
            }
        }
    }
}

kotlin {
    jvmToolchain(25)
}