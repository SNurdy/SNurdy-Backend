package com.snurdy.backend.global.storage

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(S3Properties::class)
class StorageConfig
