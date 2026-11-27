package com.snurdy.backend.domain.migration.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class MigrationFailureEventListener(
    private val migrationSessionFailureService: MigrationSessionFailureService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
    fun onMigrationFailure(event: MigrationFailureEvent) {
        runCatching {
            migrationSessionFailureService.markFailed(event.sessionId, event.reason)
        }.onSuccess { failed ->
            // 실패 세션이 쌓여도 문의 전까지 알 수 없었으므로 커밋된 FAILED 전환을 error 로 남겨 Sentry 알림을 받음
            // 학습 내용과 사용자 식별 정보는 넣지 않고 세션 ID 와 ErrorCode 메시지만 남김
            if (failed) {
                log.error("마이그레이션 FAILED sessionId={} reason={}", event.sessionId, event.reason)
            }
        }.onFailure {
            log.error("마이그레이션 FAILED 기록 실패 — sessionId={}", event.sessionId, it)
        }
    }
}
