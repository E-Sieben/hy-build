plugins {
    id("java-gradle-plugin")
    id("maven-publish")
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
    plugins {
        register(name) {
            id = packageName
            implementationClass = "$packageName.$mainClass"
        }
    }
}

kotlin {
    jvmToolchain(25)
}