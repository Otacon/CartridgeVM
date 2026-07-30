package io

internal actual fun readTextResource(path: String): String {
    val normalizedPath = path.removePrefix("/")
    val stream = Thread.currentThread().contextClassLoader.getResourceAsStream(normalizedPath)
        ?: object {}.javaClass.classLoader.getResourceAsStream(normalizedPath)
    return checkNotNull(stream) { "Missing resource: $path" }
        .bufferedReader()
        .use { it.readText() }
}
