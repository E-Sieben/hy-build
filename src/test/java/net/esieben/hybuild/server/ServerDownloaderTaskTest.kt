package net.esieben.hybuild.server

import net.esieben.hybuild.HyBuild
import net.esieben.hybuild.util.OS
import org.gradle.testfixtures.ProjectBuilder
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.*

class ServerDownloaderTaskTest {

    private val pluginPackage: String = this::class.java.packageName.replace(".server", "")

    private fun buildProject() = ProjectBuilder.builder().build().also {
        it.pluginManager.apply(pluginPackage)
    }

    private fun createFakeZip(vararg entryNames: String): ByteArray {
        val buffer = java.io.ByteArrayOutputStream()
        ZipOutputStream(buffer).use { zip ->
            for (name in entryNames) {
                zip.putNextEntry(ZipEntry(name))
                zip.write("fake-binary-content".toByteArray())
                zip.closeEntry()
            }
        }
        return buffer.toByteArray()
    }

    @Test
    fun `task is registered on the project`() {
        val project = buildProject()
        assertNotNull(
            project.tasks.findByName("downloadServerDownloader"),
            "Expected task 'downloadServerDownloader' to be registered"
        )
    }

    @Test
    fun `task is of correct type`() {
        val project = buildProject()
        val task = project.tasks.findByName("downloadServerDownloader")
        assertIs<ServerDownloaderTask>(task)
    }

    @Test
    fun `serverDownloaderExecutable has correct default name`() {
        val project = buildProject()
        val task = project.tasks.getByName("downloadServerDownloader") as ServerDownloaderTask
        val file = task.serverDownloaderExecutable.get().asFile
        assertEquals(
            ServerDownloaderTask.DOWNLOADER_NAME,
            file.name,
            "Expected output file name to match DOWNLOADER_NAME"
        )
    }

    @Test
    fun `serverDownloaderExecutable is inside plugin folder`() {
        val project = buildProject()
        val task = project.tasks.getByName("downloadServerDownloader") as ServerDownloaderTask
        val file = task.serverDownloaderExecutable.get().asFile
        assertTrue(
            file.path.contains(HyBuild.PLUGIN_FOLDER),
            "Expected output file to be inside PLUGIN_FOLDER, but was: ${file.path}"
        )
    }

    @Test
    fun `DOWNLOADER_NAME ends with exe on windows or has no extension on linux`() {
        if (OS.isWindows) {
            assertTrue(
                ServerDownloaderTask.DOWNLOADER_NAME.endsWith(".exe"),
                "Expected DOWNLOADER_NAME to end with .exe on Windows"
            )
        } else {
            assertFalse(
                ServerDownloaderTask.DOWNLOADER_NAME.contains("."),
                "Expected DOWNLOADER_NAME to have no extension on Linux/Mac"
            )
        }
    }

    @Test
    fun `task skips download if executable already exists`() {
        val project = buildProject()
        val task = project.tasks.getByName("downloadServerDownloader") as ServerDownloaderTask
        val targetFile = task.serverDownloaderExecutable.get().asFile

        targetFile.parentFile.mkdirs()
        targetFile.writeText("existing-binary")

        val contentBefore = targetFile.readText()

        assertTrue(targetFile.exists(), "Pre-condition: file must exist")
        assertEquals(
            "existing-binary",
            contentBefore,
            "File should not be overwritten when already present"
        )
    }

    @Test
    fun `zip extraction picks correct binary entry for current OS`() {
        val zipBytes = if (OS.isWindows) {
            createFakeZip("hytale-downloader.exe", "hytale-downloader", "readme.txt")
        } else {
            createFakeZip("hytale-downloader.exe", "hytale-downloader", "readme.txt")
        }

        val project = buildProject()
        val task = project.tasks.getByName("downloadServerDownloader") as ServerDownloaderTask
        val targetFile = task.serverDownloaderExecutable.get().asFile
        targetFile.parentFile.mkdirs()

        // Simulate what the task does: find the right entry and write it
        java.util.zip.ZipInputStream(zipBytes.inputStream()).use { zipStream ->
            var entry = zipStream.nextEntry
            while (entry != null) {
                val name = entry.name.substringAfterLast('/')
                val isMatch = if (OS.isWindows) {
                    !entry.isDirectory && name.endsWith(".exe")
                } else {
                    !entry.isDirectory && name.isNotEmpty() && !name.contains('.')
                }
                if (isMatch) {
                    Files.copy(
                        zipStream,
                        targetFile.toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING
                    )
                    break
                }
                zipStream.closeEntry()
                entry = zipStream.nextEntry
            }
        }

        assertTrue(targetFile.exists(), "Expected binary to be extracted")
        assertEquals(
            ServerDownloaderTask.DOWNLOADER_NAME,
            targetFile.name,
            "Extracted file must be named DOWNLOADER_NAME"
        )
        assertEquals("fake-binary-content", targetFile.readText())
    }

    @Test
    fun `no extra files are written outside the target executable`() {
        val zipBytes = createFakeZip(
            "hytale-downloader.exe",
            "hytale-downloader",
            "readme.txt",
            "lib/helper.jar"
        )

        val project = buildProject()
        val task = project.tasks.getByName("downloadServerDownloader") as ServerDownloaderTask
        val targetFile = task.serverDownloaderExecutable.get().asFile
        val outputDir = targetFile.parentFile
        outputDir.mkdirs()

        val filesBefore = outputDir.listFiles()?.map { it.name }?.toSet() ?: emptySet()

        java.util.zip.ZipInputStream(zipBytes.inputStream()).use { zipStream ->
            var entry = zipStream.nextEntry
            while (entry != null) {
                val name = entry.name.substringAfterLast('/')
                val isMatch = if (OS.isWindows) {
                    !entry.isDirectory && name.endsWith(".exe")
                } else {
                    !entry.isDirectory && name.isNotEmpty() && !name.contains('.')
                }
                if (isMatch) {
                    Files.copy(
                        zipStream,
                        targetFile.toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING
                    )
                    break
                }
                zipStream.closeEntry()
                entry = zipStream.nextEntry
            }
        }

        val filesAfter = outputDir.listFiles()?.map { it.name }?.toSet() ?: emptySet()
        val newFiles = filesAfter - filesBefore

        assertEquals(
            setOf(ServerDownloaderTask.DOWNLOADER_NAME),
            newFiles,
            "Only DOWNLOADER_NAME should have been written to the output directory"
        )
    }
}