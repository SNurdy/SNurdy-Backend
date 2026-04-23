package com.snurdy.backend.domain.migration.service

import java.util.UUID

/**
 * 업로드 트랜잭션이 롤백될 때 세션을 FAILED 상태로 기록하기 위한 이벤트.
 * 게시 자체는 `ApplicationEventPublisher.publishEvent(...)` 로 수행하며,
 * 처리는 `MigrationFailureEventListener` 의 @TransactionalEventListener(phase = AFTER_ROLLBACK) 에서 별도 트랜잭션(REQUIRES_NEW)으로 수행한다.
 */
data class MigrationFailureEvent(
    val sessionId: UUID,
    val reason: String
)
