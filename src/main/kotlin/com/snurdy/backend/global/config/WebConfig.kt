package com.snurdy.backend.global.config

import com.snurdy.backend.domain.user.account.entity.SocialType
import com.snurdy.backend.domain.user.wrongAnswerNote.dto.WrongAnswerPeriod
import com.snurdy.backend.global.constants.WrongAnswerNoteError
import com.snurdy.backend.global.exception.ApiException
import com.snurdy.backend.global.security.CurrentUserIdArgumentResolver
import org.springframework.context.annotation.Configuration
import org.springframework.format.FormatterRegistry
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebConfig(
    private val currentUserIdArgumentResolver: CurrentUserIdArgumentResolver
) : WebMvcConfigurer {
    override fun addFormatters(registry: FormatterRegistry) {
        registry.addConverter(String::class.java, SocialType::class.java) {
                source -> SocialType.valueOf(source.uppercase())
        }
        registry.addConverter(String::class.java, WrongAnswerPeriod::class.java) { source ->
            try {
                WrongAnswerPeriod.valueOf(source.uppercase())
            } catch (_: IllegalArgumentException) {
                throw ApiException(WrongAnswerNoteError.INVALID_PERIOD)
            }
        }
    }

    override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
        resolvers.add(currentUserIdArgumentResolver)
    }
}
