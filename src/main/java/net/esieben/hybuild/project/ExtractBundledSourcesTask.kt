package net.esieben.hybuild.project

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File

/**
 * Extracts Java source bundles that are shipped as plugin resources into the project's build
 * directory so they can be compiled as part of the user's project.
 *
 * Each bundle is a resource directory inside the plugin JAR. A bundle named {@code "codec-sources"}
 * must contain an accompanying manifest at {@code /codec-sources/manifest.txt} listing one
 * {@code packagePath:fileName} entry per line, e.g.:
 * <pre>
 *   net/esieben/hybuild/codec:CodecToken.java
 *   net/esieben/hybuild/codec:SimpleBuilderCodec.java
 * </pre>
 */
@CacheableTask
abstract class ExtractBundledSourcesTask : DefaultTask() {

    @get:Input
    abstract val bundles: ListProperty<String>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun extract() {
        var total = 0
        for (bundle in bundles.get()) {
            val manifestStream = ExtractBundledSourcesTask::class.java
                .getResourceAsStream("/$bundle/manifest.txt")
                ?: error("Bundle '$bundle' has no manifest at /$bundle/manifest.txt")

            val entries = manifestStream.bufferedReader().use { it.readLines() }
                .map { it.trim() }
                .filter { it.isNotEmpty() }

            for (entry in entries) {
                val (packagePath, fileName) = entry.split(":", limit = 2)
                val destDir = File(outputDirectory.get().asFile, packagePath)
                destDir.mkdirs()
                val srcStream = ExtractBundledSourcesTask::class.java
                    .getResourceAsStream("/$bundle/$fileName")
                    ?: error("Bundled source not found: /$bundle/$fileName")
                srcStream.use { it.copyTo(File(destDir, fileName).outputStream()) }
                total++
            }
        }
        logger.lifecycle("ExtractBundledSources: $total file(s) written to ${outputDirectory.get().asFile}")
    }
}
