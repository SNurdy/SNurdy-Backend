package com.snurdy.backend.global.exception

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.snurdy.backend.global.constants.AuthError
import com.snurdy.backend.global.constants.CommonError
import feign.FeignException
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.JwtException
import jakarta.validation.ConstraintViolationException
import java.net.URI
import java.sql.SQLException
import org.hibernate.JDBCException
import org.hibernate.exception.ConstraintViolationException as HibernateConstraintViolationException
import org.hibernate.exception.DataException as HibernateDataException
import org.hibernate.exception.LockAcquisitionException as HibernateLockAcquisitionException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.dao.OptimisticLockingFailureException
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
    is DataIntegrityViolationException -> this.toDataIntegrityApiException()
    is OptimisticLockingFailureException -> ApiException(CommonError.CONFLICT, cause = this)

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
    val host = runCatching { request()?.url()?.let { URI.create(it).host } }.getOrNull()?.lowercase()
    val error = when {
        host == null -> CommonError.INTERNAL_ERROR
        host.matchesHost("kakao.com") -> AuthError.KAKAO_API_ERROR
        host.matchesHost("apple.com") -> AuthError.APPLE_API_ERROR
        host.matchesHost("googleapis.com") -> AuthError.GOOGLE_API_ERROR
        else -> CommonError.INTERNAL_ERROR
    }
    // 기본 메시지 사용
    return ApiException(error, cause = this)
}

private fun String.matchesHost(suffix: String): Boolean =
    this == suffix || endsWith(".$suffix")

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

// DataIntegrityViolation 은 원인별로 409(중복) / 400(FK·NOT NULL·형식·길이) / 409(락) / 500(그 외)으로 분류.
// ConstraintViolation 만이 아니라 Hibernate JDBCException 계열 전체에서 sqlState/errorCode 를 읽는다.
// 상세 메시지는 보안상 노출하지 않고 유형만 알려준다.
private fun DataIntegrityViolationException.toDataIntegrityApiException(): ApiException {
    val classification = classifySqlCause(this)

    return when (classification) {
        SqlClassification.DUPLICATE -> ApiException(CommonError.DUPLICATED, cause = this)
        SqlClassification.FK_VIOLATION -> ApiException(CommonError.INVALID_REFERENCE, cause = this)
        SqlClassification.NOT_NULL_VIOLATION -> ApiException(CommonError.BAD_REQUEST, "필수 값이 누락되었습니다", this)
        SqlClassification.DATA_FORMAT_OR_RANGE -> ApiException(CommonError.BAD_REQUEST, "값의 형식 또는 길이가 올바르지 않습니다", this)
        SqlClassification.LOCK_CONFLICT -> ApiException(CommonError.CONFLICT, cause = this)
        SqlClassification.OTHER_CONSTRAINT -> ApiException(CommonError.CONFLICT, cause = this)
        SqlClassification.UNKNOWN -> ApiException(CommonError.INTERNAL_ERROR, cause = this)
    }
}

private enum class SqlClassification {
    DUPLICATE,
    FK_VIOLATION,
    NOT_NULL_VIOLATION,
    DATA_FORMAT_OR_RANGE,
    LOCK_CONFLICT,
    OTHER_CONSTRAINT,
    UNKNOWN
}

private fun classifySqlCause(root: Throwable): SqlClassification {
    // 원인 체인에서 Hibernate JDBCException 우선 → 없으면 표준 SQLException
    val (sqlState, errorCode) = generateSequence<Throwable>(root) { it.cause }
        .mapNotNull { cause ->
            when (cause) {
                is JDBCException -> cause.sqlState to cause.errorCode
                is SQLException -> cause.sqlState to cause.errorCode
                else -> null
            }
        }
        .firstOrNull() ?: return SqlClassification.UNKNOWN

    // Hibernate 고수준 타입도 존재하면 유형 힌트로 사용
    val hintType = generateSequence<Throwable>(root) { it.cause }.firstOrNull {
        it is HibernateConstraintViolationException ||
            it is HibernateDataException ||
            it is HibernateLockAcquisitionException
    }

    return when {
        // 중복 키
        errorCode in setOf(1062, 1169) -> SqlClassification.DUPLICATE

        // FK 위반
        errorCode in setOf(1451, 1452, 1216, 1217) -> SqlClassification.FK_VIOLATION

        // NOT NULL / 기본값 없음
        errorCode in setOf(1048, 1364) -> SqlClassification.NOT_NULL_VIOLATION

        // 락 경합 / 교착상태 → 재시도 가능
        errorCode in setOf(1205, 1213) || hintType is HibernateLockAcquisitionException ->
            SqlClassification.LOCK_CONFLICT

        // 데이터 길이/형식/범위 (SQLState 22xxx 및 MySQL 1406 "data too long" / 1264 "out of range" / 1292 "incorrect value")
        sqlState?.startsWith("22") == true ||
            errorCode in setOf(1264, 1265, 1292, 1366, 1406) ||
            hintType is HibernateDataException ->
            SqlClassification.DATA_FORMAT_OR_RANGE

        // 그 외 제약조건 (SQLState 23xxx)
        sqlState?.startsWith("23") == true || hintType is HibernateConstraintViolationException ->
            SqlClassification.OTHER_CONSTRAINT

        else -> SqlClassification.UNKNOWN
    }
}
