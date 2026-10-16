package com.snurdy.backend.global.exception

import com.snurdy.backend.global.constants.CommonError
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.dao.CannotAcquireLockException
import org.springframework.dao.PessimisticLockingFailureException
import java.sql.SQLTransactionRollbackException

class LockFailureMappingTest {

    @Test
    fun `데드락으로 발생한 CannotAcquireLockException은 SERVICE_UNAVAILABLE`() {
        val sqlEx = SQLTransactionRollbackException("Deadlock found when trying to get lock", "40001", 1213)
        val ex = CannotAcquireLockException("deadlock", sqlEx)
        assertEquals(CommonError.SERVICE_UNAVAILABLE, ex.toApiException().error)
    }

    @Test
    fun `락 대기 초과로 발생한 CannotAcquireLockException은 SERVICE_UNAVAILABLE`() {
        val sqlEx = SQLTransactionRollbackException("Lock wait timeout exceeded", "40001", 1205)
        val ex = CannotAcquireLockException("lock wait timeout", sqlEx)
        assertEquals(CommonError.SERVICE_UNAVAILABLE, ex.toApiException().error)
    }

    @Test
    fun `그 밖의 PessimisticLockingFailureException도 SERVICE_UNAVAILABLE`() {
        val ex = PessimisticLockingFailureException("pessimistic lock failure")
        assertEquals(CommonError.SERVICE_UNAVAILABLE, ex.toApiException().error)
    }
}