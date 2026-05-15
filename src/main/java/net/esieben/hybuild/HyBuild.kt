package net.esieben.hybuild

import net.esieben.hybuild.server.ServerDownloaderTask
import org.gradle.api.Plugin
import org.gradle.api.Project


class HyBuild : Plugin<Project> {

    companion object {
        const val PLUGIN_FOLDER = ".hytale"
    }

    val className: String = this::class.java.simpleName

    override fun apply(project: Project) {
        val logger = project.logger
        logger.lifecycle("Hello from the $className Plugin")

        project.tasks.register("downloadServerDownloader", ServerDownloaderTask::class.java) {
            it.group = "hytale"
            it.description = "Downloads and extracts the Hytale Server Downloader executable"
        }
    }
}