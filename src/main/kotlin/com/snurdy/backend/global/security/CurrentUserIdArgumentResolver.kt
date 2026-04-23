package com.snurdy.backend.global.security

import com.snurdy.backend.global.constants.CommonError
import com.snurdy.backend.global.exception.ApiException
import org.springframework.core.MethodParameter
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer
import java.util.UUID

@Component
class CurrentUserIdArgumentResolver : HandlerMethodArgumentResolver {

    override fun supportsParameter(parameter: MethodParameter): Boolean {
        if (!parameter.hasParameterAnnotation(CurrentUserId::class.java)) return false
        if (parameter.parameterType != UUID::class.java) {
            throw IllegalStateException(
                "@CurrentUserId 파라미터 타입은 java.util.UUID 이어야 합니다. (found: ${parameter.parameterType})"
            )
        }
        return true
    }

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?
    ): UUID {
        val principal = SecurityContextHolder.getContext().authentication?.principal
        return when (principal) {
            is UUID -> principal
            is String -> runCatching { UUID.fromString(principal) }
                .getOrElse { throw ApiException(CommonError.UNAUTHORIZED) }
            else -> throw ApiException(CommonError.UNAUTHORIZED)
        }
    }
}
