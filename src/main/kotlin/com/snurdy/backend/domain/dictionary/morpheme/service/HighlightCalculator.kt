package com.snurdy.backend.domain.dictionary.morpheme.service

import com.snurdy.backend.domain.dictionary.morpheme.repository.MorphemeVariantProjection
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class HighlightCalculator {

    private val log = LoggerFactory.getLogger(HighlightCalculator::class.java)

    /**
     * 용어(termSpelling) 내에서 형태소의 하이라이팅 범위를 계산합니다.
     *
     * Fallback 전략:
     *   1순위 — variant(괄호 확장형)로 매칭 시도
     *   2순위 — base(괄호 제거형)로 fallback 매칭
     *   실패  — null 반환 + 로깅
     *
     * @param termSpelling  용어 철자 (예: "hepatocyte")
     * @param variants      해당 형태소의 variant/base 목록
     * @return HighlightRange 또는 null
     */
    fun calculate(
        termSpelling: String,
        variants: List<MorphemeVariantProjection>
    ): HighlightRange? {
        val lowered = termSpelling.lowercase()

        // 1순위: variant(확장형)로 매칭 — 가장 긴 variant부터 시도
        for (v in variants.sortedByDescending { it.variant.length }) {
            val idx = lowered.indexOf(v.variant)
            if (idx >= 0) {
                return HighlightRange(
                    start = idx,
                    length = v.variant.length
                )
            }
        }

        // 2순위: base(기본형)로 fallback — 가장 긴 base부터 시도
        for (v in variants.sortedByDescending { it.base.length }) {
            val idx = lowered.indexOf(v.base)
            if (idx >= 0) {
                return HighlightRange(
                    start = idx,
                    length = v.base.length
                )
            }
        }

        // 매칭 실패 — anomaly 로깅
        log.warn(
            "[하이라이트 이상] 매칭 실패 — term='{}', variants={}",
            termSpelling,
            variants.map { "${it.variant}/${it.base}" }
        )
        return null
    }
}

data class HighlightRange(
    val start: Int,
    val length: Int
)