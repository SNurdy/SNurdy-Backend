package com.snurdy.backend.global.storage

import io.awspring.cloud.s3.ObjectMetadata
import io.awspring.cloud.s3.S3Operations
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.NoSuchKeyException
import java.io.InputStream

// S3 업로드/삭제 wrapper
// 도메인 (profile-images 등) 별 호출자가 폴더명 + filename 만 전달
// 환경 prefix 는 S3Properties 가 자동 prepend
@Service
class S3StorageService(
    private val s3Operations: S3Operations,
    private val s3Client: S3Client,
    private val s3Properties: S3Properties
) {

    // 업로드 후 public URL 반환. ACL 비활성 환경이라 bucket policy 가 public-read 부여
    fun upload(
        domainFolder: String,
        filename: String,
        inputStream: InputStream,
        contentType: String
    ): String {
        val key = s3Properties.resolveKey(domainFolder, filename)
        val metadata = ObjectMetadata.builder()
            .contentType(contentType)
            .build()
        val resource = s3Operations.upload(s3Properties.bucket, key, inputStream, metadata)
        return resource.url.toString()
    }

    // 같은 버킷 소속 URL 만 삭제, 외부 URL 은 skip
    // 객체 없거나 권한 없어도 best effort (예외 삼킴)
    fun deleteByUrl(url: String?) {
        if (url.isNullOrBlank()) return
        val key = extractKey(url) ?: return
        runCatching {
            s3Client.deleteObject(
                DeleteObjectRequest.builder()
                    .bucket(s3Properties.bucket)
                    .key(key)
                    .build()
            )
        }.onFailure { e ->
            if (e !is NoSuchKeyException) {
                // 인프라 장애 무시 (best effort). 운영 시 로깅 추가 검토
            }
        }
    }

    // S3 URL 에서 key 추출. 다른 버킷 URL 이면 null
    private fun extractKey(url: String): String? {
        val bucket = s3Properties.bucket
        val pathStyle = "/${bucket}/"
        val virtualHosted = "${bucket}.s3."
        return when {
            url.contains(virtualHosted) -> url.substringAfter(".amazonaws.com/", "").ifBlank { null }
            url.contains(pathStyle) -> url.substringAfter(pathStyle, "").ifBlank { null }
            else -> null
        }
    }
}
