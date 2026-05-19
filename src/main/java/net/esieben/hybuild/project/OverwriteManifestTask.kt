package net.esieben.hybuild.project

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

abstract class OverwriteManifestTask : AbstractManifestTask() {

    @get:OutputFile
    abstract override val manifestFile: RegularFileProperty

    @TaskAction
    fun overwriteManifest() {
        validateProjectCoordinates()
        warnIfOptionalMissing()

        val file = manifestFile.get().asFile

        @Suppress("UNCHECKED_CAST")
        val existing: MutableMap<String, Any?> = if (file.exists()) {
            (JsonSlurper().parse(file) as Map<String, Any?>).toMutableMap()
        } else {
            mutableMapOf()
        }

        val authorsList = authors.get()
        val desired = buildMap {
            put("Group", project.group.toString())
            put("Name", project.name)
            put("Version", project.version.toString())
            if (authorsList.isNotEmpty()) put("Authors", authorsList.map { mapOf("Name" to it) })
            put("Main", deriveMainClass())
            put("ServerVersion", resolveServerVersion())
            pluginDescription.orNull?.let { put("Description", it) }
            website.orNull?.let { put("Website", it) }
        }

        val changed = mutableListOf<String>()
        for ((key, newValue) in desired) {
            if (JsonOutput.toJson(existing[key]) != JsonOutput.toJson(newValue)) {
                logger.lifecycle("  $key: ${existing[key]} → $newValue")
                existing[key] = newValue
                changed += key
            }
        }

        if (changed.isEmpty()) {
            logger.lifecycle("Manifest is already up to date.")
            return
        }

        file.parentFile.mkdirs()
        file.writeText(JsonOutput.prettyPrint(JsonOutput.toJson(existing)))
        logger.lifecycle("Manifest updated (${changed.size} field(s) changed) at ${file.absolutePath}")
    }
}
