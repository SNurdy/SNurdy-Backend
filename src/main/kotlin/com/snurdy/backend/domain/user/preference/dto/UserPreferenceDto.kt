package com.snurdy.backend.domain.user.preference.dto

import com.snurdy.backend.domain.test.entity.TestType
import com.snurdy.backend.domain.user.preference.entity.UserPreferenceEntity
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

@Schema(description = "사용자 학습 옵션 조회 응답")
data class UserPreferenceResponse(
    @field:Schema(description = "사용자 기본 테스트 유형. null 이면 다음 테스트 진입 시 모달 강제", example = "MEANING_TO_TERM")
    val defaultTestType: TestType?,
    @field:Schema(description = "마이페이지 다마고치 영역에 표시할 선택 단어장 id", example = "6")
    val selectedGlossaryId: Long
)

fun UserPreferenceEntity.toResponse() = UserPreferenceResponse(
    defaultTestType = defaultTestType,
    selectedGlossaryId = selectedGlossaryId
)

@Schema(description = "기본 테스트 유형 변경 요청, null 전달 시 다음 진입에서 선택 모달 강제")
data class UpdateDefaultTestTypeRequest(
    @field:Schema(description = "사용자 기본 테스트 유형", example = "MEANING_TO_TERM")
    val defaultTestType: TestType?
)

@Schema(description = "기본 테스트 유형 변경 응답")
data class UpdateDefaultTestTypeResponse(
    @field:Schema(description = "변경된 기본 테스트 유형. null 이면 다음 진입 시 모달 강제", example = "MEANING_TO_TERM")
    val defaultTestType: TestType?
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

fun UserPreferenceEntity.toDefaultTestTypeResponse() = UpdateDefaultTestTypeResponse(
    defaultTestType = defaultTestType
)

fun UserPreferenceEntity.toSelectedGlossaryResponse() = UpdateSelectedGlossaryResponse(
    selectedGlossaryId = selectedGlossaryId
)
