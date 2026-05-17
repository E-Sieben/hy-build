package net.esieben.hybuild.project

import groovy.json.JsonSlurper
import net.esieben.hybuild.HyBuild
import org.gradle.testfixtures.ProjectBuilder
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class CreateManifestTaskTest {

    private fun setup(
        projectName: String = "my-plugin",
        group: String = "com.example",
        version: String = "1.0.0",
        versionZipName: String = "2024.01.01-abc"
    ): Pair<CreateManifestTask, File> {
        val projectDir = Files.createTempDirectory("hy-build-test").toFile()
        File(projectDir, HyBuild.PLUGIN_FOLDER).mkdirs()
        File(projectDir, "${HyBuild.PLUGIN_FOLDER}/$versionZipName.zip").createNewFile()

        val manifestFile = File(projectDir, "manifest.json")
        val project = ProjectBuilder.builder().withName(projectName).withProjectDir(projectDir).build()
        project.group = group
        project.version = version

        val task = project.tasks.register("createManifest", CreateManifestTask::class.java) {
            it.authors.set(listOf("Alice"))
            it.manifestFile.set(manifestFile)
        }.get()

        return task to manifestFile
    }

    @Suppress("UNCHECKED_CAST")
    private fun File.parseManifest() = JsonSlurper().parse(this) as Map<String, Any?>

    @Test
    fun `manifest contains all fields derived from project properties`() {
        // Arrange
        val (task, manifestFile) = setup(projectName = "my-plugin", group = "com.example", version = "1.2.3")

        // Act
        task.createManifest()

        // Assert
        val manifest = manifestFile.parseManifest()
        assertEquals("com.example", manifest["Group"])
        assertEquals("my-plugin", manifest["Name"])
        assertEquals("1.2.3", manifest["Version"])
    }

    @Test
    fun `main class is PascalCase project name prefixed with group`() {
        // Arrange
        val (task, manifestFile) = setup(projectName = "my-plugin", group = "com.example")

        // Act
        task.createManifest()

        // Assert
        assertEquals("com.example.MyPlugin", manifestFile.parseManifest()["Main"])
    }

    @Test
    fun `server version is derived from the version zip filename`() {
        // Arrange
        val (task, manifestFile) = setup(versionZipName = "2024.06.15-deadbeef")

        // Act
        task.createManifest()

        // Assert
        assertEquals("2024.06.15-deadbeef", manifestFile.parseManifest()["ServerVersion"])
    }

    @Test
    fun `optional fields are present when configured`() {
        // Arrange
        val projectDir = Files.createTempDirectory("hy-build-test").toFile()
        File(projectDir, HyBuild.PLUGIN_FOLDER).mkdirs()
        File(projectDir, "${HyBuild.PLUGIN_FOLDER}/2024.01.01-abc.zip").createNewFile()
        val manifestFile = File(projectDir, "manifest.json")
        val project = ProjectBuilder.builder().withProjectDir(projectDir).build()
        project.group = "com.example"
        project.version = "1.0.0"

        val task = project.tasks.register("createManifest", CreateManifestTask::class.java) {
            it.authors.set(listOf("Alice"))
            it.pluginDescription.set("A test plugin")
            it.website.set("https://example.com")
            it.manifestFile.set(manifestFile)
        }.get()

        // Act
        task.createManifest()

        // Assert
        val manifest = manifestFile.parseManifest()
        assertEquals("A test plugin", manifest["Description"])
        assertEquals("https://example.com", manifest["Website"])
    }

    @Test
    fun `optional fields are absent from manifest when not configured`() {
        // Arrange
        val (task, manifestFile) = setup()

        // Act
        task.createManifest()

        // Assert
        val manifest = manifestFile.parseManifest()
        assertNull(manifest["Description"])
        assertNull(manifest["Website"])
    }

    @Test
    fun `fails with clear error when no server version zip is present`() {
        // Arrange
        val projectDir = Files.createTempDirectory("hy-build-test").toFile()
        File(projectDir, HyBuild.PLUGIN_FOLDER).mkdirs() // empty — no zip inside
        val project = ProjectBuilder.builder().withProjectDir(projectDir).build()
        project.group = "com.example"
        project.version = "1.0.0"

        val task = project.tasks.register("createManifest", CreateManifestTask::class.java) {
            it.authors.set(listOf("Alice"))
            it.manifestFile.set(File(projectDir, "manifest.json"))
        }.get()

        // Act + Assert
        assertFailsWith<IllegalStateException> { task.createManifest() }
    }
}
