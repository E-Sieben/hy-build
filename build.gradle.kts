plugins {
    id("java-gradle-plugin")
    id("pl.allegro.tech.build.axion-release").version("1.21.1")
}

version = scmVersion.version
group = "net.esieben"
var packageName: String = "$group.${name.replace("-", "")}"
var mainClass: String =
    name.split("-").joinToString("") { it.replaceFirstChar { char -> char.uppercase() } }

repositories {
    mavenCentral()
}

dependencies {}

gradlePlugin {
    plugins {
        register(name) {
            id = packageName
            implementationClass = "$packageName.$mainClass"
        }
    }
}