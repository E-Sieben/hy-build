package net.esieben.hybuild.project

import net.esieben.hybuild.HyBuild
import org.gradle.testfixtures.ProjectBuilder
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AddHytaleFolderToGitignoreTaskTest {

    private fun setup(): Pair<File, AddHytaleFolderToGitignoreTask> {
        val projectDir = Files.createTempDirectory("hy-build-test").toFile()
        val project = ProjectBuilder.builder().withProjectDir(projectDir).build()
        val task = project.tasks.register("addHytaleFolderToGitignore", AddHytaleFolderToGitignoreTask::class.java).get()
        return projectDir to task
    }

    @Test
    fun `creates gitignore with hytale section when file does not exist`() {
        // Arrange
        val (projectDir, task) = setup()

        // Act
        task.addHytaleToGitignore()

        // Assert
        val content = File(projectDir, ".gitignore").readText()
        assertTrue(HyBuild.PLUGIN_FOLDER in content)
    }

    @Test
    fun `appends hytale section to existing gitignore without altering existing content`() {
        // Arrange
        val (projectDir, task) = setup()
        File(projectDir, ".gitignore").writeText("build/\n.gradle/\n")

        // Act
        task.addHytaleToGitignore()

        // Assert
        val content = File(projectDir, ".gitignore").readText()
        assertTrue("build/" in content)
        assertTrue(".gradle/" in content)
        assertTrue(HyBuild.PLUGIN_FOLDER in content)
    }

    @Test
    fun `does not add hytale section when already present`() {
        // Arrange
        val (projectDir, task) = setup()
        File(projectDir, ".gitignore").writeText("${HyBuild.PLUGIN_FOLDER}\n")

        // Act
        task.addHytaleToGitignore()

        // Assert
        val occurrences = File(projectDir, ".gitignore").readLines()
            .count { it.trim() == HyBuild.PLUGIN_FOLDER }
        assertEquals(1, occurrences)
    }

    @Test
    fun `inserts newline separator before section when existing file has no trailing newline`() {
        // Arrange
        val (projectDir, task) = setup()
        File(projectDir, ".gitignore").writeText("build/") // no trailing newline

        // Act
        task.addHytaleToGitignore()

        // Assert — original content and new section must be on separate lines
        val content = File(projectDir, ".gitignore").readText()
        assertTrue(content.startsWith("build/\n"))
        assertTrue(HyBuild.PLUGIN_FOLDER in content)
    }
}
