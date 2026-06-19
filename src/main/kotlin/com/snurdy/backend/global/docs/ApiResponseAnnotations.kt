package com.snurdy.backend.global.docs

// 이 endpoint 가 실제로 던질 수 있는 도메인 ErrorCode 들을 명시한다
// OperationCustomizer 가 어노테이션을 읽어 Swagger 응답 example 을 자동 생성한다
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class ApiErrorCodes(vararg val codes: String)

// 이 endpoint 가 성공 시 사용하는 SuccessCode 를 명시한다
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class ApiSuccessCode(val code: String)
