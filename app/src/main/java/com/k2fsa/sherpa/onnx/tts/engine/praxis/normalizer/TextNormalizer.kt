package com.k2fsa.sherpa.onnx.tts.engine.praxis.normalizer

/**
 * Normalizes raw TTS input text before synthesis.
 *
 * Pipeline (order matters):
 *   1. NumberNormalizer  — ISO dates, currency, time, ordinals, comma-numbers
 *   2. AbbreviationExpander — title abbreviations, then acronyms
 *   3. PauseInjector     — sentence-pause rules (runs last so abbreviation dots are already gone)
 *
 * Pure Kotlin/JVM — no Android imports. Testable as plain JUnit tests.
 */
object TextNormalizer {
    fun normalize(input: String): String = input
        .let(NumberNormalizer::normalize)
        .let(AbbreviationExpander::expand)
        .let(PauseInjector::inject)
}
