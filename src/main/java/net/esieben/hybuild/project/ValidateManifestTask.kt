package net.esieben.hybuild.project

import groovy.json.JsonSlurper
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction

abstract class ValidateManifestTask : DefaultTask() {

    @get:InputFile
    abstract val manifestFile: RegularFileProperty

    init {
        manifestFile.convention(
            project.layout.projectDirectory.file("src/main/resources/manifest.json")
        )
    }

    @TaskAction
    fun validateManifest() {
        val file = manifestFile.get().asFile

        @Suppress("UNCHECKED_CAST")
        val manifest = JsonSlurper().parse(file) as Map<String, Any?>

        val errors = mutableListOf<String>()

        listOf("Group", "Name", "Version", "Main", "ServerVersion").forEach { field ->
            if (manifest[field]?.toString().isNullOrBlank()) errors += "Missing required field: $field"
        }

        val authors = manifest["Authors"]
        when {
            authors == null -> errors += "Missing required field: Authors"
            authors !is List<*> || authors.isEmpty() -> errors += "Authors must be a non-empty array"
            else -> {
                @Suppress("UNCHECKED_CAST")
                (authors as List<Map<String, Any?>>).forEachIndexed { i, author ->
                    if (author["Name"]?.toString().isNullOrBlank()) errors += "Authors[$i].Name is required"
                }
            }
        }

        manifest["Version"]?.toString()?.let { version ->
            if (!version.matches(SEMVER_PATTERN)) errors += "Version '$version' is not a valid SemVer"
        }

        manifest["Main"]?.toString()?.let { main ->
            if (!main.matches(MAIN_CLASS_PATTERN)) errors += "Main '$main' does not match FQN class pattern"
        }

        check(errors.isEmpty()) {
            "manifest.json validation failed:\n" + errors.joinToString("\n") { "  - $it" }
        }

        logger.lifecycle("manifest.json is valid.")
    }

    companion object {
        val SEMVER_PATTERN = Regex(
            """^(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)""" +
                    """(?:-((?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\.(?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?""" +
                    """(?:\+([0-9a-zA-Z-]+(?:\.[0-9a-zA-Z-]+)*))?$"""
        )
        val MAIN_CLASS_PATTERN = Regex("""^(?:[a-z][a-z0-9]*\.)+[A-Z][A-Za-z0-9]*$""")
    }
}
