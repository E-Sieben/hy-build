package net.esieben.hybuild

import net.esieben.hybuild.project.AddHytaleFolderToGitignoreTask
import net.esieben.hybuild.project.CreateManifestTask
import net.esieben.hybuild.project.OverwriteManifestTask
import net.esieben.hybuild.project.ValidateManifestTask
import net.esieben.hybuild.server.DownloadServerDownloaderTask
import net.esieben.hybuild.server.ExtractServerZipTask
import net.esieben.hybuild.server.LaunchServerDownloaderTask
import net.esieben.hybuild.server.RunServerTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.bundling.Jar

class HyBuild : Plugin<Project> {

    companion object {
        const val PLUGIN_FOLDER = ".hytale"
    }

    override fun apply(project: Project) {
        project.logger.lifecycle("${this::class.java.simpleName} Plugin is starting")

        registerServerTasks(project)
        registerProjectTasks(project)

        project.plugins.withId("java") {
            project.plugins.apply("io.freefair.lombok")
            project.dependencies.add(
                "compileOnly",
                project.files("$PLUGIN_FOLDER/HytaleServer.jar")
            )
        }
    }

    private fun registerProjectTasks(project: Project) {
        val extension = project.extensions.create("hytale", HyBuildExtension::class.java)

        project.tasks.register(
            "addHytaleFolderToGitignore",
            AddHytaleFolderToGitignoreTask::class.java
        ) {
            it.group = "hytale project"
            it.description = "Adds the .hytale folder to your gitignore file"
        }

        val createManifestTask = project.tasks.register(
            "createManifest",
            CreateManifestTask::class.java
        ) {
            it.authors.convention(extension.authors)
            it.pluginDescription.convention(extension.description)
            it.website.convention(extension.website)
            it.group = "hytale project"
            it.description = "Generates manifest.json for the Hytale plugin"
        }

        val validateManifestTask = project.tasks.register(
            "validateManifest",
            ValidateManifestTask::class.java
        ) {
            it.dependsOn(createManifestTask)
            it.manifestFile.set(createManifestTask.flatMap { t -> t.manifestFile })
            it.group = "hytale project"
            it.description = "Validates the plugin manifest.json against the Hytale manifest schema"
        }

        createManifestTask.configure { it.finalizedBy(validateManifestTask) }

        project.tasks.register(
            "overwriteManifest",
            OverwriteManifestTask::class.java
        ) {
            it.authors.convention(extension.authors)
            it.pluginDescription.convention(extension.description)
            it.website.convention(extension.website)
            it.manifestFile.convention(createManifestTask.flatMap { t -> t.manifestFile })
            it.finalizedBy(validateManifestTask)
            it.group = "hytale project"
            it.description = "Updates only changed fields in the existing manifest.json"
        }

        project.plugins.withId("java") {
            project.tasks.named("processResources") { it.dependsOn(createManifestTask) }
            project.tasks.named("check") { it.dependsOn(validateManifestTask) }
        }
    }

    private fun registerServerTasks(project: Project) {
        val downloadServerDownloaderTask = project.tasks.register(
            "downloadServerDownloader",
            DownloadServerDownloaderTask::class.java
        ) {
            it.group = "hytale server"
            it.description = "Downloads and extracts the official Hytale Server Downloader executable"
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
            it.description = "Runs the official Hytale Server Downloader, fetching the server version package"
        }

        val extractServerZipTask = project.tasks.register(
            "extractServerZip",
            ExtractServerZipTask::class.java
        ) {
            it.dependsOn(launchServerDownloaderTask)
            it.group = "hytale server"
            it.description = "Extracts HytaleServer.jar, HytaleServer.aot and Assets.zip from the downloaded version package"
        }

        val runServerTask = project.tasks.register(
            "runServer",
            RunServerTask::class.java
        ) {
            it.dependsOn(extractServerZipTask)
            it.group = "hytale server"
            it.description = "Starts the Hytale Server, downloading and extracting server files first if needed"
        }

        project.plugins.withId("java") {
            val jarTask = project.tasks.named("jar", Jar::class.java)
            runServerTask.configure {
                it.dependsOn(jarTask)
                it.pluginJar.set(jarTask.flatMap { t -> t.archiveFile })
            }
        }
    }
}
