package com.snurdy.backend.domain.dictionary.glossary

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class LevelCalculatorTest {

    @ParameterizedTest
    @CsvSource(
        "0.0,    0",
        "19.9,   0",
        "20.0,   1",
        "39.9,   1",
        "40.0,   2",
        "59.9,   2",
        "60.0,   3",
        "79.9,   3",
        "80.0,   4",
        "99.9,   4",
        "100.0,  5"
    )
    fun `progressRate 에 따라 0~5 레벨로 매핑된다`(rate: Double, expected: Int) {
        assertThat(LevelCalculator.fromProgressRate(rate)).isEqualTo(expected)
    }

    @Test
    fun `totalTermCount 가 0 이면 분모 보호로 level 0 을 반환한다`() {
        assertThat(LevelCalculator.fromCounts(learnedCount = 5, totalTermCount = 0L)).isEqualTo(0)
    }

    @Test
    fun `learnedCount 와 totalTermCount 로 직접 계산한 결과가 progressRate 기반과 일치한다`() {
        // 학습 50 / 전체 100 = 50.0 → level 2
        assertThat(LevelCalculator.fromCounts(learnedCount = 50, totalTermCount = 100L)).isEqualTo(2)
        // 학습 100 / 전체 100 = 100.0 → level 5
        assertThat(LevelCalculator.fromCounts(learnedCount = 100, totalTermCount = 100L)).isEqualTo(5)
    }
}
