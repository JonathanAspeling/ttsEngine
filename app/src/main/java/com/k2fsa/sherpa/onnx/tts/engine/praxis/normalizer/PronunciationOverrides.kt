package com.k2fsa.sherpa.onnx.tts.engine.praxis.normalizer

import java.io.File

internal object PronunciationOverrides {

    private var file: File? = null
    private var overrides: LinkedHashMap<String, String> = LinkedHashMap()

    fun init(filePath: String) {
        file = File(filePath)
        reload()
    }

    private fun reload() {
        overrides = (file?.takeIf { it.exists() }?.readLines() ?: emptyList())
            .asSequence()
            .filter { it.isNotBlank() && !it.startsWith('#') }
            .mapNotNull { line ->
                val sep = line.indexOf('=')
                if (sep < 0) null
                else line.substring(0, sep).trim() to line.substring(sep + 1).trim()
            }
            .toMap(LinkedHashMap())
    }

    fun apply(text: String): String {
        if (overrides.isEmpty()) return text
        var result = text
        for ((word, expansion) in overrides.entries.sortedByDescending { it.key.length }) {
            result = result.replace(
                Regex("\\b${Regex.escape(word)}\\b", RegexOption.IGNORE_CASE),
                expansion
            )
        }
        return result
    }

    fun getAll(): List<Pair<String, String>> = overrides.map { it.key to it.value }

    fun add(word: String, expansion: String) {
        val f = file ?: return
        val key = word.trim()
        val lines = if (f.exists()) f.readLines().toMutableList() else mutableListOf()
        lines.removeAll { !it.startsWith('#') && it.substringBefore('=').trim().equals(key, ignoreCase = true) }
        lines.add("$key=${expansion.trim()}")
        f.writeText(lines.joinToString("\n"))
        reload()
    }

    fun remove(word: String) {
        val f = file ?: return
        if (!f.exists()) return
        val lines = f.readLines().filter {
            it.startsWith('#') || !it.substringBefore('=').trim().equals(word, ignoreCase = true)
        }
        f.writeText(lines.joinToString("\n"))
        reload()
    }

    // Inject overrides directly for unit testing (no file needed)
    internal fun loadForTest(pairs: List<Pair<String, String>>) {
        overrides = pairs.toMap(LinkedHashMap())
    }
}
