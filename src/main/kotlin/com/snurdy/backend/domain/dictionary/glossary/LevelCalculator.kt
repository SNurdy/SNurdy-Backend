package com.snurdy.backend.domain.dictionary.glossary

// 단어장 학습률(0~100) 을 0~5 레벨로 매핑한다.
// 단어장에 부착되는 사용자 시점 지표라 dictionary/glossary 패키지에 위치.
object LevelCalculator {

    fun fromProgressRate(progressRate: Double): Int = when {
        progressRate >= 100.0 -> 5
        progressRate >= 80.0 -> 4
        progressRate >= 60.0 -> 3
        progressRate >= 40.0 -> 2
        progressRate >= 20.0 -> 1
        else -> 0
    }

    fun fromCounts(learnedCount: Int, totalTermCount: Long): Int {
        if (totalTermCount <= 0L) return 0
        val rate = learnedCount.toDouble() / totalTermCount.toDouble() * 100.0
        return fromProgressRate(rate)
    }
}
