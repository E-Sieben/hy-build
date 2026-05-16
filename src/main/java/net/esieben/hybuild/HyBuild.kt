package net.esieben.hybuild

import net.esieben.hybuild.project.AddHytaleFolderToGitignoreTask
import net.esieben.hybuild.server.DownloadServerDownloaderTask
import net.esieben.hybuild.server.LaunchServerDownloaderTask
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
                "Runs the official Hytale Server downloader, downloading Asset.zip and the server.jar"
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