package com.snurdy.backend.domain.test.common.repository

import com.snurdy.backend.domain.test.common.entity.TestSubmissionEntity
import com.snurdy.backend.domain.test.common.entity.TestSubmissionId
import org.springframework.data.jpa.repository.JpaRepository

interface TestSubmissionRepository : JpaRepository<TestSubmissionEntity, TestSubmissionId>