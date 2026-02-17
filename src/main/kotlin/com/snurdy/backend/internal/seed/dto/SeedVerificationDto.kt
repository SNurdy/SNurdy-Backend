package com.snurdy.backend.internal.seed.dto

data class SeedSummaryResponse(
    val tableCounts: TableCounts,
    val glossaryDistribution: GlossaryDistribution
)

data class TableCounts(
    val glossaries: Long,
    val terms: Long,
    val glossaryTerms: Long,
    val morphemes: Long,
    val termMorphemes: Long
)

data class GlossaryDistribution(
    val mappedGlossaryIds: List<MappedGlossaryCount>,
    val unmappedGlossaries: List<GlossaryInfo>
)

data class MappedGlossaryCount(
    val glossaryId: Long,
    val mappedTermCount: Long
)

data class GlossaryInfo(
    val glossaryId: Long,
    val category: String,
    val title: String
)

data class SeedAnomaliesResponse(
    val glossariesWithoutMappings: List<GlossaryInfo>,
    val termsWithoutGlossary: List<TermInfo>,
    val termMorphemePositionAnomalies: List<TermMorphemePositionAnomaly>,
    val orphanMorphemes: OrphanMorphemeReport
)

data class TermInfo(
    val termId: Long,
    val spelling: String,
    val meaning: String
)

data class TermMorphemePositionAnomaly(
    val termId: Long,
    val minPosition: Int,
    val maxPosition: Int,
    val rowCount: Long,
    val distinctPositionCount: Long
)

data class OrphanMorphemeReport(
    val count: Long,
    val samples: List<MorphemeInfo>
)

data class MorphemeInfo(
    val morphemeId: Long,
    val spelling: String,
    val meaning: String
)
