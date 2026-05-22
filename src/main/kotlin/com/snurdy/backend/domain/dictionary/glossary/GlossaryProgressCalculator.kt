package com.snurdy.backend.domain.dictionary.glossary

// 단어장 학습 진척률(0~100, 소수점 1자리) 과 그로부터 도출되는 0~5 레벨을 계산한다.
// 단어장 응답에 부착되는 사용자 시점 지표라 dictionary/glossary 패키지에 위치.
object GlossaryProgressCalculator {

    fun progressRate(learnedCount: Int, totalTermCount: Long): Double {
        if (totalTermCount <= 0L) return 0.0
        return (learnedCount.toDouble() / totalTermCount.toDouble() * 1000.0).toInt() / 10.0
    }

    fun levelFromRate(progressRate: Double): Int = when {
        progressRate >= 100.0 -> 5
        progressRate >= 80.0 -> 4
        progressRate >= 60.0 -> 3
        progressRate >= 40.0 -> 2
        progressRate >= 20.0 -> 1
        else -> 0
    }

    fun levelFromCounts(learnedCount: Int, totalTermCount: Long): Int =
        levelFromRate(progressRate(learnedCount, totalTermCount))
}
