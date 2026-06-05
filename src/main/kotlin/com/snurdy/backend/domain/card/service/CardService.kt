package com.snurdy.backend.domain.card.service

import com.snurdy.backend.domain.card.dto.Card
import com.snurdy.backend.domain.card.dto.CardsRequest
import com.snurdy.backend.domain.card.dto.CardsResponse
import com.snurdy.backend.domain.dictionary.glossary.repository.GlossaryRepository
import com.snurdy.backend.domain.dictionary.glossary.repository.GlossaryTermRepository
import com.snurdy.backend.domain.test.entity.TestType
import com.snurdy.backend.global.constants.DictionaryError
import com.snurdy.backend.global.constants.TestError
import com.snurdy.backend.global.exception.ApiException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class CardService(
    private val glossaryRepository: GlossaryRepository,
    private val glossaryTermRepository: GlossaryTermRepository
) {

    @Transactional(readOnly = true)
    fun listCards(userId: UUID, request: CardsRequest): CardsResponse {
        if (!glossaryRepository.existsById(request.glossaryId)) {
            throw ApiException(DictionaryError.GLOSSARY_NOT_FOUND)
        }

        val requireAbbreviation = request.testType.requiresAbbreviation()
        val sampled = glossaryTermRepository.sampleCardTerms(
            userId = userId,
            glossaryId = request.glossaryId,
            requireAbbreviation = requireAbbreviation,
            count = request.count
        )

        if (sampled.isEmpty()) throw ApiException(TestError.TEST_TYPE_NOT_AVAILABLE)

        val cards = sampled.map {
            Card(
                termId = it.termId,
                spelling = it.spelling,
                meaning = it.meaning,
                abbreviation = it.abbreviation
            )
        }
        return CardsResponse(glossaryId = request.glossaryId, cards = cards)
    }
}

private fun TestType.requiresAbbreviation(): Boolean = when (this) {
    TestType.WRITE_ABBREVIATION, TestType.MC_PICK_ABBR -> true
    else -> false
}
