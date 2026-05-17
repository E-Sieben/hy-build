package net.esieben.hybuild.project

import groovy.json.JsonOutput
import org.gradle.api.tasks.TaskAction

abstract class CreateManifestTask : AbstractManifestTask() {

    @TaskAction
    fun createManifest() {
        warnIfOptionalMissing()

        val manifest = buildMap<String, Any> {
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
