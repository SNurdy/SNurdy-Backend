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

    /**
     * 업로드 트랜잭션 ROLLBACK 후에만 호출된다.
     * `MigrationSessionFailureService.markFailed` 는 REQUIRES_NEW 로 선언되어 별도 트랜잭션에서 커밋된다.
     * 실패 시 애플리케이션 요청 흐름을 방해하지 않도록 예외를 삼키되 로깅.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
    fun onMigrationFailure(event: MigrationFailureEvent) {
        runCatching {
            migrationSessionFailureService.markFailed(event.sessionId, event.reason)
        }.onFailure {
            log.error("migration failure 상태 저장 실패: sessionId={}", event.sessionId, it)
        }
    }
}
