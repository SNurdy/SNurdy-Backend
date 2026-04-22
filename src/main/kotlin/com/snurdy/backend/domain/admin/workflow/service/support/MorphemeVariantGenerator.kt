package com.snurdy.backend.domain.admin.workflow.service.support

import com.snurdy.backend.domain.dictionary.morpheme.entity.MorphemeEntity
import com.snurdy.backend.domain.dictionary.morpheme.entity.MorphemeVariantEntity
import com.snurdy.backend.domain.dictionary.morpheme.repository.MorphemeVariantRepository
import com.snurdy.backend.global.constants.CommonError
import com.snurdy.backend.global.exception.ApiException
import org.springframework.stereotype.Component

@Component
class MorphemeVariantGenerator(
    private val morphemeVariantRepository: MorphemeVariantRepository
) {

    fun regenerate(morpheme: MorphemeEntity) {
        val morphemeId = morpheme.id ?: throw ApiException(CommonError.INTERNAL_ERROR)
        morphemeVariantRepository.deleteByMorphemeId(morphemeId)
        val tokens = parseSpelling(morpheme.spelling)
        if (tokens.isEmpty()) return
        morphemeVariantRepository.saveAll(
            tokens.map { MorphemeVariantEntity(morpheme = morpheme, variant = it.variant, base = it.base) }
        )
    }

    internal fun parseSpelling(spelling: String): List<VariantToken> {
        val seen = LinkedHashSet<String>()
        val results = mutableListOf<VariantToken>()

        spelling.split(";", ",")
            .map { it.trim().lowercase() }
            .filter { it.isNotBlank() }
            .forEach { segment ->
                val isOCombining = segment.contains(O_COMBINING_REGEX)

                expandOptionalParentheses(segment).forEach { expanded ->
                    val variant = expanded.replace(NON_ALPHANUMERIC_REGEX, "").trim()
                    if (variant.isBlank() || !seen.add(variant)) return@forEach

                    val base = when {
                        isOCombining && variant.length > 1 && variant.endsWith("o") -> variant.dropLast(1)
                        else -> variant
                    }
                    results.add(VariantToken(variant = variant, base = base))
                }
            }
        return results
    }

    private fun expandOptionalParentheses(token: String): List<String> {
        val match = OPTIONAL_PARENS_REGEX.matchEntire(token.trim()) ?: return listOf(token)
        val prefix = match.groupValues[1]
        val optional = match.groupValues[2]
        val suffix = match.groupValues[3]
        return listOf((prefix + suffix).trim(), (prefix + optional + suffix).trim())
    }

    companion object {
        private val O_COMBINING_REGEX = Regex("\\(o\\)")
        private val NON_ALPHANUMERIC_REGEX = Regex("[^a-z0-9]")
        private val OPTIONAL_PARENS_REGEX = Regex("^(.*)\\(([^)]+)\\)(.*)$")
    }
}

internal data class VariantToken(
    val variant: String,
    val base: String
)
