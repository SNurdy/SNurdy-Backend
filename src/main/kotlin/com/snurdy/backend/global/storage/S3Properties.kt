package com.snurdy.backend.global.storage

import org.springframework.boot.context.properties.ConfigurationProperties

// storage.s3.* 환경 변수 binding
// bucket 은 환경 변수 AWS_S3_BUCKET 으로 환경별 주입
// object-prefix 는 local 에서만 "local/" 로 격리, dev/prod 는 빈 문자열
@ConfigurationProperties(prefix = "storage.s3")
data class S3Properties(
    val bucket: String,
    val objectPrefix: String = "",
    val profileImageMaxSizeBytes: Long = DEFAULT_PROFILE_IMAGE_MAX_SIZE_BYTES
) {

    fun resolveKey(domainFolder: String, filename: String): String =
        "${objectPrefix}${domainFolder}/${filename}"

    companion object {
        private const val DEFAULT_PROFILE_IMAGE_MAX_SIZE_BYTES: Long = 5 * 1024 * 1024
    }
}
