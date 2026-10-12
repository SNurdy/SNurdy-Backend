package com.snurdy.backend.domain.study.glossary.dto

import com.snurdy.backend.domain.dictionary.dto.OrderedMorpheme
import com.snurdy.backend.domain.dictionary.glossary.repository.SimpleTermProjection
import com.snurdy.backend.domain.study.glossary.docs.GlossaryStudySwaggerDescription
import com.snurdy.backend.domain.study.glossary.docs.GlossaryStudySwaggerExample
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull

@Schema(description = GlossaryStudySwaggerDescription.GLOSSARY_STUDY_CARDS_REQUEST)
data class GlossaryStudyCardsRequest(
    @field:NotNull
    @field:Min(1)
    @field:Max(50)
    @field:Schema(
        description = GlossaryStudySwaggerDescription.GLOSSARY_STUDY_CARDS_REQUEST_COUNT,
        example = GlossaryStudySwaggerExample.GLOSSARY_STUDY_CARDS_REQUEST_COUNT
    )
    val count: Int
)

@Schema(description = GlossaryStudySwaggerDescription.GLOSSARY_STUDY_CARDS_RESPONSE)
data class GlossaryStudyCardsResponse(
    @field:Schema(
        description = GlossaryStudySwaggerDescription.GLOSSARY_STUDY_CARDS_RESPONSE_REQUESTED_COUNT,
        example = GlossaryStudySwaggerExample.GLOSSARY_STUDY_CARDS_RESPONSE_REQUESTED_COUNT
    )
    val requestedCount: Int,

    @field:Schema(description = GlossaryStudySwaggerDescription.GLOSSARY_STUDY_CARDS_RESPONSE_TERMS)
    val terms: List<GlossaryStudyTermItem>
)

@Schema(description = GlossaryStudySwaggerDescription.GLOSSARY_STUDY_TERM_ITEM)
data class GlossaryStudyTermItem(
    @field:Schema(
        description = GlossaryStudySwaggerDescription.GLOSSARY_STUDY_TERM_TERM_ID,
        example = GlossaryStudySwaggerExample.GLOSSARY_STUDY_TERM_TERM_ID
    )
    val termId: Long,
    @field:Schema(
        description = GlossaryStudySwaggerDescription.GLOSSARY_STUDY_TERM_ITEM_SPELLING,
        example = GlossaryStudySwaggerExample.GLOSSARY_STUDY_TERM_ITEM_SPELLING
    )
    val spelling: String,
    @field:Schema(
        description = GlossaryStudySwaggerDescription.GLOSSARY_STUDY_TERM_MEANING,
        example = GlossaryStudySwaggerExample.GLOSSARY_STUDY_TERM_MEANING
    )
    val meaning: String,
    @field:Schema(
        description = GlossaryStudySwaggerDescription.GLOSSARY_STUDY_TERM_ABBREVIATION,
        example = GlossaryStudySwaggerExample.GLOSSARY_STUDY_TERM_ABBREVIATION
    )
    val abbreviation: String?,
    @field:Schema(
        description = GlossaryStudySwaggerDescription.GLOSSARY_STUDY_TERM_EXPLANATION,
        example = GlossaryStudySwaggerExample.GLOSSARY_STUDY_TERM_EXPLANATION
    )
    val explanation: String?,
    @field:Schema(
        description = GlossaryStudySwaggerDescription.GLOSSARY_STUDY_TERM_BOOKMARKED,
        example = GlossaryStudySwaggerExample.GLOSSARY_STUDY_TERM_BOOKMARKED
    )
    val bookmarked: Boolean,
    @field:Schema(
        description = GlossaryStudySwaggerDescription.GLOSSARY_STUDY_TERM_ITEM_MORPHEMES,
        example = GlossaryStudySwaggerExample.GLOSSARY_STUDY_TERM_ITEM_MORPHEMES
    )
    val morphemes: List<OrderedMorpheme>
)

fun SimpleTermProjection.toStudyTerm(
    bookmarked: Boolean,
    morphemes: List<OrderedMorpheme>
): GlossaryStudyTermItem  = GlossaryStudyTermItem (
    termId = termId,
    spelling = spelling,
    meaning = meaning,
    abbreviation = abbreviation,
    explanation = explanation,
    bookmarked = bookmarked,
    morphemes = morphemes
)