package net.esieben.hybuild.util

class OS {
    companion object {
        val isWindows: Boolean get() = System.getProperty("os.name").lowercase().contains("win")
    }
}