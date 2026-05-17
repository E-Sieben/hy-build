package net.esieben.hybuild.project

import net.esieben.hybuild.HyBuild
import net.esieben.hybuild.server.ExtractServerZipTask
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile

abstract class AbstractManifestTask : DefaultTask() {

    @get:Internal
    abstract val hytaleFolder: DirectoryProperty

    @get:Input
    abstract val authors: ListProperty<String>

    @get:Input
    @get:Optional
    abstract val pluginDescription: Property<String>

    @get:Input
    @get:Optional
    abstract val website: Property<String>

    @get:OutputFile
    abstract val manifestFile: RegularFileProperty

    init {
        hytaleFolder.convention(project.layout.projectDirectory.dir(HyBuild.PLUGIN_FOLDER))
        manifestFile.convention(
            project.layout.projectDirectory.file("src/main/resources/manifest.json")
        )
    }

    protected fun warnIfOptionalMissing() {
        if (!pluginDescription.isPresent) logger.lifecycle(MISSING_DESCRIPTION_MSG)
        if (!website.isPresent) logger.lifecycle(MISSING_WEBSITE_MSG)
    }

    protected fun deriveMainClass(): String {
        val className = project.rootProject.name
            .split("-")
            .joinToString("") { it.replaceFirstChar(Char::uppercaseChar) }
        return "${project.group}.$className"
    }

    protected fun resolveServerVersion(): String {
        val folder = hytaleFolder.get().asFile
        return folder.listFiles()
            ?.firstOrNull { it.isFile && it.name.matches(ExtractServerZipTask.VERSION_ZIP_PATTERN) }
            ?.nameWithoutExtension
            ?: error(
                "No server version ZIP found in '${folder.absolutePath}'. " +
                        "Run launchServerDownloader first."
            )
    }

    companion object {
        private const val MISSING_DESCRIPTION_MSG =
            "No description set for this plugin. " +
                    "Add 'description = \"...\"' inside a hytale { } block in build.gradle.kts."
        private const val MISSING_WEBSITE_MSG =
            "No website set for this plugin. " +
                    "Add 'website = \"...\"' inside a hytale { } block in build.gradle.kts."
    }
}
