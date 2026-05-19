package net.esieben.hybuild.server

import net.esieben.hybuild.util.OS
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.util.concurrent.TimeUnit

abstract class LaunchServerDownloaderTask : DefaultTask() {

    @get:InputFile
    abstract val serverDownloaderExecutable: RegularFileProperty

    @TaskAction
    fun launchDownloader() {
        val executable = serverDownloaderExecutable.get().asFile
        if (!OS.isWindows) executable.setExecutable(true)

        val targetVersion = runCatching { runProcess(executable, "-print-version") }
            .getOrNull()?.second?.lastOrNull { it.isNotBlank() }
        if (targetVersion != null && executable.parentFile.resolve("$targetVersion.zip").exists()) {
            logger.lifecycle("Server version '$targetVersion' already downloaded, skipping.")
            return
        }

        logger.lifecycle("Launching Hytale Downloader")
        val (exit) = runProcess(executable)
        check(exit == 0) { "Hytale Downloader exited with non-zero exit code: $exit" }
        logger.lifecycle("Hytale Downloader completed successfully.")
    }

    private fun runProcess(executable: File, vararg args: String): Pair<Int, List<String>> {
        val process = ProcessBuilder(executable.absolutePath, *args)
            .directory(executable.parentFile)
            .redirectErrorStream(true)
            .start()
        val lines = mutableListOf<String>()
        val pump = Thread {
            process.inputStream.bufferedReader().forEachLine { line ->
                logger.lifecycle(line)
                lines += line
            }
        }.also { it.isDaemon = true; it.start() }
        val completed = process.waitFor(20, TimeUnit.MINUTES)
        if (!completed) {
            process.destroyForcibly()
            error("Process '${executable.name}' timed out after 10 minutes")
        }
        pump.join(30_000)
        return process.exitValue() to lines
    }
}