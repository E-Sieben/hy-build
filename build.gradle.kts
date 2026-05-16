plugins {
    id("java-gradle-plugin")
    id("com.gradle.plugin-publish").version("2.1.1")
    id("pl.allegro.tech.build.axion-release").version("1.21.1")
    kotlin("jvm")
}

version = scmVersion.version
group = "net.esieben"
var packageName: String = "$group.${name.replace("-", "")}"
var mainClass: String =
    name.split("-").joinToString("") { it.replaceFirstChar { char -> char.uppercase() } }

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(gradleTestKit())
    testImplementation(kotlin("test"))
}

gradlePlugin {
    website = "https://github.com/E-Sieben/hy-build"
    vcsUrl = "https://github.com/E-Sieben/hy-build.git"
    plugins {
        register(name) {
            id = packageName
            displayName = mainClass
            description = "A Plugin made for Hytale Plugin creation, testing and releasing"
            implementationClass = "$packageName.$mainClass"
            tags = listOf("hytale", "kotlin")
        }
    }
}

kotlin {
    jvmToolchain(25)
}