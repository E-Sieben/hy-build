package net.esieben.hybuild.server

import net.esieben.hybuild.HyBuild
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.zip.ZipInputStream

abstract class ExtractServerZipTask : DefaultTask() {

    companion object {
        val VERSION_ZIP_PATTERN = Regex("""\d+\.\d+\.\d+-.+\.zip""")
    }

    @get:Internal
    abstract val hytaleFolder: DirectoryProperty

    init {
        hytaleFolder.convention(
            project.layout.projectDirectory.dir(HyBuild.PLUGIN_FOLDER)
        )
    }

    @TaskAction
    fun extractServerZip() {
        val folder = hytaleFolder.get().asFile

        val versionZip = folder.listFiles()
            ?.firstOrNull { it.isFile && it.name.matches(VERSION_ZIP_PATTERN) }
            ?: error("No server version ZIP found in ${folder.absolutePath}. Run launchServerDownloader first.")

        val serverJar = folder.resolve("HytaleServer.jar")
        val serverAot = folder.resolve("HytaleServer.aot")
        val assetsZip = folder.resolve("Assets.zip")
        if (serverJar.exists() && serverAot.exists() && assetsZip.exists()) {
            logger.lifecycle("All server files already present, skipping extraction.")
            return
        }

        logger.lifecycle("Extracting server files from '${versionZip.name}'")

        ZipInputStream(versionZip.inputStream()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val target: File? = when (File(entry.name).name) {
                        "Assets.zip" -> folder.resolve("Assets.zip")
                        "HytaleServer.jar" -> folder.resolve("HytaleServer.jar")
                        "HytaleServer.aot" -> folder.resolve("HytaleServer.aot")
                        else -> null
                    }
                    if (target != null) {
                        logger.lifecycle("  Extracting '${entry.name}' → '${target.name}'")
                        Files.copy(zip, target.toPath(), StandardCopyOption.REPLACE_EXISTING)
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }

        logger.lifecycle("Server files extracted successfully.")
    }
}
