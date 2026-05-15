package net.esieben.hybuild

import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.Test
import kotlin.test.assertNotNull

class HyBuildTest {

    val pluginPackage: String = this::class.java.packageName

    @Test
    fun `plugin applies successfully to project`() {
        val project = ProjectBuilder.builder().build()

        project.pluginManager.apply(pluginPackage)

        assertNotNull(project.plugins.getPlugin(HyBuild::class.java))
    }
}
