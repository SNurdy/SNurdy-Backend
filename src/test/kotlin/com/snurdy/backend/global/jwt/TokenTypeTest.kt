package com.snurdy.backend.global.jwt

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class TokenTypeTest {

    @Test
    fun `대문자 ACCESS 인식`() {
        assertEquals(TokenType.ACCESS, TokenType.fromClaim("ACCESS"))
    }

    @Test
    fun `대문자 REFRESH 인식`() {
        assertEquals(TokenType.REFRESH, TokenType.fromClaim("REFRESH"))
    }

    @Test
    fun `대소문자 무관하게 admin claim도 ADMIN으로 매핑된다 (구버전 토큰 호환)`() {
        assertEquals(TokenType.ADMIN, TokenType.fromClaim("admin"))
        assertEquals(TokenType.ADMIN, TokenType.fromClaim("ADMIN"))
        assertEquals(TokenType.ADMIN, TokenType.fromClaim("Admin"))
    }

    @Test
    fun `알 수 없는 type 또는 null이면 null 반환`() {
        assertNull(TokenType.fromClaim(null))
        assertNull(TokenType.fromClaim(""))
        assertNull(TokenType.fromClaim("unknown"))
    }
}
