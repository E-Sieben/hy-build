package net.esieben.hybuild.project

import org.gradle.testfixtures.ProjectBuilder
import java.io.File
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ValidateManifestTaskTest {

    private fun taskWithManifest(json: String): ValidateManifestTask {
        val manifestFile = File.createTempFile("manifest", ".json").also { it.writeText(json) }
        val project = ProjectBuilder.builder().build()
        return project.tasks.register("validateManifest", ValidateManifestTask::class.java) {
            it.manifestFile.set(manifestFile)
        }.get()
    }

    private fun validManifest(
        group: String = "com.example",
        name: String = "my-plugin",
        version: String = "1.0.0",
        authors: String = """[{"Name": "Alice"}]""",
        main: String = "com.example.MyPlugin",
        serverVersion: String = "2024.01.01-abc"
    ) = """
        {
            "Group": "$group",
            "Name": "$name",
            "Version": "$version",
            "Authors": $authors,
            "Main": "$main",
            "ServerVersion": "$serverVersion"
        }
    """.trimIndent()

    @Test
    fun `valid manifest passes without error`() {
        // Arrange
        val task = taskWithManifest(validManifest())

        // Act + Assert
        task.validateManifest()
    }

    @Test
    fun `missing required field fails and names the field`() {
        // Arrange — remove Group field
        val json = validManifest().replace(""""Group": "com.example",""", "")
        val task = taskWithManifest(json)

        // Act
        val ex = assertFailsWith<IllegalStateException> { task.validateManifest() }

        // Assert
        assertTrue("Group" in ex.message!!)
    }

    @Test
    fun `empty authors array fails`() {
        // Arrange
        val task = taskWithManifest(validManifest(authors = "[]"))

        // Act
        val ex = assertFailsWith<IllegalStateException> { task.validateManifest() }

        // Assert
        assertTrue("Authors" in ex.message!!)
    }

    @Test
    fun `non-semver version string fails`() {
        // Arrange
        val task = taskWithManifest(validManifest(version = "1.0-SNAPSHOT"))

        // Act
        val ex = assertFailsWith<IllegalStateException> { task.validateManifest() }

        // Assert
        assertTrue("SemVer" in ex.message!!)
    }

    @Test
    fun `main class without package prefix fails`() {
        // Arrange
        val task = taskWithManifest(validManifest(main = "MyPlugin"))

        // Act
        val ex = assertFailsWith<IllegalStateException> { task.validateManifest() }

        // Assert
        assertTrue("Main" in ex.message!!)
    }

    @Test
    fun `semver with pre-release and build metadata passes`() {
        // Arrange
        val task = taskWithManifest(validManifest(version = "1.0.0-alpha.1+build.42"))

        // Act + Assert
        task.validateManifest()
    }

    @Test
    fun `multiple validation errors are all reported in one failure`() {
        // Arrange — remove Group and use invalid version, so we get at least 2 errors
        val json = validManifest().replace(""""Group": "com.example",""", "")
            .replace(""""1.0.0"""", """"not-semver"""")
        val task = taskWithManifest(json)

        // Act
        val ex = assertFailsWith<IllegalStateException> { task.validateManifest() }

        // Assert
        assertTrue("Group" in ex.message!!)
        assertTrue("SemVer" in ex.message!!)
    }
}
