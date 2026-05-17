package net.esieben.hybuild

import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class HyBuildTest {

    private fun projectWithPlugin() = ProjectBuilder.builder().build().also {
        it.pluginManager.apply(HyBuild::class.java)
    }

    @Test
    fun `plugin applies successfully`() {
        // Arrange + Act
        val project = projectWithPlugin()

        // Assert
        assertNotNull(project.plugins.getPlugin(HyBuild::class.java))
    }

    @Test
    fun `hytale extension is registered`() {
        // Arrange + Act
        val project = projectWithPlugin()

        // Assert
        assertNotNull(project.extensions.findByType(HyBuildExtension::class.java))
    }

    @Test
    fun `server tasks are registered in hytale server group`() {
        // Arrange + Act
        val project = projectWithPlugin()

        // Assert
        listOf("downloadServerDownloader", "launchServerDownloader", "extractServerZip", "runServer")
            .forEach { name ->
                val task = project.tasks.findByName(name)
                assertNotNull(task, "Expected task '$name' to be registered")
                assertEquals("hytale server", task.group, "Task '$name' should be in 'hytale server' group")
            }
    }

    @Test
    fun `project tasks are registered in hytale project group`() {
        // Arrange + Act
        val project = projectWithPlugin()

        // Assert
        listOf("addHytaleFolderToGitignore", "createManifest", "validateManifest", "overwriteManifest")
            .forEach { name ->
                val task = project.tasks.findByName(name)
                assertNotNull(task, "Expected task '$name' to be registered")
                assertEquals("hytale project", task.group, "Task '$name' should be in 'hytale project' group")
            }
    }

    @Test
    fun `lombok plugin is applied when java plugin is present`() {
        // Arrange
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("java")

        // Act
        project.pluginManager.apply(HyBuild::class.java)

        // Assert
        assertNotNull(project.plugins.findPlugin("io.freefair.lombok"))
    }
}
