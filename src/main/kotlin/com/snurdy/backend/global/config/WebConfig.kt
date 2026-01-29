package com.snurdy.backend.global.config

import com.snurdy.backend.domain.user.entity.SocialType
import org.springframework.context.annotation.Configuration
import org.springframework.format.FormatterRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebConfig : WebMvcConfigurer {
    override fun addFormatters(registry: FormatterRegistry) {
        registry.addConverter(String::class.java, SocialType::class.java) {
                source -> SocialType.valueOf(source.uppercase())
        }
    }
}