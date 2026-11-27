package ai.bondhu.assistant.util

private val bn = arrayOf("০", "১", "২", "৩", "৪", "৫", "৬", "৭", "৮", "৯")

fun String.toBengaliDigits(): String = map { c ->
    if (c in '0'..'9') bn[c - '0'] else c
}.joinToString("")

fun String.normalizeDigits(): String = map { c ->
    val idx = bn.indexOf(c.toString())
    if (idx >= 0) ('0' + idx) else c
}.joinToString("")

fun bengaliVolume(percent: Int): String = "$percent শতাংশ".toBengaliDigits()
