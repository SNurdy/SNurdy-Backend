package com.snurdy.backend.domain.version.service

import com.snurdy.backend.domain.version.dto.UpdateType
import com.snurdy.backend.domain.version.dto.VersionCheckResponse
import com.snurdy.backend.domain.version.dto.toResponse
import com.snurdy.backend.domain.version.entity.Platform
import com.snurdy.backend.domain.version.repository.AppVersionRepository
import com.snurdy.backend.global.constants.VersionError
import com.snurdy.backend.global.exception.ApiException
import org.springframework.stereotype.Service

@Service
class VersionService(
    private val appVersionRepository: AppVersionRepository
) {

    fun checkVersion(clientVersion: String, platform: String): VersionCheckResponse {
        val parsedPlatform = parsePlatform(platform)
        val clientSemver = parseVersion(clientVersion)

        val appVersion = appVersionRepository.findByPlatform(parsedPlatform)
            ?: throw ApiException(VersionError.VERSION_INFO_NOT_FOUND)

        val minimumSemver = parseVersion(appVersion.minimumVersion)
        val latestSemver = parseVersion(appVersion.latestVersion)

        val updateType = when {
            clientSemver < minimumSemver -> UpdateType.FORCE_UPDATE
            clientSemver < latestSemver -> UpdateType.RECOMMEND_UPDATE
            else -> UpdateType.LATEST
        }

        return appVersion.toResponse(updateType)
    }

    private fun parsePlatform(platform: String): Platform =
        runCatching { Platform.valueOf(platform.trim().uppercase()) }
            .getOrElse { throw ApiException(VersionError.UNSUPPORTED_PLATFORM) }

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
}