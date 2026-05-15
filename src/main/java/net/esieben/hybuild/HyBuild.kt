package net.esieben.hybuild

import org.gradle.api.Plugin
import org.gradle.api.Project

class HyBuild : Plugin<Project> {
    val className: String = this::class.java.simpleName

    override fun apply(project: Project) {
        println("Hello from the $className")
    }
}