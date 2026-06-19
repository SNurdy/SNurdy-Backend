package com.snurdy.backend.global.jwt

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class JwtPropertiesTest {

    @Test
    fun `refreshTokenValidityDays는 밀리초 값을 일 단위로 변환한다`() {
        val props = JwtProperties(
            secretKey = "secret",
            accessTokenExpiration = 3_600_000L,
            refreshTokenExpiration = 2_592_000_000L
        )
        assertEquals(30L, props.refreshTokenValidityDays)
    }

    @Test
    fun `하루 미만은 0일로 버린다`() {
        val props = JwtProperties(
            secretKey = "secret",
            accessTokenExpiration = 0L,
            refreshTokenExpiration = 3_600_000L
        )
        assertEquals(0L, props.refreshTokenValidityDays)
    }
}
