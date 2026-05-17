package net.esieben.hybuild.server

import net.esieben.hybuild.HyBuild
import org.gradle.testfixtures.ProjectBuilder
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExtractServerZipTaskTest {

    private fun setup(): Pair<ExtractServerZipTask, File> {
        val projectDir = Files.createTempDirectory("hy-build-test").toFile()
        val hytaleDir = File(projectDir, HyBuild.PLUGIN_FOLDER).also { it.mkdirs() }
        val project = ProjectBuilder.builder().withProjectDir(projectDir).build()
        val task = project.tasks.register("extractServerZip", ExtractServerZipTask::class.java).get()
        return task to hytaleDir
    }

    private fun createVersionZip(dir: File, name: String = "2024.01.01-abc"): File {
        val zip = File(dir, "$name.zip")
        ZipOutputStream(zip.outputStream()).use { zos ->
            // Simulate the real server zip structure where JAR and AOT are inside a subdirectory
            for ((entry, content) in listOf(
                "Server/HytaleServer.jar" to "fake-jar",
                "Server/HytaleServer.aot" to "fake-aot",
                "Assets.zip" to "fake-assets"
            )) {
                zos.putNextEntry(ZipEntry(entry))
                zos.write(content.toByteArray())
                zos.closeEntry()
            }
        }
        return zip
    }

    @Test
    fun `extracts all three server files from version zip`() {
        // Arrange
        val (task, hytaleDir) = setup()
        createVersionZip(hytaleDir)

        // Act
        task.extractServerZip()

        // Assert
        assertTrue(File(hytaleDir, "HytaleServer.jar").exists())
        assertTrue(File(hytaleDir, "HytaleServer.aot").exists())
        assertTrue(File(hytaleDir, "Assets.zip").exists())
    }

    @Test
    fun `extracts files correctly when they are in subdirectories inside the zip`() {
        // Arrange
        val (task, hytaleDir) = setup()
        createVersionZip(hytaleDir) // JAR and AOT are under Server/ subdirectory

        // Act
        task.extractServerZip()

        // Assert — files land flat in the hytale folder, not inside a subdirectory
        assertFalse(File(hytaleDir, "Server/HytaleServer.jar").exists())
        assertTrue(File(hytaleDir, "HytaleServer.jar").exists())
    }

    @Test
    fun `overwrites existing files so an updated version zip is never ignored`() {
        // Arrange
        val (task, hytaleDir) = setup()
        File(hytaleDir, "HytaleServer.jar").writeText("old-version")
        createVersionZip(hytaleDir) // zip contains "fake-jar" as new content

        // Act
        task.extractServerZip()

        // Assert — old content is replaced by whatever is in the current zip
        assertEquals("fake-jar", File(hytaleDir, "HytaleServer.jar").readText())
    }

    @Test
    fun `fails with clear error when no version zip is present`() {
        // Arrange
        val (task, _) = setup() // hytale dir exists but is empty

        // Act + Assert
        assertFailsWith<IllegalStateException> { task.extractServerZip() }
    }
}
