package com.snurdy.backend.global.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

@Configuration
class ClockConfig {
    /**
     * 시간 의존 로직(streak 계산, 토큰 TTL, 감사 기록 등)을 테스트에서 고정할 수 있도록 Clock 을 주입 가능하게 만든다.
     * 운영 환경은 UTC 를 사용한다. 타임존 기반 로직(Asia/Seoul 등)은 사용하는 쪽에서 `clock.withZone(APP_ZONE_ID)` 로 변환.
     */
    @Bean
    fun clock(): Clock = Clock.systemUTC()
}
