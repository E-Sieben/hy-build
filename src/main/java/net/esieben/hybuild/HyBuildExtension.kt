package net.esieben.hybuild

import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

abstract class HyBuildExtension {
    abstract val authors: ListProperty<String>
    abstract val description: Property<String>
    abstract val website: Property<String>
}
