package net.esieben.hybuild

import net.esieben.hybuild.project.AddHytaleFolderToGitignoreTask
import net.esieben.hybuild.server.DownloadServerDownloaderTask
import net.esieben.hybuild.server.ExtractServerZipTask
import net.esieben.hybuild.server.LaunchServerDownloaderTask
import net.esieben.hybuild.server.RunServerTask
import org.gradle.api.Plugin
import org.gradle.api.Project


class HyBuild : Plugin<Project> {

    companion object {
        const val PLUGIN_FOLDER = ".hytale"
    }

    val className: String = this::class.java.simpleName

    override fun apply(project: Project) {
        val logger = project.logger
        logger.lifecycle("$className Plugin is starting")


        /// Server
        val downloadServerDownloaderTask = project.tasks.register(
            "downloadServerDownloader",
            DownloadServerDownloaderTask::class.java
        ) {
            it.group = "hytale server"
            it.description =
                "Downloads and extracts the official Hytale Server Downloader executable"
        }
        val launchServerDownloaderTask = project.tasks.register(
            "launchServerDownloader",
            LaunchServerDownloaderTask::class.java
        ) {
            it.dependsOn(downloadServerDownloaderTask)
            it.serverDownloaderExecutable.set(
                downloadServerDownloaderTask.flatMap { t -> t.serverDownloaderExecutable }
            )
            it.group = "hytale server"
            it.description =
                "Runs the official Hytale Server Downloader, fetching the server version package"
        }
        val extractServerZipTask = project.tasks.register(
            "extractServerZip",
            ExtractServerZipTask::class.java
        ) {
            it.dependsOn(launchServerDownloaderTask)
            it.group = "hytale server"
            it.description =
                "Extracts HytaleServer.jar, HytaleServer.aot and Assets.zip from the downloaded version package"
        }
        project.tasks.register(
            "runServer",
            RunServerTask::class.java
        ) {
            it.dependsOn(extractServerZipTask)
            it.group = "hytale server"
            it.description =
                "Starts the Hytale Server, downloading and extracting server files first if needed"
        }

        /// Project Setups
        project.tasks.register(
            "addHytaleFolderToGitignore",
            AddHytaleFolderToGitignoreTask::class.java
        ) {
            it.group = "hytale project"
            it.description = "Adds the .hytale folder to your gitignore file"
        }
    }
}