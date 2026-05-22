package net.esieben.hybuild.project

import groovy.json.JsonOutput
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault(because = "Writes manifest.json into the project source tree, not the build directory")
abstract class CreateManifestTask : AbstractManifestTask() {

    @get:OutputFile
    abstract override val manifestFile: RegularFileProperty

    @TaskAction
    fun createManifest() {
        validateProjectCoordinates()
        warnIfOptionalMissing()

        val manifest = buildMap {
            put("Group", project.group.toString())
            put("Name", project.name)
            put("Version", project.version.toString())
            put("Authors", authors.get().map { mapOf("Name" to it) })
            put("Main", deriveMainClass())
            put("ServerVersion", resolveServerVersion())
            pluginDescription.orNull?.let { put("Description", it) }
            website.orNull?.let { put("Website", it) }
        }

        val output = manifestFile.get().asFile
        output.parentFile.mkdirs()
        output.writeText(JsonOutput.prettyPrint(JsonOutput.toJson(manifest)))
        logger.lifecycle("Manifest written to ${output.absolutePath}")
    }
}
