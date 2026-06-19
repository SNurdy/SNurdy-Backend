package com.snurdy.backend.domain.version.service

import com.snurdy.backend.domain.version.config.AppVersionProperties
import com.snurdy.backend.domain.version.dto.UpdateType
import com.snurdy.backend.domain.version.dto.VersionCheckResponse
import com.snurdy.backend.domain.version.dto.toResponse
import com.snurdy.backend.global.constants.VersionError
import com.snurdy.backend.global.exception.ApiException
import org.springframework.stereotype.Service

@Service
class VersionService(
    private val appVersionProperties: AppVersionProperties
) {

    fun checkVersion(clientVersion: String, platform: String): VersionCheckResponse {
        validatePlatform(platform)
        val clientSemver = parseVersion(clientVersion)
        val minimumSemver = parseVersion(appVersionProperties.minimum)
        val latestSemver = parseVersion(appVersionProperties.latest)

        val updateType = when {
            clientSemver < minimumSemver -> UpdateType.FORCE_UPDATE
            clientSemver < latestSemver -> UpdateType.RECOMMEND_UPDATE
            else -> UpdateType.LATEST
        }

        return appVersionProperties.toResponse(updateType)
    }

    private fun validatePlatform(platform: String) {
        if (platform.trim().uppercase() != SUPPORTED_PLATFORM) {
            throw ApiException(VersionError.UNSUPPORTED_PLATFORM)
        }
    }

    private fun parseVersion(version: String): SemanticVersion {
        val parts = version.split(".")
        if (parts.size != 3 || parts.any { it.toIntOrNull() == null }) {
            throw ApiException(VersionError.INVALID_VERSION_FORMAT)
        }
        return SemanticVersion(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
    }

    private data class SemanticVersion(
        val major: Int,
        val minor: Int,
        val patch: Int
    ) : Comparable<SemanticVersion> {
        override fun compareTo(other: SemanticVersion): Int =
            compareValuesBy(this, other, { it.major }, { it.minor }, { it.patch })
    }

    companion object {
        private const val SUPPORTED_PLATFORM = "IOS"
    }
}
