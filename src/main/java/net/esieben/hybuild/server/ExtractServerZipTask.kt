package net.esieben.hybuild.server

import net.esieben.hybuild.HyBuild
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.zip.ZipInputStream

abstract class ExtractServerZipTask : DefaultTask() {

    companion object {
        val VERSION_ZIP_PATTERN = Regex("""\d+\.\d+\.\d+-.+\.zip""")
    }

    @get:InputDirectory
    @get:Optional
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
            ?.filter { it.isFile && it.name.matches(VERSION_ZIP_PATTERN) }
            ?.maxByOrNull { it.name }
            ?: error("No server version ZIP found in ${folder.absolutePath}. Run launchServerDownloader first.")

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
