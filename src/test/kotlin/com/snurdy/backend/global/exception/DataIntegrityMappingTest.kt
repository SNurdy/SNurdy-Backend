package com.snurdy.backend.global.exception

import com.snurdy.backend.global.constants.CommonError
import org.hibernate.exception.ConstraintViolationException as HibernateConstraintViolationException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.dao.DataIntegrityViolationException
import java.sql.SQLIntegrityConstraintViolationException

class DataIntegrityMappingTest {

    @Test
    fun `MySQL 1062 중복키는 DUPLICATED`() {
        val ex = dataIntegrityException(sqlState = "23000", errorCode = 1062)
        val api = ex.toApiException()
        assertEquals(CommonError.DUPLICATED, api.error)
    }

    @Test
    fun `MySQL 1452 FK 위반은 INVALID_REFERENCE`() {
        val ex = dataIntegrityException(sqlState = "23000", errorCode = 1452)
        val api = ex.toApiException()
        assertEquals(CommonError.INVALID_REFERENCE, api.error)
    }

    @Test
    fun `MySQL 1048 NOT NULL 위반은 BAD_REQUEST`() {
        val ex = dataIntegrityException(sqlState = "23000", errorCode = 1048)
        val api = ex.toApiException()
        assertEquals(CommonError.BAD_REQUEST, api.error)
    }

    @Test
    fun `알 수 없는 원인은 INTERNAL_ERROR`() {
        val ex = DataIntegrityViolationException("unknown", RuntimeException("no hibernate cause"))
        val api = ex.toApiException()
        assertEquals(CommonError.INTERNAL_ERROR, api.error)
    }

    private fun dataIntegrityException(sqlState: String, errorCode: Int): DataIntegrityViolationException {
        val sqlEx = SQLIntegrityConstraintViolationException("msg", sqlState, errorCode)
        val hibernateEx = HibernateConstraintViolationException("msg", sqlEx, "constraint_name")
        return DataIntegrityViolationException("wrap", hibernateEx)
    }
}
