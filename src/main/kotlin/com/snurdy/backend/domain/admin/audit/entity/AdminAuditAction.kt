package com.snurdy.backend.domain.admin.audit.entity

enum class AdminAuditAction {
    ADMIN_LOGIN_SUCCESS,
    ADMIN_LOGIN_FAILED,
    ADMIN_LOGOUT,
    REQUEST,
    UPDATE,
    VALIDATE,
    APPLY,
    CANCEL,
    ROLLBACK
}

object AdminAuditTarget {
    const val CHANGE_REQUESTS = "change_requests"
    const val ADMIN_USERS = "admin_users"
}
