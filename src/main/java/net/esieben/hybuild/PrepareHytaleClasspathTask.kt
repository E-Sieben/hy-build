package net.esieben.hybuild

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import java.io.File

abstract class PrepareHytaleClasspathTask : DefaultTask() {

    @get:Input
    abstract val includeAIJavadoc: Property<Boolean>

    @get:Input
    @get:Optional
    abstract val version: Property<String>

    @get:InputFile
    @get:Optional
    abstract val serverJar: RegularFileProperty

    @get:OutputFile
    abstract val extractedSourcesJar: RegularFileProperty

    @get:OutputDirectory
    abstract val mavenLocalArtifactDir: DirectoryProperty

    init {
        onlyIf("AI Javadoc is disabled") { includeAIJavadoc.get() }
    }

    @TaskAction
    fun prepare() {
        val ver = version.orNull?.takeIf { it.isNotBlank() }
        if (ver == null) {
            logger.lifecycle("PrepareHytaleClasspath: no server version found — skipping Maven Local install.")
            return
        }

        val jar = serverJar.orNull?.asFile
        if (jar == null || !jar.exists()) {
            logger.lifecycle("PrepareHytaleClasspath: HytaleServer.jar not found — skipping Maven Local install.")
            return
        }

        val sourcesJar = extractedSourcesJar.get().asFile
        extractBundledSourcesJar(sourcesJar)
        if (!sourcesJar.exists()) {
            logger.lifecycle("PrepareHytaleClasspath: bundled sources JAR not available — skipping Maven Local install.")
            return
        }

        installToMavenLocal(jar, sourcesJar, ver)
        logger.lifecycle("PrepareHytaleClasspath: installed $ver to Maven Local.")
    }

    private fun extractBundledSourcesJar(dest: File) {
        if (dest.exists()) return
        val stream =
            PrepareHytaleClasspathTask::class.java.getResourceAsStream("/HytaleServer-sources.jar")
                ?: return
        stream.use { input -> dest.outputStream().use { output -> input.copyTo(output) } }
    }

    private fun installToMavenLocal(jar: File, sourcesJar: File, version: String) {
        val artifactDir = mavenLocalArtifactDir.get().asFile
        artifactDir.mkdirs()

        val artifact = HytaleVersionSource.HYTALE_ARTIFACT
        jar.copyTo(File(artifactDir, "$artifact-$version.jar"), overwrite = true)
        sourcesJar.copyTo(File(artifactDir, "$artifact-$version-sources.jar"), overwrite = true)

        val pom = File(artifactDir, "$artifact-$version.pom")
        if (!pom.exists()) pom.writeText(buildPom(version))
    }

    private fun buildPom(version: String) = """<?xml version="1.0" encoding="UTF-8"?>
<project>
  <modelVersion>4.0.0</modelVersion>
  <groupId>${HytaleVersionSource.HYTALE_GROUP}</groupId>
  <artifactId>${HytaleVersionSource.HYTALE_ARTIFACT}</artifactId>
  <version>$version</version>
  <packaging>jar</packaging>
</project>"""
}
