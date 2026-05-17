package net.esieben.hybuild.project

import org.gradle.api.tasks.TaskAction
import java.time.LocalDate

abstract class InitializeProjectTask : AbstractManifestTask() {

    @TaskAction
    fun initializeProject() {
        val classContent = """
            package ${project.group}.${deriveMainClassName().lowercase()};

            import com.hypixel.hytale.logger.HytaleLogger;
            import com.hypixel.hytale.server.core.plugin.JavaPlugin;
            import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
            import java.util.logging.Level;
            import lombok.NonNull;
            
            /**
             * Main entry point for ${project.name}.
             * <p>
             * ${pluginDescription.getOrElse("Please add a description here")}
             *
             * @author ${project.group}
             * @since ${LocalDate.now()}
             */
            @SuppressWarnings("unused")
            public class ${deriveMainClassName()} extends JavaPlugin {
            
              /// The logger instance specific to this plugin.
              @NonNull
              private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
            
              /// Plugin Name.
              @NonNull
              private static final String PLUGIN_NAME = "${project.name}";
            
              /// The singleton instance of the plugin, available after initialization.
              private static ${deriveMainClassName()} instance;
            
              /// Initializes the plugin
              public ${deriveMainClassName()}(@NonNull JavaPluginInit init) {
                super(init);
                instance = this;
              }
            
              /**
               * Called during the setup phase. Register event listeners, commands, and component types here.
               * Other plugins may not be fully set up yet at this point.
               */
              @Override
              protected void setup() {
                LOGGER.at(Level.INFO).log("[%s] setup", PLUGIN_NAME);
              }
            
              /**
               * Called after all plugins have completed setup. Safe to interact with other plugins, access the
               * Universe, and perform actions that depend on the full server state. Signals that the plugin has
               * fully started.
               */
              @Override
              protected void start() {
                LOGGER.at(Level.INFO).log("[%s] start", PLUGIN_NAME);
              }
            
              /// Called during server shutdown. Clear static references to prevent memory leaks.
              @Override
              protected void shutdown() {
                instance = null;
                LOGGER.at(Level.INFO).log("[%s] shutdown", PLUGIN_NAME);
              }
            }
        """.trimIndent()

        val mainClassFile = project.layout.projectDirectory.dir(
            "src/main/java/${deriveMainClass().replace(".", "/")}.java"
        ).asFile
        if (!mainClassFile.exists()) {
            mainClassFile.parentFile.mkdirs()
        }
        mainClassFile.writeText(classContent)

        logger.lifecycle("Generated custom Main class at: ${mainClassFile.absolutePath}")

    }
}