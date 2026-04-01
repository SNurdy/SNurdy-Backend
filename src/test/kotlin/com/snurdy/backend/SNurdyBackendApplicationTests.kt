package com.snurdy.backend

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@Disabled("환경 의존 설정이 많아 SeedDataIntegrityTest에서 통합 부팅 검증을 수행")
@SpringBootTest
internal class SNurdyBackendApplicationTests {
    @Test
    fun contextLoads() {
    }
}
