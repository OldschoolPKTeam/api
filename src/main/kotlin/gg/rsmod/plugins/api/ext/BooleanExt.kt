package gg.rsmod.plugins.api.ext

fun Boolean.toInt() = if (this) 1 else 0

fun Boolean.toReverseInt() = if (this) 0 else 1