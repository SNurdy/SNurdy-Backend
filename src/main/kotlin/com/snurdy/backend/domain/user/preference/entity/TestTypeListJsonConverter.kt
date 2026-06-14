package com.snurdy.backend.domain.user.preference.entity

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.snurdy.backend.domain.test.entity.TestType
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

// user_preferences.default_test_types JSON 컬럼과 List<TestType> 간 매핑
// 빈 리스트 = 모달 강제 의미 (사용자가 아직 선택 안 함)
// DB NULL 도 빈 리스트로 흡수
@Converter(autoApply = false)
class TestTypeListJsonConverter : AttributeConverter<List<TestType>, String> {

    override fun convertToDatabaseColumn(attribute: List<TestType>?): String =
        MAPPER.writeValueAsString(attribute ?: emptyList<TestType>())

    override fun convertToEntityAttribute(dbData: String?): List<TestType> =
        if (dbData.isNullOrBlank()) emptyList() else MAPPER.readValue(dbData)

    companion object {
        private val MAPPER = jacksonObjectMapper()
    }
}
