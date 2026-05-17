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
        const val LOMBOK_VERSION = "1.18.36"
    }

    val className: String = this::class.java.simpleName

    override fun apply(project: Project) {
        val logger = project.logger
        logger.lifecycle("$className Plugin is starting")

        /// Server
        registerServerTasks(project)

        /// Project Setups
        registerProjectTasks(project)

        /// Dependencies
        project.plugins.withId("java") {
            project.dependencies.add("compileOnly", project.files("$PLUGIN_FOLDER/HytaleServer.jar"))
            project.dependencies.add("compileOnly", "org.projectlombok:lombok:$LOMBOK_VERSION")
            project.dependencies.add("annotationProcessor", "org.projectlombok:lombok:$LOMBOK_VERSION")
        }
    }

    private fun registerProjectTasks(project: Project) {
        project.tasks.register(
            "addHytaleFolderToGitignore",
            AddHytaleFolderToGitignoreTask::class.java
        ) {
            it.group = "hytale project"
            it.description = "Adds the .hytale folder to your gitignore file"
        }
    }

    private fun registerServerTasks(project: Project) {
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
    }
}