package net.esieben.hybuild.project

import net.esieben.hybuild.HyBuild
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class AddHytaleFolderToGitignoreTask : DefaultTask() {

    companion object {
        const val SECTION_HEADER = "### Hytale ###"
        const val BLOCK_TO_APPEND = "\n$SECTION_HEADER\n${HyBuild.PLUGIN_FOLDER}\n"
    }

    @get:Internal
    abstract val gitignoreFile: RegularFileProperty

    init {
        gitignoreFile.convention(
            project.layout.projectDirectory.file(".gitignore")
        )
    }

    @TaskAction
    fun addHytaleToGitignore() {
        val file: File = gitignoreFile.get().asFile

        if (!file.exists()) {
            logger.lifecycle("No .gitignore found at '${file.path}'. Creating one.")
            file.writeText(BLOCK_TO_APPEND.trimStart('\n') + "\n")
            return
        }

        val content = file.readText()
        val lines = content.lines()

        val alreadyPresent = lines.any { it.trim() == HyBuild.PLUGIN_FOLDER }

        if (alreadyPresent) {
            logger.lifecycle("'${HyBuild.PLUGIN_FOLDER}' is already present in '${file.path}'. Nothing to do.")
            return
        }

        logger.lifecycle("Appending Hytale section to '${file.path}'.")

        val separator = if (content.endsWith("\n")) "" else "\n"
        file.appendText("$separator$BLOCK_TO_APPEND")
    }
}