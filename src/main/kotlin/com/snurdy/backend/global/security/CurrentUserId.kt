package com.snurdy.backend.global.security

/**
 * 컨트롤러 파라미터에 붙이면 `SecurityContextHolder.getContext().authentication.principal`를
 * UUID로 해석해 주입한다. 미인증 상태면 `ApiException(CommonError.UNAUTHORIZED)` 를 던진다.
 *
 * Spring 의 `@AuthenticationPrincipal`을 얇게 감싼 도메인 친화 어노테이션.
 * JWT 필터가 항상 principal=UUID 로 세팅하므로 추가 변환 없이 직접 바인딩 가능.
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class CurrentUserId
