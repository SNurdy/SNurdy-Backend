package com.snurdy.backend.domain.admin.workflow.service.model

data class ValidationEntry(
    val code: String,
    val message: String
)

data class ValidationResult(
    val errors: List<ValidationEntry> = emptyList(),
    val warnings: List<ValidationEntry> = emptyList()
) {
    val passed: Boolean get() = errors.isEmpty()

    fun errorCodes(): String? =
        errors.takeIf { it.isNotEmpty() }?.joinToString(";") { it.code }

    fun errorMessages(): String? =
        errors.takeIf { it.isNotEmpty() }?.joinToString("; ") { it.message }

    fun warningMessages(): String? =
        warnings.takeIf { it.isNotEmpty() }?.joinToString("; ") { it.message }
}

class ValidationCollector {
    private val errors = mutableListOf<ValidationEntry>()
    private val warnings = mutableListOf<ValidationEntry>()

    fun error(code: ValidationCode, vararg args: Any?) {
        errors.add(ValidationEntry(code = code.code, message = code.format(*args)))
    }

    fun warn(code: ValidationCode, vararg args: Any?) {
        warnings.add(ValidationEntry(code = code.code, message = code.format(*args)))
    }

    fun build() = ValidationResult(errors = errors.toList(), warnings = warnings.toList())

    fun hasErrors(): Boolean = errors.isNotEmpty()
}