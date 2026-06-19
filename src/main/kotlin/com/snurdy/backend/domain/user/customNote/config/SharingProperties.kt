package com.snurdy.backend.domain.user.customNote.config

import org.springframework.boot.context.properties.ConfigurationProperties

// 공유 단어장 URL 생성 설정. iOS Universal Link 동작용 prefix
@ConfigurationProperties(prefix = "sharing")
data class SharingProperties(
    val customNoteUrlPrefix: String = "https://snurdy.org/shared/"
) {
    fun buildShareUrl(shareCode: String): String = "$customNoteUrlPrefix$shareCode"
}
