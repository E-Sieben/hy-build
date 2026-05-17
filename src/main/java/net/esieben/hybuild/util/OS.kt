package net.esieben.hybuild.util

class OS {
    companion object {
        val isWindows: Boolean get() = System.getProperty("os.name").lowercase().contains("win")
        val isMac: Boolean get() = System.getProperty("os.name").lowercase().contains("mac")
        val hasDisplay: Boolean get() = System.getenv("DISPLAY") != null || System.getenv("WAYLAND_DISPLAY") != null
    }
}