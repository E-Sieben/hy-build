package net.esieben.hybuild.server

import net.esieben.hybuild.HyBuild
import net.esieben.hybuild.util.OS
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class RunServerTask : DefaultTask() {

    @get:Internal
    abstract val hytaleFolder: DirectoryProperty

    @get:InputFile
    @get:Optional
    abstract val pluginJar: RegularFileProperty

    @get:Input
    @get:Optional
    abstract val pluginBaseName: Property<String>

    init {
        hytaleFolder.convention(
            project.layout.projectDirectory.dir(HyBuild.PLUGIN_FOLDER)
        )
    }

    @TaskAction
    fun runServer() {
        val folder = hytaleFolder.get().asFile
        val serverJar = folder.resolve("HytaleServer.jar")
        val serverAot = folder.resolve("HytaleServer.aot")
        val assetsZip = folder.resolve("Assets.zip")

        val missing = listOf(serverJar, serverAot, assetsZip).filter { !it.exists() }
        check(missing.isEmpty()) {
            "Missing server files: ${missing.joinToString { it.name }}. Run extractServerZip first."
        }

        val serverDir = folder.resolve("server")
        serverDir.mkdirs()

        val modsDir = serverDir.resolve("mods")
        modsDir.mkdirs()
        pluginJar.orNull?.asFile?.let { jar ->
            val baseName = pluginBaseName.orNull
            if (baseName != null) {
                modsDir.listFiles { f -> f.extension == "jar" && f.nameWithoutExtension.startsWith("$baseName-") }
                    ?.forEach { it.delete() }
            }
            logger.lifecycle("Deploying '${jar.name}' → mods/")
            jar.copyTo(modsDir.resolve(jar.name), overwrite = true)
        }

        val command = listOf(
            javaExecutable,
            "-jar",
            serverJar.absolutePath,
            "--assets",
            assetsZip.absolutePath
        )

        if (launchInTerminal(serverDir, command)) {
            logger.lifecycle("Hytale Server launched in a new terminal window.")
        } else {
            logger.lifecycle("No graphical terminal found — running inline.")
            runInline(serverDir, command)
        }
    }

    // ── Terminal launchers ────────────────────────────────────────────────────

    private fun launchInTerminal(workingDir: File, command: List<String>): Boolean = when {
        OS.isWindows -> launchWindows(workingDir, command)
        OS.isMac -> launchMac(workingDir, command)
        OS.hasDisplay -> launchLinux(workingDir, command)
        else -> false
    }

    private fun launchWindows(workingDir: File, command: List<String>): Boolean {
        val script = workingDir.resolve("start-server.bat")
        script.writeText(
            "@echo off\r\n" +
                    command.joinToString(" ") { "\"$it\"" } + "\r\n" +
                    "echo.\r\npause\r\n"
        )
        return runCatching {
            ProcessBuilder("cmd", "/c", "start", "cmd", "/k", script.absolutePath)
                .directory(workingDir)
                .start()
        }.isSuccess
    }

    private fun launchMac(workingDir: File, command: List<String>): Boolean {
        val script = writeUnixScript(workingDir, command, waitOnExit = false)
        return runCatching {
            ProcessBuilder("open", "-a", "Terminal", script.absolutePath)
                .start()
        }.isSuccess
    }

    private fun launchLinux(workingDir: File, command: List<String>): Boolean {
        val script = writeUnixScript(workingDir, command, waitOnExit = true)

        // Try terminals in order of preference; stop at the first one found
        val terminals = listOf(
            listOf("x-terminal-emulator", "-e", script.absolutePath),
            listOf("gnome-terminal", "--", script.absolutePath),
            listOf("konsole", "-e", script.absolutePath),
            listOf("xfce4-terminal", "--", script.absolutePath),
            listOf("xterm", "-e", script.absolutePath),
        )
        return terminals.any { args ->
            runCatching {
                val found = ProcessBuilder("which", args[0])
                    .redirectErrorStream(true).start().waitFor() == 0
                if (!found) return@runCatching false
                ProcessBuilder(args).directory(workingDir).start()
                true
            }.getOrDefault(false)
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun writeUnixScript(
        workingDir: File,
        command: List<String>,
        waitOnExit: Boolean
    ): File {
        val script = workingDir.resolve("start-server.sh")
        val cmdLine = command.joinToString(" ") { unixQuote(it) }
        script.writeText(buildString {
            appendLine("#!/usr/bin/env bash")
            appendLine(cmdLine)
            if (waitOnExit) appendLine("read -p 'Server stopped. Press Enter to close...'")
        })
        script.setExecutable(true)
        return script
    }

    /** Inline fallback for headless environments (CI, SSH). */
    private fun runInline(workingDir: File, command: List<String>) {
        val process = ProcessBuilder(command).directory(workingDir).start()

        val stdoutPump = Thread { process.inputStream.copyTo(System.out) }
            .also { it.isDaemon = true; it.start() }
        val stderrPump = Thread { process.errorStream.copyTo(System.err) }
            .also { it.isDaemon = true; it.start() }
        Thread {
            runCatching {
                System.`in`.bufferedReader().lineSequence().forEach { line ->
                    process.outputStream.write((line + "\n").toByteArray())
                    process.outputStream.flush()
                }
            }
        }.also { it.isDaemon = true; it.start() }

        val exit = process.waitFor()
        stdoutPump.join(5_000)
        stderrPump.join(5_000)
        logger.lifecycle("Hytale Server exited with code $exit.")
    }

    private val javaExecutable: String
        get() {
            val exe = if (OS.isWindows) "java.exe" else "java"
            return File(System.getProperty("java.home"), "bin/$exe").absolutePath
        }

    private fun unixQuote(s: String) = "'${s.replace("'", "'\\''")}'"
}
