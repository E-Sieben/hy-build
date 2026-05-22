package net.esieben.hybuild.project

import net.esieben.hybuild.HyBuild
import net.esieben.hybuild.HytaleVersionSource
import org.gradle.testfixtures.ProjectBuilder
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PrepareHytaleClasspathTaskTest {

    private fun setup(version: String = "2024.01.01-abc"): Triple<PrepareHytaleClasspathTask, File, File> {
        val projectDir = Files.createTempDirectory("hy-build-test").toFile()
        val hytaleDir = File(projectDir, HyBuild.PLUGIN_FOLDER).also { it.mkdirs() }
        val mavenDir = Files.createTempDirectory("hy-build-maven").toFile()

        File(hytaleDir, "$version.zip").also { zip ->
            ZipOutputStream(zip.outputStream()).use { it.putNextEntry(ZipEntry("dummy")); it.closeEntry() }
        }

        val fakeJar = File(hytaleDir, "HytaleServer.jar").also { it.writeText("fake-jar") }
        val fakeJavadocJar = File(hytaleDir, "HytaleServer-javadoc.jar").also { it.writeText("fake-javadoc") }
        val artifactDir = File(mavenDir, "${HytaleVersionSource.HYTALE_GROUP.replace('.', '/')}/${HytaleVersionSource.HYTALE_ARTIFACT}/$version")

        val project = ProjectBuilder.builder().withProjectDir(projectDir).build()
        val task = project.tasks.register("prepareHytaleClasspath", PrepareHytaleClasspathTask::class.java) {
            it.includeAIJavadoc.set(true)
            it.version.set(version)
            it.serverJar.set(fakeJar)
            it.extractedJavadocJar.set(fakeJavadocJar)
            it.mavenLocalArtifactDir.set(artifactDir)
        }.get()

        return Triple(task, artifactDir, fakeJavadocJar)
    }

    @Test
    fun `installs jar and javadoc jar to maven local`() {
        val (task, artifactDir) = setup()
        task.prepare()
        assertTrue(File(artifactDir, "HytaleServer-2024.01.01-abc.jar").exists())
        assertTrue(File(artifactDir, "HytaleServer-2024.01.01-abc-javadoc.jar").exists())
    }

    @Test
    fun `writes pom to maven local`() {
        val (task, artifactDir) = setup()
        task.prepare()
        val pom = File(artifactDir, "HytaleServer-2024.01.01-abc.pom")
        assertTrue(pom.exists())
        assertContains(pom.readText(), "<artifactId>HytaleServer</artifactId>")
        assertContains(pom.readText(), "<version>2024.01.01-abc</version>")
    }

    @Test
    fun `writes gradle module metadata to maven local`() {
        val (task, artifactDir) = setup()
        task.prepare()
        val module = File(artifactDir, "HytaleServer-2024.01.01-abc.module")
        assertTrue(module.exists())
        val content = module.readText()
        assertContains(content, "\"formatVersion\": \"1.1\"")
        assertContains(content, "\"org.gradle.docstype\": \"javadoc\"")
        assertContains(content, "\"org.gradle.category\": \"documentation\"")
        assertContains(content, "HytaleServer-2024.01.01-abc-javadoc.jar")
    }

    @Test
    fun `module metadata has correct group and artifact`() {
        val (task, artifactDir) = setup()
        task.prepare()
        val content = File(artifactDir, "HytaleServer-2024.01.01-abc.module").readText()
        assertContains(content, "\"group\": \"${HytaleVersionSource.HYTALE_GROUP}\"")
        assertContains(content, "\"module\": \"${HytaleVersionSource.HYTALE_ARTIFACT}\"")
        assertContains(content, "\"version\": \"2024.01.01-abc\"")
    }

    @Test
    fun `does not overwrite existing module file`() {
        val (task, artifactDir) = setup()
        task.prepare()
        val moduleFile = File(artifactDir, "HytaleServer-2024.01.01-abc.module")
        moduleFile.writeText("existing-content")
        task.prepare()
        assertContains(moduleFile.readText(), "existing-content")
    }

    @Test
    fun `skips install when version is blank`() {
        val (task, artifactDir) = setup()
        task.version.set("")
        task.prepare()
        assertFalse(File(artifactDir, "HytaleServer-.jar").exists())
    }
}
