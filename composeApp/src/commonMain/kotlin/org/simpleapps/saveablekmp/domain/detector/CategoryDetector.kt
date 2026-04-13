package org.simpleapps.saveablekmp.domain.detector

object CategoryDetector {

    private val phoneRegex = Regex("""^[\+]?[(]?[0-9]{3}[)]?[-\s\.]?[0-9]{3}[-\s\.]?[0-9]{4,6}$""")
    private val emailRegex = Regex("""^[^\s@]+@[^\s@]+\.[^\s@]+$""")
    private val urlRegex   = Regex("""^https?://""", RegexOption.IGNORE_CASE)
    private val dateRegex  = Regex("""\d{1,2}[./\-]\d{1,2}[./\-]\d{2,4}""")
    private val passRegex  = Regex("""^(?=.*[A-Z])(?=.*[0-9])[A-Za-z0-9!@#${'$'}%^&*_\-]{8,}$""")

    fun detect(value: String): String {
        val v = value.trim()
        return when {
            v.startsWith("data:image")         -> "image"
            urlRegex.containsMatchIn(v)         -> "link"
            emailRegex.matches(v)               -> "email"
            phoneRegex.matches(v.replace(" ", "")) -> "phone"
            dateRegex.containsMatchIn(v)        -> "date"
            looksLikeAddress(v)                 -> "address"
            passRegex.matches(v)                -> "password"
            else                                -> "text"
        }
    }

    private fun looksLikeAddress(v: String): Boolean {
        if (v.length < 10 || !v.contains(" ")) return false
        val hasNumber = v.any { it.isDigit() }
        val hasCommaOrDot = v.contains(",") || v.contains(".")
        val streetWords = listOf("вул", "пров", "бул", "пл.", "street", "st.", "ave", "road", "rd.")
        return hasNumber && (hasCommaOrDot || streetWords.any { v.lowercase().contains(it) })
    }
}
