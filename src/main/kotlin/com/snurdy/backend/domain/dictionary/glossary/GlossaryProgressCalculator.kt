package com.snurdy.backend.domain.dictionary.glossary

// 단어장 학습 진척률 0 부터 100 까지 소수점 1자리와 그로부터 도출되는 1 부터 5 까지 레벨을 계산한다
// 단어장 응답에 부착되는 사용자 시점 지표라 dictionary glossary 패키지에 위치
object GlossaryProgressCalculator {

    fun progressRate(learnedCount: Int, totalTermCount: Long): Double {
        if (totalTermCount <= 0L) return 0.0
        return (learnedCount.toDouble() / totalTermCount.toDouble() * 1000.0).toInt() / 10.0
    }

    // 진척률 구간별 레벨
    // 0 부터 20 까지 Lv1
    // 21 부터 40 까지 Lv2
    // 41 부터 60 까지 Lv3
    // 61 부터 80 까지 Lv4
    // 81 부터 100 까지 Lv5
    fun levelFromRate(progressRate: Double): Int = when {
        progressRate >= 81.0 -> 5
        progressRate >= 61.0 -> 4
        progressRate >= 41.0 -> 3
        progressRate >= 21.0 -> 2
        else -> 1
    }

    fun levelFromCounts(learnedCount: Int, totalTermCount: Long): Int =
        levelFromRate(progressRate(learnedCount, totalTermCount))
}
