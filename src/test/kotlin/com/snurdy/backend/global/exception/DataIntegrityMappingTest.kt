package com.snurdy.backend.global.exception

import com.snurdy.backend.global.constants.CommonError
import org.hibernate.exception.ConstraintViolationException as HibernateConstraintViolationException
import org.hibernate.exception.DataException as HibernateDataException
import org.hibernate.exception.LockAcquisitionException as HibernateLockAcquisitionException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.dao.DataIntegrityViolationException
import java.sql.SQLDataException
import java.sql.SQLException
import java.sql.SQLIntegrityConstraintViolationException
import java.sql.SQLTransactionRollbackException

class DataIntegrityMappingTest {

    @Test
    fun `MySQL 1062 중복키는 DUPLICATED`() {
        val ex = constraintViolation(sqlState = "23000", errorCode = 1062)
        assertEquals(CommonError.DUPLICATED, ex.toApiException().error)
    }

    @Test
    fun `MySQL 1452 FK 위반은 INVALID_REFERENCE`() {
        val ex = constraintViolation(sqlState = "23000", errorCode = 1452)
        assertEquals(CommonError.INVALID_REFERENCE, ex.toApiException().error)
    }

    @Test
    fun `MySQL 1048 NOT NULL 위반은 BAD_REQUEST`() {
        val ex = constraintViolation(sqlState = "23000", errorCode = 1048)
        assertEquals(CommonError.BAD_REQUEST, ex.toApiException().error)
    }

    @Test
    fun `SQLState 22xxx data 오류는 BAD_REQUEST (데이터 형식)`() {
        val sqlEx = SQLDataException("data too long", "22001", 1406)
        val hibernateEx = HibernateDataException("data", sqlEx, "sql")
        val ex = DataIntegrityViolationException("wrap", hibernateEx)
        val api = ex.toApiException()
        assertEquals(CommonError.BAD_REQUEST, api.error)
        assertEquals("값의 형식 또는 길이가 올바르지 않습니다", api.responseMessage)
    }

    @Test
    fun `MySQL 1406 data too long 은 BAD_REQUEST`() {
        val sqlEx = SQLException("data too long", "HY000", 1406)
        val genericHibernateEx = org.hibernate.exception.GenericJDBCException("msg", sqlEx, "sql")
        val ex = DataIntegrityViolationException("wrap", genericHibernateEx)
        assertEquals(CommonError.BAD_REQUEST, ex.toApiException().error)
    }

    @Test
    fun `LockAcquisitionException은 CONFLICT`() {
        val sqlEx = SQLTransactionRollbackException("deadlock", "40001", 1213)
        val hibernateEx = HibernateLockAcquisitionException("lock", sqlEx, "sql")
        val ex = DataIntegrityViolationException("wrap", hibernateEx)
        assertEquals(CommonError.CONFLICT, ex.toApiException().error)
    }

    @Test
    fun `다른 23xxx ConstraintViolation은 CONFLICT`() {
        val ex = constraintViolation(sqlState = "23999", errorCode = 9999)
        assertEquals(CommonError.CONFLICT, ex.toApiException().error)
    }

    @Test
    fun `원인 체인에 SQLException이 없으면 INTERNAL_ERROR`() {
        val ex = DataIntegrityViolationException("unknown", RuntimeException("no hibernate cause"))
        assertEquals(CommonError.INTERNAL_ERROR, ex.toApiException().error)
    }

    private fun constraintViolation(sqlState: String, errorCode: Int): DataIntegrityViolationException {
        val sqlEx = SQLIntegrityConstraintViolationException("msg", sqlState, errorCode)
        val hibernateEx = HibernateConstraintViolationException("msg", sqlEx, "constraint_name")
        return DataIntegrityViolationException("wrap", hibernateEx)
    }
}
