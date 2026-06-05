package com.snurdy.backend.global.constants

import java.time.ZoneId

// 사용자 시점 날짜/시간 계산 (streak, dailyProgress, 자연주, 자연월 등) 의 기준 타임존
// 운영 DB 는 UTC, 응답 직렬화도 UTC 지만 사용자에게 노출되는 날짜 단위 집계는 Asia/Seoul 기준
val APP_ZONE_ID: ZoneId = ZoneId.of("Asia/Seoul")
