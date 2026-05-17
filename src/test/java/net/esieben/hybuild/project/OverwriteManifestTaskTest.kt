package net.esieben.hybuild.project

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import net.esieben.hybuild.HyBuild
import org.gradle.testfixtures.ProjectBuilder
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class OverwriteManifestTaskTest {

    private fun setup(): Triple<OverwriteManifestTask, File, File> {
        val projectDir = Files.createTempDirectory("hy-build-test").toFile()
        val hytaleDir = File(projectDir, HyBuild.PLUGIN_FOLDER).also { it.mkdirs() }
        File(hytaleDir, "2024.01.01-abc.zip").createNewFile()

        val manifestFile = File(projectDir, "manifest.json")
        val project = ProjectBuilder.builder().withName("my-plugin").withProjectDir(projectDir).build()
        project.group = "com.example"
        project.version = "1.0.0"

        val task = project.tasks.register("overwriteManifest", OverwriteManifestTask::class.java) {
            it.authors.set(listOf("Alice"))
            it.manifestFile.set(manifestFile)
        }.get()

        return Triple(task, manifestFile, projectDir)
    }

    private fun baseManifest(version: String = "1.0.0") = mapOf(
        "Group" to "com.example",
        "Name" to "my-plugin",
        "Version" to version,
        "Authors" to listOf(mapOf("Name" to "Alice")),
        "Main" to "com.example.MyPlugin",
        "ServerVersion" to "2024.01.01-abc"
    )

    @Suppress("UNCHECKED_CAST")
    private fun File.parseManifest() = JsonSlurper().parse(this) as Map<String, Any?>

    @Test
    fun `creates manifest from scratch when file does not exist`() {
        // Arrange
        val (task, manifestFile, _) = setup()

        // Act
        task.overwriteManifest()

        // Assert
        val manifest = manifestFile.parseManifest()
        assertEquals("com.example", manifest["Group"])
        assertEquals("my-plugin", manifest["Name"])
    }

    @Test
    fun `updates only the field that changed`() {
        // Arrange
        val (task, manifestFile, _) = setup()
        manifestFile.writeText(JsonOutput.toJson(baseManifest(version = "0.9.0")))

        // Act
        task.overwriteManifest() // project.version = "1.0.0", manifest has "0.9.0"

        // Assert
        val manifest = manifestFile.parseManifest()
        assertEquals("1.0.0", manifest["Version"])
        assertEquals("com.example", manifest["Group"]) // unchanged
        assertEquals("my-plugin", manifest["Name"])    // unchanged
    }

    @Test
    fun `preserves fields not managed by the task`() {
        // Arrange
        val (task, manifestFile, _) = setup()
        manifestFile.writeText(
            JsonOutput.toJson(
                baseManifest() + mapOf("Dependencies" to mapOf("com.other:lib" to ">=1.0.0"))
            )
        )

        // Act
        task.overwriteManifest()

        // Assert
        assertNotNull(manifestFile.parseManifest()["Dependencies"])
    }

    @Test
    fun `preserves manually added fields when a managed field changes`() {
        // Arrange
        val (task, manifestFile, _) = setup()
        manifestFile.writeText(
            JsonOutput.toJson(
                baseManifest(version = "0.9.0") + mapOf("IncludesAssetPack" to true)
            )
        )

        // Act
        task.overwriteManifest() // version bumps from 0.9.0 to 1.0.0, forcing a rewrite

        // Assert — IncludesAssetPack must survive even when the file is rewritten
        val manifest = manifestFile.parseManifest()
        assertEquals("1.0.0", manifest["Version"])
        assertEquals(true, manifest["IncludesAssetPack"])
    }

    @Test
    fun `leaves file untouched when all values are already current`() {
        // Arrange
        val (task, manifestFile, _) = setup()
        val originalContent = JsonOutput.toJson(baseManifest())
        manifestFile.writeText(originalContent)

        // Act
        task.overwriteManifest()

        // Assert — task returns early without rewriting, so compact JSON is preserved
        assertEquals(originalContent, manifestFile.readText())
    }
}
