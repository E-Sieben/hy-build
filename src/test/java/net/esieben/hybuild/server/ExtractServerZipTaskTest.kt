package net.esieben.hybuild.server

import net.esieben.hybuild.HyBuild
import org.gradle.testfixtures.ProjectBuilder
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.*

class ExtractServerZipTaskTest {

    private val pluginPackage = this::class.java.packageName.replace(".server", "")

    private fun buildProject() = ProjectBuilder.builder().build().also {
        it.pluginManager.apply(pluginPackage)
    }

    private fun getTask(project: org.gradle.api.Project) =
        project.tasks.getByName("extractServerZip") as ExtractServerZipTask

    private fun tempDir() = Files.createTempDirectory("hy-build-test").toFile()

    private fun writeVersionZip(dir: File, vararg entries: Pair<String, String>): File {
        val zip = dir.resolve("2026.03.26-89796e57b.zip")
        ZipOutputStream(zip.outputStream()).use { out ->
            for ((name, content) in entries) {
                out.putNextEntry(ZipEntry(name))
                out.write(content.toByteArray())
                out.closeEntry()
            }
        }
        return zip
    }

    // ── Registration ─────────────────────────────────────────────────────────

    @Test
    fun `task is registered on the project`() {
        val project = buildProject()
        assertNotNull(project.tasks.findByName("extractServerZip"))
    }

    @Test
    fun `task is of correct type`() {
        val project = buildProject()
        assertIs<ExtractServerZipTask>(project.tasks.findByName("extractServerZip"))
    }

    @Test
    fun `default hytaleFolder is inside PLUGIN_FOLDER`() {
        val project = buildProject()
        val task = getTask(project)
        assertTrue(task.hytaleFolder.get().asFile.path.contains(HyBuild.PLUGIN_FOLDER))
    }

    // ── Version zip detection ─────────────────────────────────────────────────

    @Test
    fun `VERSION_ZIP_PATTERN matches date-hash zip names`() {
        listOf(
            "2026.03.26-89796e57b.zip",
            "1.2.3-abc123.zip",
            "0.0.1-build.zip",
        ).forEach { name ->
            assertTrue(name.matches(ExtractServerZipTask.VERSION_ZIP_PATTERN), "'$name' should match")
        }
    }

    @Test
    fun `VERSION_ZIP_PATTERN rejects non-version zip names`() {
        listOf(
            "Asset.zip",
            "hytale-downloader.zip",
            "1.2.3.zip",
            "random.zip",
        ).forEach { name ->
            assertFalse(name.matches(ExtractServerZipTask.VERSION_ZIP_PATTERN), "'$name' should not match")
        }
    }

    // ── Extraction ────────────────────────────────────────────────────────────

    @Test
    fun `extractServerZip extracts all three server files`() {
        val dir = tempDir()
        writeVersionZip(dir,
            "Asset.zip" to "asset-data",
            "Server/HytaleServer.jar" to "jar-data",
            "Server/HytaleServer.aot" to "aot-data",
        )

        val task = getTask(buildProject())
        task.hytaleFolder.set(dir)
        task.extractServerZip()

        assertEquals("asset-data", dir.resolve("Asset.zip").readText())
        assertEquals("jar-data",   dir.resolve("HytaleServer.jar").readText())
        assertEquals("aot-data",   dir.resolve("HytaleServer.aot").readText())
    }

    @Test
    fun `extractServerZip strips subdirectory prefix when naming extracted files`() {
        val dir = tempDir()
        writeVersionZip(dir,
            "Server/HytaleServer.jar" to "jar-data",
            "Server/HytaleServer.aot" to "aot-data",
            "Asset.zip" to "asset-data",
        )

        val task = getTask(buildProject())
        task.hytaleFolder.set(dir)
        task.extractServerZip()

        assertTrue(dir.resolve("HytaleServer.jar").exists(), "HytaleServer.jar should be extracted flat")
        assertFalse(dir.resolve("Server").exists(), "Server/ subdirectory should not be created")
    }

    @Test
    fun `extractServerZip skips unrecognised zip entries`() {
        val dir = tempDir()
        writeVersionZip(dir,
            "Asset.zip" to "asset-data",
            "Unknown/readme.txt" to "readme",
        )

        val task = getTask(buildProject())
        task.hytaleFolder.set(dir)
        task.extractServerZip()

        assertFalse(dir.resolve("readme.txt").exists())
        assertFalse(dir.resolve("Unknown").exists())
    }

    // ── Fragment cleanup ──────────────────────────────────────────────────────

    @Test
    fun `extractServerZip keeps version zip and credentials json`() {
        val dir = tempDir()
        val versionZip = writeVersionZip(dir, "Asset.zip" to "asset-data")
        val credentials = dir.resolve("hytale-downloader-credentials.json")
        credentials.writeText("""{"token":"abc"}""")

        val task = getTask(buildProject())
        task.hytaleFolder.set(dir)
        task.extractServerZip()

        assertTrue(versionZip.exists(),   "Version zip must not be deleted")
        assertTrue(credentials.exists(),  "Credentials JSON must not be deleted")
    }

    // ── Error handling ────────────────────────────────────────────────────────

    @Test
    fun `extractServerZip throws when no version zip is present`() {
        val dir = tempDir()
        val task = getTask(buildProject())
        task.hytaleFolder.set(dir)

        assertFailsWith<IllegalStateException> { task.extractServerZip() }
    }
}
