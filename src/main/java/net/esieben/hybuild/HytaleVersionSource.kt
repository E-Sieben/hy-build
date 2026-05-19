package net.esieben.hybuild

import net.esieben.hybuild.server.ExtractServerZipTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import java.io.File

abstract class HytaleVersionSource : ValueSource<String, HytaleVersionSource.Parameters> {

    interface Parameters : ValueSourceParameters {
        val hytaleFolder: DirectoryProperty
    }

    override fun obtain(): String? {
        val folder = parameters.hytaleFolder.get().asFile

        folder.listFiles()
            ?.filter { it.isFile && it.name.matches(ExtractServerZipTask.VERSION_ZIP_PATTERN) }
            ?.maxByOrNull { it.name }?.nameWithoutExtension?.let { return it }

        return File(
            System.getProperty("user.home"),
            ".m2/repository/${HYTALE_GROUP.replace('.', '/')}/$HYTALE_ARTIFACT"
        ).listFiles()?.filter { it.isDirectory }?.maxByOrNull { it.lastModified() }?.name
    }

    companion object {
        const val HYTALE_GROUP = "com.hypixel.hytale"
        const val HYTALE_ARTIFACT = "HytaleServer"
    }
}
