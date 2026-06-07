package com.snurdy.backend.domain.test.dto

import com.snurdy.backend.domain.test.entity.TestType
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull

@Schema(description = "다지선다 오답 생성 요청")
data class DistractorsRequest(
    @field:NotNull
    @field:Schema(description = "테스트 유형, MC_PICK_TERM / MC_PICK_MEANING / MC_PICK_ABBR 만 허용", example = "MC_PICK_TERM")
    val testType: TestType,

    @field:NotEmpty
    @field:Schema(description = "보기를 만들 단어 id 목록, cards 응답의 termId 를 그대로 전달")
    val termIds: List<Long>
)

@Schema(description = "다지선다 오답 생성 응답")
data class DistractorsResponse(
    @field:Schema(
        description = "termId 별 오답 보기 3개. 정답은 클라이언트가 cards 응답에서 derive 해 추가하고 shuffle",
        example = """{"101":["Hemoglobin","Hematocrit","Erythrocyte"],"302":["Heart Rate","Blood Pressure","Body Temperature"],"183":["Magnetic Resonance Imaging","Computed Tomography","Positron Emission Tomography"]}"""
    )
    val distractorsByTermId: Map<Long, List<String>>
)
