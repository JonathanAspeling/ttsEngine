package com.k2fsa.sherpa.onnx.tts.engine.praxis.normalizer

internal object NumberNormalizer {

    private val ONES = arrayOf(
        "", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine",
        "ten", "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen",
        "seventeen", "eighteen", "nineteen"
    )
    private val TENS = arrayOf(
        "", "", "twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety"
    )
    private val MONTHS = arrayOf(
        "", "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )
    private val DAY_ORDINALS = mapOf(
        1 to "first", 2 to "second", 3 to "third", 4 to "fourth", 5 to "fifth",
        6 to "sixth", 7 to "seventh", 8 to "eighth", 9 to "ninth", 10 to "tenth",
        11 to "eleventh", 12 to "twelfth", 13 to "thirteenth", 14 to "fourteenth",
        15 to "fifteenth", 16 to "sixteenth", 17 to "seventeenth", 18 to "eighteenth",
        19 to "nineteenth", 20 to "twentieth", 21 to "twenty-first", 22 to "twenty-second",
        23 to "twenty-third", 24 to "twenty-fourth", 25 to "twenty-fifth",
        26 to "twenty-sixth", 27 to "twenty-seventh", 28 to "twenty-eighth",
        29 to "twenty-ninth", 30 to "thirtieth", 31 to "thirty-first"
    )
    private val CURRENCY_UNITS = mapOf('£' to "pounds", '$' to "dollars", '€' to "euros")

    // YYYY-MM-DD
    private val ISO_DATE = Regex("""\b(\d{4})-(0[1-9]|1[0-2])-(0[1-9]|[12]\d|3[01])\b""")

    // £1,200  $50  €9.99  (the $ must be escaped in non-raw string to avoid Kotlin interpolation)
    private val CURRENCY = Regex("([£\$€])\\s*(\\d[\\d,]*)(?:\\.(\\d{2}))?")

    // 10am  10:30pm  (case-insensitive)
    private val TIME = Regex("""\b(1[0-2]|0?[1-9])(?::([0-5]\d))?\s*([ap]m)\b""", RegexOption.IGNORE_CASE)

    // 1st 2nd 3rd … 31st (only 1–31; larger ordinals Piper handles adequately)
    private val ORDINAL = Regex("""\b([1-9]|[12]\d|3[01])(st|nd|rd|th)\b""", RegexOption.IGNORE_CASE)

    // 50%  3.5%
    private val PERCENTAGE = Regex("""(\d+(?:\.\d+)?)\s*%""")

    // 1/2  3/4  (word boundaries so it doesn't fire inside dates or URLs)
    private val FRACTION = Regex("""\b(\d+)/(\d+)\b""")

    // 3.14  0.5  — lookbehind excludes matches after . or digit (e.g. 5.2 inside 1.5.2)
    private val DECIMAL = Regex("""(?<![\d.])(\d+)\.(\d+)(?!\.\d)\b""")

    // 1,200  10,000  1,000,000
    private val COMMA_NUMBER = Regex("""\b(\d{1,3}(?:,\d{3})+)\b""")

    private val COMMON_FRACTIONS = mapOf(
        "1/2" to "a half",      "1/3" to "a third",     "2/3" to "two thirds",
        "1/4" to "a quarter",   "3/4" to "three quarters",
        "1/5" to "a fifth",     "2/5" to "two fifths",  "3/5" to "three fifths", "4/5" to "four fifths",
        "1/8" to "an eighth",   "3/8" to "three eighths", "5/8" to "five eighths", "7/8" to "seven eighths"
    )

    fun normalize(text: String): String = text
        .let { replaceIsoDates(it) }
        .let { replaceCurrency(it) }
        .let { replacePercentages(it) }
        .let { replaceTime(it) }
        .let { replaceOrdinals(it) }
        .let { replaceFractions(it) }
        .let { replaceDecimals(it) }
        .let { replaceCommaNumbers(it) }

    private fun replaceIsoDates(text: String): String =
        ISO_DATE.replace(text) { m ->
            val year = m.groupValues[1].toInt()
            val month = m.groupValues[2].toInt()
            val day = m.groupValues[3].toInt()
            "${DAY_ORDINALS[day] ?: numberToWords(day.toLong())} of ${MONTHS[month]} ${yearToWords(year)}"
        }

    private fun replaceCurrency(text: String): String =
        CURRENCY.replace(text) { m ->
            val unit = CURRENCY_UNITS[m.groupValues[1][0]] ?: return@replace m.value
            val intPart = m.groupValues[2].replace(",", "").toLongOrNull() ?: return@replace m.value
            val words = numberToWords(intPart)
            val cents = m.groupValues[3].takeIf { it.isNotEmpty() }?.toIntOrNull() ?: 0
            if (cents > 0) "$words $unit and ${numberToWords(cents.toLong())} cents" else "$words $unit"
        }

    private fun replaceTime(text: String): String =
        TIME.replace(text) { m ->
            val hour = m.groupValues[1].toIntOrNull() ?: return@replace m.value
            val minutes = m.groupValues[2]
            val period = if (m.groupValues[3].lowercase() == "am") "a.m." else "p.m."
            buildString {
                append(numberToWords(hour.toLong()))
                if (minutes.isNotEmpty()) append(" ${numberToWords(minutes.toLong())}")
                append(" $period")
            }
        }

    private fun replaceOrdinals(text: String): String =
        ORDINAL.replace(text) { m ->
            val n = m.groupValues[1].toIntOrNull() ?: return@replace m.value
            DAY_ORDINALS[n] ?: m.value
        }

    private fun replacePercentages(text: String): String =
        PERCENTAGE.replace(text) { m ->
            val value = m.groupValues[1]
            val dot = value.indexOf('.')
            if (dot == -1) {
                val n = value.toLongOrNull() ?: return@replace m.value
                "${numberToWords(n)} percent"
            } else {
                val intPart = value.substring(0, dot).toLongOrNull() ?: return@replace m.value
                val fracWords = value.substring(dot + 1).map { numberToWords(it.toString().toLong()) }.joinToString(" ")
                "${numberToWords(intPart)} point $fracWords percent"
            }
        }

    private fun replaceFractions(text: String): String =
        FRACTION.replace(text) { m ->
            val key = "${m.groupValues[1]}/${m.groupValues[2]}"
            COMMON_FRACTIONS[key] ?: run {
                val num = m.groupValues[1].toLongOrNull() ?: return@replace m.value
                val den = m.groupValues[2].toLongOrNull() ?: return@replace m.value
                "${numberToWords(num)} over ${numberToWords(den)}"
            }
        }

    private fun replaceDecimals(text: String): String =
        DECIMAL.replace(text) { m ->
            val intPart = m.groupValues[1].toLongOrNull() ?: return@replace m.value
            val fracWords = m.groupValues[2].map { numberToWords(it.toString().toLong()) }.joinToString(" ")
            "${numberToWords(intPart)} point $fracWords"
        }

    private fun replaceCommaNumbers(text: String): String =
        COMMA_NUMBER.replace(text) { m ->
            val n = m.groupValues[1].replace(",", "").toLongOrNull() ?: return@replace m.value
            numberToWords(n)
        }

    internal fun numberToWords(n: Long): String {
        if (n == 0L) return "zero"
        if (n < 0) return "minus ${numberToWords(-n)}"

        fun below1000(x: Long): String = when {
            x < 20 -> ONES[x.toInt()]
            x < 100 -> TENS[(x / 10).toInt()] + if (x % 10 != 0L) " ${ONES[(x % 10).toInt()]}" else ""
            else -> "${ONES[(x / 100).toInt()]} hundred" + if (x % 100 != 0L) " ${below1000(x % 100)}" else ""
        }

        return when {
            n < 1_000L ->
                below1000(n)
            n < 1_000_000L ->
                "${below1000(n / 1_000)} thousand" +
                        if (n % 1_000 != 0L) " ${below1000(n % 1_000)}" else ""
            n < 1_000_000_000L ->
                "${below1000(n / 1_000_000)} million" +
                        if (n % 1_000_000 != 0L) " ${numberToWords(n % 1_000_000)}" else ""
            else ->
                "${below1000(n / 1_000_000_000)} billion" +
                        if (n % 1_000_000_000 != 0L) " ${numberToWords(n % 1_000_000_000)}" else ""
        }
    }

    internal fun yearToWords(year: Int): String = when {
        year < 100 -> numberToWords(year.toLong())
        year in 1100..1999 -> {
            val high = year / 100
            val low = year % 100
            if (low == 0) "${numberToWords(high.toLong())} hundred"
            else "${numberToWords(high.toLong())} ${numberToWords(low.toLong())}"
        }
        year == 2000 -> "two thousand"
        year in 2001..2009 -> "two thousand and ${numberToWords((year - 2000).toLong())}"
        year in 2010..2099 -> "twenty ${numberToWords((year - 2000).toLong())}"
        else -> numberToWords(year.toLong())
    }
}
