package com.k2fsa.sherpa.onnx.tts.engine.praxis.normalizer

internal object PauseInjector {

    // A period not already part of an ellipsis (.. or ...), followed by whitespace.
    // Replaced with double-period + same whitespace: Piper produces a more natural sentence
    // pause at ".." than at single ".".
    private val SENTENCE_END = Regex("""(?<!\.)\.(?!\.)(\s+)""")

    fun inject(text: String): String = text
        .replace(Regex("""\n{2,}"""), ". ")   // paragraph break → sentence boundary
        .replace("\n", " ")                    // single newline → space
        .replace(";", ".")                     // semicolon → sentence end
        .replace("—", ",")                     // em dash → comma pause
        .replace(" - ", ", ")                  // spaced hyphen → comma pause
        .let { SENTENCE_END.replace(it) { m -> "..${m.groupValues[1]}" } }
        .replace(Regex(" {2,}"), " ")          // collapse any double-space artifacts
}
