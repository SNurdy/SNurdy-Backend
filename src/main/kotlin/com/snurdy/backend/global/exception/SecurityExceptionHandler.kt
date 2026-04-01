package com.snurdy.backend.global.exception

import com.fasterxml.jackson.databind.ObjectMapper
import com.snurdy.backend.global.constants.CommonError
import com.snurdy.backend.global.constants.ErrorCode
import com.snurdy.backend.global.dto.response.ApiResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.stereotype.Component

@Component
class SecurityExceptionHandler(
    private val objectMapper: ObjectMapper
) : AuthenticationEntryPoint, AccessDeniedHandler {

    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException
    ) {
        writeErrorResponse(response, CommonError.UNAUTHORIZED)
    }

    override fun handle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        accessDeniedException: AccessDeniedException
    ) {
        writeErrorResponse(response, CommonError.FORBIDDEN)
    }

    private fun writeErrorResponse(response: HttpServletResponse, error: ErrorCode) {
        response.contentType = "application/json;charset=UTF-8"
        response.status = error.status.value()
        response.writer.write(objectMapper.writeValueAsString(ApiResponse.fail(error)))
    }
}