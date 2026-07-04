package com.k2fsa.sherpa.onnx.tts.engine.praxis.normalizer

internal object AbbreviationExpander {

    // Sorted longest-first so multi-dot abbreviations (i.e., e.g.) match before any sub-sequence would.
    private val ABBREVIATIONS: List<Pair<String, String>> = listOf(
        "approx." to "approximately",
        "Blvd."   to "Boulevard",
        "Corp."   to "Corporation",
        "Dept."   to "Department",
        "Govt."   to "Government",
        "Inc."    to "Incorporated",
        "Ltd."    to "Limited",
        "Prof."   to "Professor",
        "i.e."    to "that is",
        "e.g."    to "for example",
        "Ave."    to "Avenue",
        "Fig."    to "Figure",
        "Mrs."    to "Missus",
        "Rev."    to "Reverend",
        "etc."    to "etcetera",
        "vs."     to "versus",
        "Dr."     to "Doctor",
        "Mr."     to "Mister",
        "Ms."     to "Miss",
        "No."     to "number",
        "St."     to "Saint",
    )

    // Match 2–5 consecutive uppercase letters at a word boundary.
    private val ACRONYM = Regex("""\b([A-Z]{2,5})\b""")

    // Piper reads these whole-word forms naturally; letter-by-letter sounds odd.
    private val SKIP_ACRONYMS = setOf("OK")

    fun expand(text: String): String {
        var result = text
        for ((abbr, expansion) in ABBREVIATIONS) {
            result = result.replace(abbr, expansion)
        }
        return ACRONYM.replace(result) { m ->
            val word = m.groupValues[1]
            if (word in SKIP_ACRONYMS) word else word.toCharArray().joinToString(" ")
        }
    }
}
