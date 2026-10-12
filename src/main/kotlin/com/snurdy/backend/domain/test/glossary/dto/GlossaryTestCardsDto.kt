package com.snurdy.backend.domain.test.glossary.dto

import com.snurdy.backend.domain.dictionary.dto.OrderedMorpheme
import com.snurdy.backend.domain.test.common.dto.TestType
import com.snurdy.backend.domain.test.glossary.docs.GlossaryTestSwaggerDescription
import com.snurdy.backend.domain.test.glossary.docs.GlossaryTestSwaggerExample
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotEmpty

@Schema(description = GlossaryTestSwaggerDescription.GLOSSARY_TEST_CARDS_REQUEST)
data class GlossaryTestCardsRequest(
    @field:NotEmpty
    @field:Schema(
        description = GlossaryTestSwaggerDescription.GLOSSARY_TEST_CARDS_REQUEST_TERM_IDS,
        example = GlossaryTestSwaggerExample.GLOSSARY_TEST_CARDS_REQUEST_TERM_IDS
    )
    val termIds: List<Long>,

    @field:NotEmpty
    @field:Schema(
        description = GlossaryTestSwaggerDescription.GLOSSARY_TEST_CARDS_REQUEST_TEST_TYPES,
        example = GlossaryTestSwaggerExample.GLOSSARY_TEST_CARDS_REQUEST_TEST_TYPES
    )
    val testTypes: List<TestType>
)

@Schema(description = GlossaryTestSwaggerDescription.GLOSSARY_TEST_CARDS_RESPONSE)
data class GlossaryTestCardsResponse(
    @field:Schema(
        description = GlossaryTestSwaggerDescription.GLOSSARY_TEST_CARDS_RESPONSE_AVAILABLE_COUNTS,
        example = GlossaryTestSwaggerExample.GLOSSARY_TEST_CARDS_RESPONSE_AVAILABLE_COUNTS
    )
    val availableCounts: Map<TestType, Int>,

    @field:Schema(
        description = GlossaryTestSwaggerDescription.GLOSSARY_TEST_CARDS_RESPONSE_CARDS,
        example = GlossaryTestSwaggerExample.GLOSSARY_TEST_CARDS_RESPONSE_CARDS
    )
    val cards: List<GlossaryTestCard>
)

@Schema(description = GlossaryTestSwaggerDescription.GLOSSARY_TEST_CARD)
data class GlossaryTestCard(
    @field:Schema(
        description = GlossaryTestSwaggerDescription.GLOSSARY_TEST_CARD_TERM_ID,
        example = GlossaryTestSwaggerExample.GLOSSARY_TEST_CARD_TERM_ID
    )
    val termId: Long,
    @field:Schema(
        description = GlossaryTestSwaggerDescription.GLOSSARY_TEST_CARD_TEST_TYPE,
        example = GlossaryTestSwaggerExample.GLOSSARY_TEST_CARD_TEST_TYPE
    )
    val testType: TestType,
    @field:Schema(
        description = GlossaryTestSwaggerDescription.GLOSSARY_TEST_CARD_QUESTION,
        example = GlossaryTestSwaggerExample.GLOSSARY_TEST_CARD_QUESTION
    )
    val question: String,
    @field:Schema(
        description = GlossaryTestSwaggerDescription.GLOSSARY_TEST_CARD_ANSWER,
        example = GlossaryTestSwaggerExample.GLOSSARY_TEST_CARD_ANSWER
    )
    val answer: String,
    @field:Schema(
        description = GlossaryTestSwaggerDescription.GLOSSARY_TEST_CARD_ANSWERS,
        example = GlossaryTestSwaggerExample.GLOSSARY_TEST_CARD_ANSWERS
    )
    val answers: List<String>,
    @field:Schema(
        description = GlossaryTestSwaggerDescription.GLOSSARY_TEST_CARD_DISTRACTORS,
        example = GlossaryTestSwaggerExample.GLOSSARY_TEST_CARD_DISTRACTORS
    )
    val distractors: List<String>?,
    @field:Schema(
        description = GlossaryTestSwaggerDescription.GLOSSARY_TEST_CARD_SPELLING,
        example = GlossaryTestSwaggerExample.GLOSSARY_TEST_CARD_SPELLING
    )
    val spelling: String,
    @field:Schema(
        description = GlossaryTestSwaggerDescription.GLOSSARY_TEST_CARD_MEANING,
        example = GlossaryTestSwaggerExample.GLOSSARY_TEST_CARD_MEANING
    )
    val meaning: String,
    @field:Schema(
        description = GlossaryTestSwaggerDescription.GLOSSARY_TEST_CARD_ABBREVIATION,
        example = GlossaryTestSwaggerExample.GLOSSARY_TEST_CARD_ABBREVIATION
    )
    val abbreviation: String?,
    @field:Schema(
        description = GlossaryTestSwaggerDescription.GLOSSARY_TEST_CARD_EXPLANATION,
        example = GlossaryTestSwaggerExample.GLOSSARY_TEST_CARD_EXPLANATION
    )
    val explanation: String?,
    @field:Schema(
        description = GlossaryTestSwaggerDescription.GLOSSARY_TEST_CARD_BOOKMARKED,
        example = GlossaryTestSwaggerExample.GLOSSARY_TEST_CARD_BOOKMARKED
    )
    val bookmarked: Boolean,
    @field:Schema(
        description = GlossaryTestSwaggerDescription.GLOSSARY_TEST_CARD_MORPHEMES,
        example = GlossaryTestSwaggerExample.GLOSSARY_TEST_CARD_MORPHEMES
    )
    val morphemes: List<OrderedMorpheme>
)