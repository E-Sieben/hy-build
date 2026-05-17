package net.esieben.hybuild.server

import net.esieben.hybuild.util.OS
import org.gradle.api.DefaultTask
import java.io.File
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction

abstract class LaunchServerDownloaderTask : DefaultTask() {

    @get:InputFile
    abstract val serverDownloaderExecutable: RegularFileProperty

    @TaskAction
    fun launchDownloader() {
        val executable = serverDownloaderExecutable.get().asFile

        check(executable.exists()) {
            "Hytale Downloader binary not found at ${executable.absolutePath}."
        }

        if (!OS.isWindows) {
            executable.setExecutable(true)
        }

        val targetVersion = printVersion(executable)
        if (targetVersion != null) {
            val versionZip = executable.parentFile.resolve("$targetVersion.zip")
            if (versionZip.exists()) {
                logger.lifecycle("Server version '$targetVersion' already downloaded, skipping.")
                return
            }
        }

        logger.lifecycle("Launching Hytale Downloader")

        val processBuilder = ProcessBuilder(executable.absolutePath)
            .directory(executable.parentFile)
            .redirectErrorStream(true)

        val process = processBuilder.start()

        val outputPump = Thread {
            process.inputStream.bufferedReader().forEachLine { line ->
                logger.lifecycle(line)
            }
        }.also { it.isDaemon = true; it.start() }

        val exit = process.waitFor()
        outputPump.join()

        check(exit == 0) {
            "Hytale Downloader exited with non-zero exit code: $exit"
        }

        logger.lifecycle("Hytale Downloader completed successfully.")
    }

    private fun printVersion(executable: File): String? = runCatching {
        val process = ProcessBuilder(executable.absolutePath, "-print-version")
            .directory(executable.parentFile)
            .start()
        val version = process.inputStream.bufferedReader().readText().trim()
        process.waitFor()
        version.ifBlank { null }
    }.getOrNull()
}