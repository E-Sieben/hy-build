package net.esieben.hybuild.project

import net.esieben.hybuild.HyBuild
import net.esieben.hybuild.server.ExtractServerZipTask
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional

abstract class AbstractManifestTask : DefaultTask() {

    @get:InputDirectory
    @get:Optional
    abstract val hytaleFolder: DirectoryProperty

    @get:Input
    abstract val authors: ListProperty<String>

    @get:Input
    @get:Optional
    abstract val pluginDescription: Property<String>

    @get:Input
    @get:Optional
    abstract val website: Property<String>

    @get:Internal
    abstract val manifestFile: RegularFileProperty

    init {
        hytaleFolder.convention(project.layout.projectDirectory.dir(HyBuild.PLUGIN_FOLDER))
        manifestFile.convention(
            project.layout.projectDirectory.file("src/main/resources/manifest.json")
        )
    }

    protected fun validateProjectCoordinates() {
        check(project.group.toString().isNotBlank()) {
            "project.group is not set. Add 'group = \"com.example\"' to build.gradle.kts."
        }
        val version = project.version.toString()
        check(version.isNotBlank() && version != "unspecified") {
            "project.version is not set. Add 'version = \"1.0.0\"' to build.gradle.kts."
        }
    }

    protected fun warnIfOptionalMissing() {
        if (!pluginDescription.isPresent) logger.lifecycle(MISSING_DESCRIPTION_MSG)
        if (!website.isPresent) logger.lifecycle(MISSING_WEBSITE_MSG)
    }

    fun deriveMainClassName(): String {
        val className = project.rootProject.name
            .split("-")
            .joinToString("") { it.replaceFirstChar(Char::uppercaseChar) }
        return className
    }

    protected fun deriveMainClass(): String {
        val className = deriveMainClassName()
        return "${project.group}.${className.lowercase()}.$className"
    }

    protected fun resolveServerVersion(): String {
        val folder = hytaleFolder.get().asFile
        return folder.listFiles()
            ?.filter { it.isFile && it.name.matches(ExtractServerZipTask.VERSION_ZIP_PATTERN) }
            ?.maxByOrNull { it.name }
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
