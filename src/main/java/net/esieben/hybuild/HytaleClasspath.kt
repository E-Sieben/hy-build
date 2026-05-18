package net.esieben.hybuild

import net.esieben.hybuild.server.ExtractServerZipTask.Companion.VERSION_ZIP_PATTERN
import org.gradle.api.Project
import java.io.File

internal object HytaleClasspath {

    private const val HYTALE_GROUP    = "com.hypixel.hytale"
    private const val HYTALE_ARTIFACT = "HytaleServer"

    fun setup(project: Project, extension: HyBuildExtension) {
        val hytaleDir = project.file(HyBuild.PLUGIN_FOLDER)
        val mainJar   = File(hytaleDir, "HytaleServer.jar")

        if (extension.includeAIJavadoc.get()) {
            extractBundledSourcesJar(hytaleDir)
            val sourcesJar = File(hytaleDir, "HytaleServer-sources.jar")
            if (mainJar.exists() && sourcesJar.exists()) {
                val version = detectServerVersion(hytaleDir) ?: "local"
                installToMavenLocal(mainJar, sourcesJar, version)
                project.repositories.mavenLocal()
                project.dependencies.add("compileOnly", "$HYTALE_GROUP:$HYTALE_ARTIFACT:$version")
                return
            }
        }

        project.dependencies.add("compileOnly", project.files(mainJar))
    }

    private fun extractBundledSourcesJar(hytaleDir: File) {
        if (!hytaleDir.exists()) return
        val dest = File(hytaleDir, "HytaleServer-sources.jar")
        if (dest.exists()) return
        val stream = HytaleClasspath::class.java.getResourceAsStream("/HytaleServer-sources.jar") ?: return
        stream.use { input -> dest.outputStream().use { output -> input.copyTo(output) } }
    }

    private fun detectServerVersion(hytaleDir: File): String? =
        hytaleDir.listFiles()
            ?.firstOrNull { it.isFile && it.name.matches(VERSION_ZIP_PATTERN) }
            ?.nameWithoutExtension

    private fun installToMavenLocal(jar: File, sourcesJar: File, version: String) {
        val artifactDir = File(
            System.getProperty("user.home"),
            ".m2/repository/${HYTALE_GROUP.replace('.', '/')}/$HYTALE_ARTIFACT/$version"
        )
        artifactDir.mkdirs()

        val targetJar     = File(artifactDir, "$HYTALE_ARTIFACT-$version.jar")
        val targetSources = File(artifactDir, "$HYTALE_ARTIFACT-$version-sources.jar")
        val targetPom     = File(artifactDir, "$HYTALE_ARTIFACT-$version.pom")

        if (!targetJar.exists()     || targetJar.length()     != jar.length())        jar.copyTo(targetJar,     overwrite = true)
        if (!targetSources.exists() || targetSources.length() != sourcesJar.length()) sourcesJar.copyTo(targetSources, overwrite = true)
        if (!targetPom.exists()) targetPom.writeText(buildPom(version))
    }

    private fun buildPom(version: String) = """<?xml version="1.0" encoding="UTF-8"?>
<project>
  <modelVersion>4.0.0</modelVersion>
  <groupId>$HYTALE_GROUP</groupId>
  <artifactId>$HYTALE_ARTIFACT</artifactId>
  <version>$version</version>
  <packaging>jar</packaging>
</project>"""
}
