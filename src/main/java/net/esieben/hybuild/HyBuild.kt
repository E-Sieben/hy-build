package net.esieben.hybuild

import net.esieben.hybuild.project.*
import net.esieben.hybuild.server.DownloadServerDownloaderTask
import net.esieben.hybuild.server.ExtractServerZipTask
import net.esieben.hybuild.server.LaunchServerDownloaderTask
import net.esieben.hybuild.server.RunServerTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.bundling.Jar
import java.io.File

class HyBuild : Plugin<Project> {

    companion object {
        const val PLUGIN_FOLDER = ".hytale"
    }

    override fun apply(project: Project) {
        project.logger.lifecycle("${this::class.java.simpleName} Plugin is starting")

        val extension = project.extensions.create("hytale", HyBuildExtension::class.java)
        extension.includeAIJavadoc.convention(true)

        registerServerTasks(project)
        registerProjectTasks(project, extension)

        project.plugins.withId("java") {
            project.plugins.apply("io.freefair.lombok")
            setupHytaleClasspath(project, extension)
        }
    }

    private fun registerServerTasks(project: Project) {
        val hytaleServerGroup = "hytale server"

        val downloadServerDownloaderTask = project.tasks.register(
            "downloadServerDownloader", DownloadServerDownloaderTask::class.java
        ) {
            it.group = hytaleServerGroup
            it.description =
                "Downloads and extracts the official Hytale Server Downloader executable"
        }

        val launchServerDownloaderTask = project.tasks.register(
            "launchServerDownloader", LaunchServerDownloaderTask::class.java
        ) {
            it.dependsOn(downloadServerDownloaderTask)
            it.serverDownloaderExecutable.set(
                downloadServerDownloaderTask.flatMap { t -> t.serverDownloaderExecutable })
            it.group = hytaleServerGroup
            it.description =
                "Runs the official Hytale Server Downloader, fetching the server version package"
        }

        val extractServerZipTask = project.tasks.register(
            "extractServerZip", ExtractServerZipTask::class.java
        ) {
            it.dependsOn(launchServerDownloaderTask)
            it.group = hytaleServerGroup
            it.description =
                "Extracts HytaleServer.jar, HytaleServer.aot and Assets.zip from the downloaded version package"
        }

        val runServerTask = project.tasks.register(
            "runServer", RunServerTask::class.java
        ) {
            it.dependsOn(extractServerZipTask)
            it.group = hytaleServerGroup
            it.description =
                "Starts the Hytale Server, downloading and extracting server files first if needed"
        }

        project.plugins.withId("java") {
            val jarTask = project.tasks.named("jar", Jar::class.java)
            runServerTask.configure {
                it.dependsOn(jarTask)
                it.pluginJar.set(jarTask.flatMap { t -> t.archiveFile })
                it.pluginBaseName.set(jarTask.flatMap { t -> t.archiveBaseName })
            }
        }
    }

    private fun registerProjectTasks(project: Project, extension: HyBuildExtension) {
        val hytaleProjectGroup = "hytale project"

        val addHytaleFolderToGitignoreTask = project.tasks.register(
            "addHytaleFolderToGitignore", AddHytaleFolderToGitignoreTask::class.java
        ) {
            it.group = hytaleProjectGroup
            it.description = "Adds the .hytale folder to your gitignore file"
        }

        val createManifestTask = project.tasks.register(
            "createManifest", CreateManifestTask::class.java
        ) {
            it.authors.convention(extension.authors)
            it.pluginDescription.convention(extension.description)
            it.website.convention(extension.website)
            it.group = hytaleProjectGroup
            it.description =
                "Generates a fresh manifest.json, replacing all content including any manually added fields"
        }

        val validateManifestTask = project.tasks.register(
            "validateManifest", ValidateManifestTask::class.java
        ) {
            it.group = hytaleProjectGroup
            it.description = "Validates the plugin manifest.json against the Hytale manifest schema"
        }

        val overwriteManifestTask = project.tasks.register(
            "overwriteManifest", OverwriteManifestTask::class.java
        ) {
            it.authors.convention(extension.authors)
            it.pluginDescription.convention(extension.description)
            it.website.convention(extension.website)
            it.finalizedBy(validateManifestTask)
            it.group = hytaleProjectGroup
            it.description =
                "Updates only changed fields in the existing manifest.json, preserving manually added fields"
        }

        val extractServerZipTask = project.tasks.named("extractServerZip")
        createManifestTask.configure { t -> t.mustRunAfter(extractServerZipTask) }

        project.tasks.register(
            "initializeProject", InitializeProjectTask::class.java
        ) {
            it.dependsOn(extractServerZipTask, createManifestTask, addHytaleFolderToGitignoreTask)
            it.authors.convention(extension.authors)
            it.pluginDescription.convention(extension.description)
            it.website.convention(extension.website)
            it.group = hytaleProjectGroup
            it.description = "Creates all necessary files needed for the project"
        }

        project.plugins.withId("java") {
            project.tasks.named("processResources") { it.dependsOn(overwriteManifestTask) }
            project.tasks.named("check") { it.dependsOn(overwriteManifestTask) }
        }
    }

    private fun setupHytaleClasspath(project: Project, extension: HyBuildExtension) {
        val hytaleDir = project.layout.projectDirectory.dir(PLUGIN_FOLDER)

        val versionProvider = project.providers.of(HytaleVersionSource::class.java) {
            it.parameters.hytaleFolder.set(hytaleDir)
        }

        val extractServerZipTask = project.tasks.named("extractServerZip", ExtractServerZipTask::class.java)

        project.tasks.register("prepareHytaleClasspath", PrepareHytaleClasspathTask::class.java) {
            it.group = "hytale project"
            it.description =
                "Extracts the AI-generated javadoc JAR and installs Hytale Server to Maven Local"
            it.includeAIJavadoc.set(extension.includeAIJavadoc)
            it.version.set(versionProvider)
            it.serverJar.set(extractServerZipTask.flatMap { t -> t.serverJar })
            it.extractedJavadocJar.set(hytaleDir.file("HytaleServer-javadoc.jar"))
            it.mavenLocalArtifactDir.set(
                project.layout.dir(
                versionProvider.map { ver ->
                    File(
                        System.getProperty("user.home"),
                        ".m2/repository/${
                            HytaleVersionSource.HYTALE_GROUP.replace(
                                '.',
                                '/'
                            )
                        }/${HytaleVersionSource.HYTALE_ARTIFACT}/$ver"
                    )
                }))
        }

        project.tasks.named("compileJava") { it.dependsOn(project.tasks.named("prepareHytaleClasspath")) }

        project.repositories.mavenLocal()

        project.configurations.getByName("compileOnly").withDependencies { deps ->
            val javadoc = extension.includeAIJavadoc.get()
            val ver = versionProvider.orNull

            if (javadoc && !ver.isNullOrBlank()) {
                deps.add(
                    project.dependencies.create(
                        "${HytaleVersionSource.HYTALE_GROUP}:${HytaleVersionSource.HYTALE_ARTIFACT}:$ver"
                    )
                )
            } else {
                deps.add(project.dependencies.create(project.files(hytaleDir.file("HytaleServer.jar"))))
            }
        }
    }
}
