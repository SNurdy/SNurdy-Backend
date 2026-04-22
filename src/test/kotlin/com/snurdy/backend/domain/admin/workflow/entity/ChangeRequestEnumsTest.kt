package com.snurdy.backend.domain.admin.workflow.entity

import com.snurdy.backend.global.constants.AdminError
import com.snurdy.backend.global.exception.ApiException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class ChangeRequestEnumsTest {

    @Test
    fun `fromRequestOrNull은 대소문자 무관하게 매핑되고, 빈값은 null`() {
        assertEquals(ChangeRequestStatus.VALIDATED, ChangeRequestStatus.fromRequestOrNull("validated"))
        assertEquals(ChangeRequestStatus.APPLIED, ChangeRequestStatus.fromRequestOrNull(" APPLIED "))
        assertNull(ChangeRequestStatus.fromRequestOrNull(null))
        assertNull(ChangeRequestStatus.fromRequestOrNull(""))
        assertNull(ChangeRequestStatus.fromRequestOrNull("unknown"))
    }

    @Test
    fun `EntityType fromRequest는 모르는 값에 대해 ApiException`() {
        val ex = assertThrows(ApiException::class.java) {
            ChangeRequestEntityType.fromRequest("UNKNOWN")
        }
        assertEquals(AdminError.INVALID_WORKFLOW_ENTITY_TYPE, ex.error)
    }

    @Test
    fun `Operation fromRequest는 모르는 값에 대해 ApiException`() {
        val ex = assertThrows(ApiException::class.java) {
            ChangeRequestOperation.fromRequest("PURGE")
        }
        assertEquals(AdminError.INVALID_WORKFLOW_OPERATION, ex.error)
    }
}
