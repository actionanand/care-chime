package com.actionanand.carechime.app.domain.validation

import java.math.BigDecimal
import java.net.URI
import java.time.LocalDate

sealed interface FormResult<out T> {
    data class Valid<out T>(val value: T) : FormResult<T>
    data class Invalid(val errors: Map<String, String>) : FormResult<Nothing>
}

fun <T> FormResult<T>.errorsOrEmpty(): Map<String, String> =
    (this as? FormResult.Invalid)?.errors ?: emptyMap()

/** Collects user-friendly, field-keyed messages. Messages never echo the entered medical content. */
class FieldValidator {
    private val errors = linkedMapOf<String, String>()

    fun error(field: String, message: String) {
        errors.putIfAbsent(field, message)
    }

    fun hasError(field: String): Boolean = field in errors

    fun required(field: String, value: String, label: String, maxLength: Int): String {
        val clean = value.trim()
        when {
            clean.isEmpty() -> error(field, "$label is required.")
            clean.length > maxLength -> error(field, "$label must be $maxLength characters or fewer.")
        }
        return clean
    }

    fun optional(field: String, value: String, label: String, maxLength: Int): String? {
        val clean = value.trim()
        if (clean.length > maxLength) error(field, "$label must be $maxLength characters or fewer.")
        return clean.ifEmpty { null }
    }

    fun optionalDate(
        field: String,
        value: String,
        label: String,
        today: LocalDate,
        allowFuture: Boolean = false,
    ): LocalDate? {
        val clean = value.trim()
        if (clean.isEmpty()) return null
        val date = parseDate(clean)
        when {
            date == null -> error(field, "Enter $label as YYYY-MM-DD.")
            !allowFuture && date.isAfter(today) -> error(field, "$label cannot be in the future.")
            date.isBefore(EARLIEST_DATE) -> error(field, "$label is too far in the past.")
            date.isAfter(LATEST_DATE) -> error(field, "$label is too far in the future.")
        }
        return date
    }

    fun requiredDate(
        field: String,
        value: String,
        label: String,
        today: LocalDate,
        allowFuture: Boolean = false,
    ): LocalDate? {
        if (value.isBlank()) {
            error(field, "$label is required.")
            return null
        }
        return optionalDate(field, value, label, today, allowFuture)
    }

    fun <T> result(build: () -> T): FormResult<T> =
        if (errors.isEmpty()) FormResult.Valid(build()) else FormResult.Invalid(errors.toMap())

    companion object {
        val EARLIEST_DATE: LocalDate = LocalDate.of(1880, 1, 1)
        val LATEST_DATE: LocalDate = LocalDate.of(2200, 12, 31)

        fun parseDate(text: String): LocalDate? =
            runCatching { LocalDate.parse(text.trim()) }.getOrNull()
    }
}

object MapsUrlValidator {
    private val googleHost = Regex("""^(www\.)?google\.[a-z]{2,3}(\.[a-z]{2})?$""")

    /** Accepts only HTTPS Google Maps links; rejects credentials, other hosts and malformed URLs. */
    fun isValid(url: String): Boolean {
        val uri = runCatching { URI(url.trim()) }.getOrNull() ?: return false
        if (!uri.scheme.equals("https", ignoreCase = true)) return false
        if (uri.rawUserInfo != null) return false
        if (uri.port != -1 && uri.port != 443) return false
        val host = uri.host?.lowercase() ?: return false
        val path = uri.rawPath.orEmpty()
        return when {
            host == "maps.google.com" || host == "maps.app.goo.gl" -> true
            host == "goo.gl" -> path.startsWith("/maps")
            googleHost.matches(host) -> path == "/maps" || path.startsWith("/maps/")
            else -> false
        }
    }
}

object DoseAmountParser {
    private val MAX = BigDecimal("100000")

    /** Returns normalised plain decimal text, or null when not a positive number with ≤3 decimals. */
    fun normalise(text: String): String? {
        val clean = text.trim().replace(',', '.')
        if (clean.isEmpty() || !clean.matches(Regex("""\d+(\.\d+)?"""))) return null
        val value = runCatching { BigDecimal(clean) }.getOrNull() ?: return null
        if (value.signum() <= 0 || value > MAX) return null
        val stripped = value.stripTrailingZeros()
        if (stripped.scale() > 3) return null
        return stripped.toPlainString()
    }
}
