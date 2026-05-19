package net.esieben.hybuild.server

import net.esieben.hybuild.HyBuild
import net.esieben.hybuild.util.OS
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.net.URI
import java.net.URL
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.zip.ZipInputStream

abstract class DownloadServerDownloaderTask : DefaultTask() {
    companion object {
        val DOWNLOADER_URL: URL =
            URI.create("https://downloader.hytale.com/hytale-downloader.zip").toURL()
        val DOWNLOADER_NAME: String =
            if (OS.isWindows) "hytale-downloader.exe" else "hytale-downloader"
    }

    @get:OutputFile
    abstract val serverDownloaderExecutable: RegularFileProperty

    init {
        serverDownloaderExecutable.convention(
            project.layout.projectDirectory.file("${HyBuild.PLUGIN_FOLDER}/$DOWNLOADER_NAME")
        )
    }

    @TaskAction
    fun downloadServerDownloader() {
        val targetExecutable = serverDownloaderExecutable.get().asFile
        val outputDir = targetExecutable.parentFile
        outputDir.mkdirs()

        logger.lifecycle("Downloading and extracting Hytale Server Downloader...")

        DOWNLOADER_URL.openStream().use { webStream ->
            ZipInputStream(webStream).use { zipStream ->
                var entry = zipStream.nextEntry
                while (entry != null) {
                    val isMatch = if (OS.isWindows) {
                        !entry.isDirectory && entry.name.endsWith(".exe")
                    } else {
                        !entry.isDirectory
                                && !entry.name.contains("/")
                                && !entry.name.contains(".")
                    }

                    if (isMatch) {
                        logger.lifecycle("Extracting '${entry.name}' → '${targetExecutable.name}'")
                        Files.copy(
                            zipStream,
                            targetExecutable.toPath(),
                            StandardCopyOption.REPLACE_EXISTING
                        )
                        zipStream.closeEntry()
                        break
                    }

                    zipStream.closeEntry()
                    entry = zipStream.nextEntry
                }
            }
        }

        check(targetExecutable.exists()) {
            "Extraction failed: no matching binary found in ZIP from $DOWNLOADER_URL"
        }

        if (!OS.isWindows) {
            targetExecutable.setExecutable(true)
            logger.lifecycle("Set executable permission on ${targetExecutable.absolutePath}")
        }
    }
}