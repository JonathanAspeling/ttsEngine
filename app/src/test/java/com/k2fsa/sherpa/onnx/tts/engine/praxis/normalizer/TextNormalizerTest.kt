package com.k2fsa.sherpa.onnx.tts.engine.praxis.normalizer

import org.junit.Assert.assertEquals
import org.junit.Test

class TextNormalizerTest {

    // ── NumberNormalizer ─────────────────────────────────────────────────────

    @Test fun `ISO date`() =
        assertEquals("fourth of July twenty twenty six", NumberNormalizer.normalize("2026-07-04"))

    @Test fun `ISO date year 1999`() =
        assertEquals("first of January nineteen ninety nine", NumberNormalizer.normalize("1999-01-01"))

    @Test fun `ISO date year 2000`() =
        assertEquals("first of January two thousand", NumberNormalizer.normalize("2000-01-01"))

    @Test fun `ISO date year 2001`() =
        assertEquals("first of January two thousand and one", NumberNormalizer.normalize("2001-01-01"))

    @Test fun `currency pounds`() =
        assertEquals("one thousand two hundred pounds", NumberNormalizer.normalize("£1,200"))

    @Test fun `currency dollars`() =
        assertEquals("fifty dollars", NumberNormalizer.normalize("$50"))

    @Test fun `currency with cents`() =
        assertEquals("nine dollars and ninety nine cents", NumberNormalizer.normalize("$9.99"))

    @Test fun `time am`() =
        assertEquals("ten a.m.", NumberNormalizer.normalize("10am"))

    @Test fun `time pm with minutes`() =
        assertEquals("ten thirty p.m.", NumberNormalizer.normalize("10:30pm"))

    @Test fun `ordinal 1st`() =
        assertEquals("first place", NumberNormalizer.normalize("1st place"))

    @Test fun `ordinal 21st`() =
        assertEquals("twenty-first", NumberNormalizer.normalize("21st"))

    @Test fun `ordinal 31st`() =
        assertEquals("thirty-first", NumberNormalizer.normalize("31st"))

    @Test fun `comma number`() =
        assertEquals("one thousand five hundred items", NumberNormalizer.normalize("1,500 items"))

    @Test fun `comma number large`() =
        assertEquals("one million items", NumberNormalizer.normalize("1,000,000 items"))

    @Test fun `numberToWords zero`() =
        assertEquals("zero", NumberNormalizer.numberToWords(0))

    @Test fun `numberToWords hundred`() =
        assertEquals("one hundred", NumberNormalizer.numberToWords(100))

    @Test fun `numberToWords thousand`() =
        assertEquals("one thousand two hundred", NumberNormalizer.numberToWords(1200))

    // ── AbbreviationExpander ─────────────────────────────────────────────────

    @Test fun `Dr expansion`() =
        assertEquals("Doctor Smith", AbbreviationExpander.expand("Dr. Smith"))

    @Test fun `etc expansion`() =
        assertEquals("apples, oranges, etcetera", AbbreviationExpander.expand("apples, oranges, etc."))

    @Test fun `ie expansion`() =
        assertEquals("that is, a test", AbbreviationExpander.expand("i.e., a test"))

    @Test fun `eg expansion`() =
        assertEquals("for example something", AbbreviationExpander.expand("e.g. something"))

    @Test fun `acronym API`() =
        assertEquals("A P I endpoint", AbbreviationExpander.expand("API endpoint"))

    @Test fun `acronym URL`() =
        assertEquals("U R L", AbbreviationExpander.expand("URL"))

    @Test fun `acronym WPP`() =
        assertEquals("W P P", AbbreviationExpander.expand("WPP"))

    @Test fun `OK is not expanded`() =
        assertEquals("OK", AbbreviationExpander.expand("OK"))

    @Test fun `lowercase words not touched`() =
        assertEquals("the cat sat", AbbreviationExpander.expand("the cat sat"))

    // ── PauseInjector ────────────────────────────────────────────────────────

    @Test fun `sentence pause`() =
        assertEquals("Hello.. World", PauseInjector.inject("Hello. World"))

    @Test fun `semicolon becomes sentence pause`() =
        assertEquals("Hello.. world", PauseInjector.inject("Hello; world"))

    @Test fun `paragraph break becomes sentence boundary`() =
        assertEquals("A.. B", PauseInjector.inject("A\n\nB"))

    @Test fun `single newline becomes space`() =
        assertEquals("A B", PauseInjector.inject("A\nB"))

    @Test fun `em dash becomes comma`() =
        assertEquals("A,B", PauseInjector.inject("A—B"))

    @Test fun `spaced hyphen becomes comma pause`() =
        assertEquals("A, B", PauseInjector.inject("A - B"))

    @Test fun `ellipsis not multiplied`() {
        val result = PauseInjector.inject("Hello... world")
        assertEquals("Hello... world", result)
    }

    @Test fun `double period not extended further`() {
        val result = PauseInjector.inject("Hello.. world")
        assertEquals("Hello.. world", result)
    }

    // ── TextNormalizer integration ───────────────────────────────────────────

    @Test fun `full pipeline`() {
        val input = "The API costs £1,200. See Dr. Smith."
        val result = TextNormalizer.normalize(input)
        assertEquals("The A P I costs one thousand two hundred pounds.. See Doctor Smith.", result)
    }

    @Test fun `date in sentence`() {
        val input = "The meeting is on 2026-07-04."
        val result = TextNormalizer.normalize(input)
        assertEquals("The meeting is on fourth of July twenty twenty six.", result)
    }

    @Test fun `empty string`() =
        assertEquals("", TextNormalizer.normalize(""))

    @Test fun `plain text unchanged`() =
        assertEquals("hello world", TextNormalizer.normalize("hello world"))
}
