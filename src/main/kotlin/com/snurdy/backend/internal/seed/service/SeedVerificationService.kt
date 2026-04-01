package com.snurdy.backend.internal.seed.service

import com.snurdy.backend.domain.dictionary.glossary.repository.GlossaryRepository
import com.snurdy.backend.domain.dictionary.glossary.repository.GlossaryTermRepository
import com.snurdy.backend.domain.dictionary.morpheme.repository.MorphemeRepository
import com.snurdy.backend.domain.dictionary.term.repository.TermMorphemeRepository
import com.snurdy.backend.domain.dictionary.term.repository.TermRepository
import com.snurdy.backend.internal.seed.dto.GlossaryDistribution
import com.snurdy.backend.internal.seed.dto.GlossaryInfo
import com.snurdy.backend.internal.seed.dto.MappedGlossaryCount
import com.snurdy.backend.internal.seed.dto.MorphemeInfo
import com.snurdy.backend.internal.seed.dto.OrphanMorphemeReport
import com.snurdy.backend.internal.seed.dto.SeedAnomaliesResponse
import com.snurdy.backend.internal.seed.dto.SeedSummaryResponse
import com.snurdy.backend.internal.seed.dto.TableCounts
import com.snurdy.backend.internal.seed.dto.TermInfo
import com.snurdy.backend.internal.seed.dto.TermMorphemePositionAnomaly
import org.springframework.stereotype.Service

@Service
class SeedVerificationService(
    private val glossaryRepository: GlossaryRepository,
    private val glossaryTermRepository: GlossaryTermRepository,
    private val termRepository: TermRepository,
    private val morphemeRepository: MorphemeRepository,
    private val termMorphemeRepository: TermMorphemeRepository
) {

    fun getSummary(): SeedSummaryResponse {
        val glossaryCounts = glossaryRepository.findGlossaryTermCounts()
        val mappedDistribution = glossaryTermRepository.findGlossaryIdDistribution()

        return SeedSummaryResponse(
            tableCounts = TableCounts(
                glossaries = glossaryRepository.count(),
                terms = termRepository.count(),
                glossaryTerms = glossaryTermRepository.count(),
                morphemes = morphemeRepository.count(),
                termMorphemes = termMorphemeRepository.count()
            ),
            glossaryDistribution = GlossaryDistribution(
                mappedGlossaryIds = mappedDistribution.map {
                    MappedGlossaryCount(
                        glossaryId = it.glossaryId,
                        mappedTermCount = it.mappedTermCount
                    )
                },
                unmappedGlossaries = glossaryCounts
                    .filter { it.termCount == 0L }
                    .map {
                        GlossaryInfo(
                            glossaryId = it.glossaryId,
                            category = it.category,
                            title = it.title
                        )
                    }
            )
        )
    }

    fun getAnomalies(sampleLimit: Int = DEFAULT_SAMPLE_LIMIT): SeedAnomaliesResponse {
        val glossaryCounts = glossaryRepository.findGlossaryTermCounts()

        return SeedAnomaliesResponse(
            glossariesWithoutMappings = glossaryCounts
                .filter { it.termCount == 0L }
                .map {
                    GlossaryInfo(
                        glossaryId = it.glossaryId,
                        category = it.category,
                        title = it.title
                    )
                },
            termsWithoutGlossary = glossaryTermRepository.findTermsWithoutGlossary(sampleLimit)
                .map {
                    TermInfo(
                        termId = it.termId,
                        spelling = it.spelling,
                        meaning = it.meaning
                    )
                },
            termMorphemePositionAnomalies = termMorphemeRepository.findPositionAnomalies()
                .map {
                    TermMorphemePositionAnomaly(
                        termId = it.termId,
                        minPosition = it.minPosition,
                        maxPosition = it.maxPosition,
                        rowCount = it.rowCount,
                        distinctPositionCount = it.distinctPositionCount
                    )
                },
            orphanMorphemes = OrphanMorphemeReport(
                count = morphemeRepository.countOrphanMorphemes(),
                samples = morphemeRepository.findOrphanMorphemeSamples(sampleLimit)
                    .map {
                        MorphemeInfo(
                            morphemeId = it.morphemeId,
                            spelling = it.spelling,
                            meaning = it.meaning
                        )
                    }
            )
        )
    }

    companion object {
        private const val DEFAULT_SAMPLE_LIMIT = 20
    }
}
