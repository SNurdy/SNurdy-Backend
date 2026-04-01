package com.snurdy.backend.domain.version.entity

import com.snurdy.backend.global.entity.BaseTimeEntity
import jakarta.persistence.*

@Entity
@Table(name = "app_versions")
class AppVersionEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, unique = true)
    val platform: Platform,

    @Column(name = "latest_version", nullable = false, length = 20)
    var latestVersion: String,

    @Column(name = "minimum_version", nullable = false, length = 20)
    var minimumVersion: String,

    @Column(name = "update_message", length = 500)
    var updateMessage: String? = null,

    @Column(name = "force_update_message", length = 500)
    var forceUpdateMessage: String? = null,

    @Column(name = "store_url", length = 500)
    var storeUrl: String? = null

) : BaseTimeEntity()