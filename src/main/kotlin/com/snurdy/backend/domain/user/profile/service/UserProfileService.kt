package com.snurdy.backend.domain.user.profile.service

import com.snurdy.backend.domain.user.account.entity.UserEntity
import com.snurdy.backend.domain.user.account.repository.UserRepository
import com.snurdy.backend.domain.user.profile.dto.MarketingConsentResponse
import com.snurdy.backend.domain.user.profile.dto.ProfileImageUploadResponse
import com.snurdy.backend.domain.user.profile.dto.UserProfileResponse
import com.snurdy.backend.domain.user.profile.dto.UserProfileUpdateRequest
import com.snurdy.backend.domain.user.profile.dto.toMarketingConsentResponse
import com.snurdy.backend.domain.user.profile.dto.toProfileResponse
import com.snurdy.backend.global.constants.ProfileImageError
import com.snurdy.backend.global.constants.UserError
import com.snurdy.backend.global.exception.ApiException
import com.snurdy.backend.global.storage.S3Properties
import com.snurdy.backend.global.storage.S3StorageService
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.time.Clock
import java.util.UUID

@Service
class UserProfileService(
    private val userRepository: UserRepository,
    private val s3StorageService: S3StorageService,
    private val s3Properties: S3Properties,
    private val clock: Clock
) {

    @Transactional(readOnly = true)
    fun getProfile(userId: UUID): UserProfileResponse =
        findActiveUser(userId).toProfileResponse()

    @Transactional
    fun updateProfile(userId: UUID, request: UserProfileUpdateRequest): UserProfileResponse {
        val user = findActiveUser(userId)

        request.nickname?.let { user.updateNickname(it) }
        request.profileImageUrl?.let { user.updateProfileImageUrl(it) }

        return user.toProfileResponse()
    }

    @Transactional
    fun updateMarketingConsent(userId: UUID, agreed: Boolean): MarketingConsentResponse {
        val user = findActiveUser(userId)
        user.setMarketingConsent(agreed, clock.instant())

        return user.toMarketingConsentResponse()
    }

    @Transactional
    fun uploadProfileImage(userId: UUID, image: MultipartFile?): ProfileImageUploadResponse {
        if (image == null || image.isEmpty) throw ApiException(ProfileImageError.REQUIRED)
        if (image.size > s3Properties.profileImageMaxSizeBytes) throw ApiException(ProfileImageError.TOO_LARGE)
        val extension = resolveAllowedExtension(image)

        val user = findActiveUser(userId)
        val previousImageUrl = user.profileImageUrl

        val filename = "${userId}/${UUID.randomUUID()}.${extension}"
        val url = s3StorageService.upload(
            domainFolder = PROFILE_IMAGE_FOLDER,
            filename = filename,
            inputStream = image.inputStream,
            contentType = image.contentType ?: "image/${extension}"
        )
        user.updateProfileImageUrl(url)
        s3StorageService.deleteByUrl(previousImageUrl)

        return ProfileImageUploadResponse(profileImageUrl = url)
    }

    private fun resolveAllowedExtension(image: MultipartFile): String {
        val contentType = image.contentType?.lowercase()
        val originalName = image.originalFilename?.lowercase().orEmpty()
        val byContentType = when (contentType) {
            "image/jpeg" -> "jpg"
            "image/png" -> "png"
            else -> null
        }
        val byExtension = when {
            originalName.endsWith(".jpg") -> "jpg"
            originalName.endsWith(".jpeg") -> "jpg"
            originalName.endsWith(".png") -> "png"
            else -> null
        }
        // contentType 과 확장자 둘 다 확인. 하나라도 허용 외면 거부
        if (byContentType == null || byExtension == null || byContentType != byExtension) {
            throw ApiException(ProfileImageError.INVALID_FORMAT)
        }
        return byContentType
    }

    private fun findActiveUser(userId: UUID): UserEntity {
        val user = userRepository.findByIdOrNull(userId)
            ?: throw ApiException(UserError.ACCOUNT_NOT_FOUND)
        if (user.isDeleted()) throw ApiException(UserError.ALREADY_DELETED)
        return user
    }

    companion object {
        private const val PROFILE_IMAGE_FOLDER = "profile-images"
    }
}
