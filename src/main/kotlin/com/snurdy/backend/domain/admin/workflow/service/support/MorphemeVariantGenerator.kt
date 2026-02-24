package com.snurdy.backend.domain.admin.workflow.service.support

import com.snurdy.backend.domain.catalog.morpheme.entity.MorphemeEntity
import com.snurdy.backend.domain.catalog.morpheme.entity.MorphemeVariantEntity
import com.snurdy.backend.domain.catalog.morpheme.repository.MorphemeVariantRepository
import org.springframework.stereotype.Component

@Component
class MorphemeVariantGenerator(
    private val morphemeVariantRepository: MorphemeVariantRepository
) {

    fun regenerate(morpheme: MorphemeEntity) {
        morphemeVariantRepository.deleteByMorphemeId(morpheme.id!!)
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
            .flatMap { expandOptionalParentheses(it) }
            .forEach { token ->
                val variant = token.replace(Regex("[^a-z0-9]"), "").trim()
                if (variant.isBlank() || !seen.add(variant)) return@forEach
                val base = if (variant.length > 1 && variant.endsWith("o")) variant.dropLast(1) else variant
                results.add(VariantToken(variant = variant, base = base))
            }
        return results
    }

    private fun expandOptionalParentheses(token: String): List<String> {
        val regex = Regex("^(.*)\\(([^)]+)\\)(.*)$")
        val match = regex.matchEntire(token.trim()) ?: return listOf(token)
        val prefix = match.groupValues[1]
        val optional = match.groupValues[2]
        val suffix = match.groupValues[3]
        return listOf((prefix + suffix).trim(), (prefix + optional + suffix).trim())
    }
}

internal data class VariantToken(
    val variant: String,
    val base: String
)