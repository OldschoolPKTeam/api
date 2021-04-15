package gg.rsmod.plugins.api.ext

import java.lang.IllegalArgumentException
import java.net.URL

fun Any.getPluginResource(name: String): URL {
    return this::class.java.classLoader.getResource(name) ?: throw IllegalArgumentException("No resource found: $name")
}

fun Any.getPluginResourcePackaged(name: String): URL {
    val packageName = this::class.java.`package`.name.replace(".", "/")
    return getPluginResource("$packageName/$name")
}