package com.snurdy.backend.global.exception

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.snurdy.backend.global.constants.AuthError
import com.snurdy.backend.global.constants.CommonError
import feign.FeignException
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.JwtException
import jakarta.validation.ConstraintViolationException
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.method.annotation.HandlerMethodValidationException

fun Throwable.toApiException(): ApiException = when (this) {
    is ApiException -> this

    // JWT - 기본 메시지 사용
    is ExpiredJwtException -> ApiException(AuthError.EXPIRED_TOKEN, cause = this)
    is JwtException -> ApiException(AuthError.INVALID_TOKEN, cause = this)

    // Feign - 기본 메시지 사용
    is FeignException -> this.toFeignApiException()

    // 요청 파싱 - 상세 정보 필요
    is HttpMessageNotReadableException -> this.toParseApiException()
    is MethodArgumentNotValidException -> this.toValidationApiException()
    is HandlerMethodValidationException -> this.toMethodValidationApiException()
    is ConstraintViolationException -> this.toConstraintViolationApiException()

    // HTTP 메서드 - 어떤 메서드인지 알려줌
    is HttpRequestMethodNotSupportedException ->
        ApiException(CommonError.METHOD_NOT_ALLOWED, "${CommonError.METHOD_NOT_ALLOWED.message}: ${this.method}")

    // 파라미터 누락 - 어떤 파라미터인지 알려줌
    is MissingServletRequestParameterException ->
        ApiException(CommonError.BAD_REQUEST, "필수 파라미터 누락: ${this.parameterName}")

    // 그 외 - 기본 메시지 사용
    else -> ApiException(CommonError.INTERNAL_ERROR, cause = this)
}

private fun FeignException.toFeignApiException(): ApiException {
    val error = when {
        request()?.url()?.contains("kakao") == true -> AuthError.KAKAO_API_ERROR
        request()?.url()?.contains("apple") == true -> AuthError.APPLE_API_ERROR
        request()?.url()?.contains("google") == true -> AuthError.GOOGLE_API_ERROR
        else -> CommonError.INTERNAL_ERROR
    }
    // 기본 메시지 사용
    return ApiException(error, cause = this)
}

private fun HttpMessageNotReadableException.toParseApiException(): ApiException {
    // 상세 정보 필요 - 어떤 필드가 문제인지
    val detailMessage = when (val cause = this.cause) {
        is MismatchedInputException -> {
            val fieldName = cause.path.lastOrNull()?.fieldName ?: "unknown"
            if (cause.message?.contains("missing", ignoreCase = true) == true) {
                "필수 필드 누락: $fieldName"
            } else {
                "잘못된 형식: $fieldName"
            }
        }
        is JsonParseException -> "JSON 문법 오류"
        else -> null  // null이면 기본 메시지 사용
    }

    return ApiException(CommonError.BAD_REQUEST, detailMessage)
}

private fun MethodArgumentNotValidException.toValidationApiException(): ApiException {
    // 상세 정보 필요 - 어떤 필드가 검증 실패인지
    val firstError = bindingResult.fieldErrors.firstOrNull()
    val detailMessage = firstError?.let { "${it.field}: ${it.defaultMessage}" }

    return ApiException(CommonError.BAD_REQUEST, detailMessage)
}

private fun HandlerMethodValidationException.toMethodValidationApiException(): ApiException {
    val detailMessage = parameterValidationResults
        .asSequence()
        .flatMap { it.resolvableErrors.asSequence() }
        .mapNotNull { it.defaultMessage }
        .firstOrNull()

    return ApiException(CommonError.BAD_REQUEST, detailMessage)
}

private fun ConstraintViolationException.toConstraintViolationApiException(): ApiException {
    val detailMessage = constraintViolations
        .asSequence()
        .map { violation ->
            val path = violation.propertyPath?.toString()?.substringAfterLast(".")
            if (path.isNullOrBlank()) violation.message else "$path: ${violation.message}"
        }
        .firstOrNull()

    return ApiException(CommonError.BAD_REQUEST, detailMessage)
}
