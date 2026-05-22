package net.esieben.hybuild.project

import net.esieben.hybuild.HytaleVersionSource
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault
import java.io.File

@DisableCachingByDefault(because = "Installs artifacts to Maven Local — a shared mutable store that cannot be safely shared across builds")
abstract class PrepareHytaleClasspathTask : DefaultTask() {

    @get:Input
    abstract val includeAIJavadoc: Property<Boolean>

    @get:Input
    @get:Optional
    abstract val version: Property<String>

    @get:InputFile
    @get:Optional
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val serverJar: RegularFileProperty

    @get:OutputFile
    abstract val extractedJavadocJar: RegularFileProperty

    @get:OutputDirectory
    abstract val mavenLocalArtifactDir: DirectoryProperty

    init {
        onlyIf("AI Javadoc is enabled") { includeAIJavadoc.get() }
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

        val javadocJar = extractedJavadocJar.get().asFile
        extractBundledJavadocJar(javadocJar)
        if (!javadocJar.exists()) {
            logger.lifecycle("PrepareHytaleClasspath: bundled javadoc JAR not available — skipping Maven Local install.")
            return
        }

        installToMavenLocal(jar, javadocJar, ver)
        logger.lifecycle("PrepareHytaleClasspath: installed $ver to Maven Local.")
    }

    private fun extractBundledJavadocJar(dest: File) {
        if (dest.exists()) return
        val stream =
            PrepareHytaleClasspathTask::class.java.getResourceAsStream("/HytaleServer-javadoc.jar")
                ?: return
        stream.use { input -> dest.outputStream().use { output -> input.copyTo(output) } }
    }

    private fun installToMavenLocal(jar: File, javadocJar: File, version: String) {
        val artifactDir = mavenLocalArtifactDir.get().asFile
        artifactDir.mkdirs()

        val artifact = HytaleVersionSource.HYTALE_ARTIFACT
        jar.copyTo(File(artifactDir, "$artifact-$version.jar"), overwrite = true)
        javadocJar.copyTo(File(artifactDir, "$artifact-$version-javadoc.jar"), overwrite = true)

        val pom = File(artifactDir, "$artifact-$version.pom")
        if (!pom.exists()) pom.writeText(buildPom(version))

        val module = File(artifactDir, "$artifact-$version.module")
        if (!module.exists()) module.writeText(buildModuleMetadata(version))
    }

    private fun buildPom(version: String) = """<?xml version="1.0" encoding="UTF-8"?>
<project>
  <modelVersion>4.0.0</modelVersion>
  <groupId>${HytaleVersionSource.HYTALE_GROUP}</groupId>
  <artifactId>${HytaleVersionSource.HYTALE_ARTIFACT}</artifactId>
  <version>$version</version>
  <packaging>jar</packaging>
</project>"""

    private fun buildModuleMetadata(version: String): String {
        val group = HytaleVersionSource.HYTALE_GROUP
        val artifact = HytaleVersionSource.HYTALE_ARTIFACT
        return """{
  "formatVersion": "1.1",
  "component": {
    "group": "$group",
    "module": "$artifact",
    "version": "$version"
  },
  "variants": [
    {
      "name": "apiElements",
      "attributes": {
        "org.gradle.category": "library",
        "org.gradle.libraryelements": "jar",
        "org.gradle.usage": "java-api"
      },
      "files": [
        { "name": "$artifact-$version.jar", "url": "$artifact-$version.jar" }
      ]
    },
    {
      "name": "javadocElements",
      "attributes": {
        "org.gradle.category": "documentation",
        "org.gradle.docstype": "javadoc",
        "org.gradle.usage": "java-runtime"
      },
      "files": [
        { "name": "$artifact-$version-javadoc.jar", "url": "$artifact-$version-javadoc.jar" }
      ]
    }
  ]
}"""
    }
}
