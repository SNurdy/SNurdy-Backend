package com.snurdy.backend.domain.user.preference.dto

import com.snurdy.backend.domain.test.entity.TestType
import com.snurdy.backend.domain.user.preference.entity.UserPreferenceEntity
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

@Schema(description = "사용자 학습 옵션 조회 응답")
data class UserPreferenceResponse(
    @field:Schema(
        description = "사용자가 마지막으로 선택한 테스트 유형 리스트. 빈 리스트면 다음 진입 시 모달 강제",
        example = "[\"WRITE_TERM_FROM_MEANING\", \"MC_MEANING_FROM_TERM\", \"MC_TERM_FROM_ABBREVIATION\", \"MC_TERM_FROM_MEANING\"]"
    )
    val defaultTestTypes: List<TestType>,
    @field:Schema(description = "마이페이지 다마고치 영역에 표시할 선택 단어장 id", example = "6")
    val selectedGlossaryId: Long,
    @field:Schema(description = "한 세션 학습 카드 수 기본값 (10/20/30/40/50)", example = "10")
    val defaultCardCount: Int
)

fun UserPreferenceEntity.toResponse() = UserPreferenceResponse(
    defaultTestTypes = defaultTestTypes,
    selectedGlossaryId = selectedGlossaryId,
    defaultCardCount = defaultCardCount
)

@Schema(description = "기본 테스트 유형 변경 요청. 빈 리스트면 다음 진입에서 선택 모달 강제")
data class UpdateDefaultTestTypesRequest(
    @field:NotNull
    @field:Schema(
        description = "사용자가 마지막으로 선택한 테스트 유형 리스트. 빈 리스트 허용",
        example = "[\"WRITE_TERM_FROM_MEANING\", \"MC_MEANING_FROM_TERM\", \"MC_TERM_FROM_ABBREVIATION\", \"MC_TERM_FROM_MEANING\"]"
    )
    val defaultTestTypes: List<TestType>?
)

@Schema(description = "기본 테스트 유형 변경 응답")
data class UpdateDefaultTestTypesResponse(
    @field:Schema(
        description = "변경된 테스트 유형 리스트. 빈 리스트면 다음 진입 시 모달 강제",
        example = "[\"WRITE_TERM_FROM_MEANING\", \"MC_MEANING_FROM_TERM\", \"MC_TERM_FROM_ABBREVIATION\", \"MC_TERM_FROM_MEANING\"]"
    )
    val defaultTestTypes: List<TestType>
)

@Schema(description = "선택된 단어장 변경 요청 (마이페이지 다마고치 영역에 표시할 단어장)")
data class UpdateSelectedGlossaryRequest(
    @field:NotNull
    @field:Schema(description = "선택할 단어장 id (기본 카탈로그 단어장만 허용)", example = "6")
    val glossaryId: Long?
)

@Schema(description = "선택된 단어장 변경 응답")
data class UpdateSelectedGlossaryResponse(
    @field:Schema(description = "저장된 선택 단어장 id", example = "6")
    val selectedGlossaryId: Long
)

@Schema(description = "한 세션 학습 카드 수 변경 요청")
data class UpdateDefaultCardCountRequest(
    @field:NotNull
    @field:Schema(description = "10/20/30/40/50 중 하나", example = "30")
    val count: Int?
)

@Schema(description = "한 세션 학습 카드 수 변경 응답")
data class UpdateDefaultCardCountResponse(
    @field:Schema(description = "저장된 카드 수", example = "30")
    val defaultCardCount: Int
)

fun UserPreferenceEntity.toDefaultTestTypesResponse() = UpdateDefaultTestTypesResponse(
    defaultTestTypes = defaultTestTypes
)

fun UserPreferenceEntity.toSelectedGlossaryResponse() = UpdateSelectedGlossaryResponse(
    selectedGlossaryId = selectedGlossaryId
)

fun UserPreferenceEntity.toDefaultCardCountResponse() = UpdateDefaultCardCountResponse(
    defaultCardCount = defaultCardCount
)
