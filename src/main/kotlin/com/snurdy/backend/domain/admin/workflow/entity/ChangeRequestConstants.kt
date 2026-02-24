package com.snurdy.backend.domain.admin.workflow.entity

object ChangeRequestStatus {
    // 요청됨 — 검증 전
    const val REQUESTED = "REQUESTED"

    // 검증완료 — 반영 대기
    const val VALIDATED = "VALIDATED"

    // 반영완료 — dev DB 적용됨
    const val APPLIED = "APPLIED"

    // 검증실패 — 수정 후 재검증 가능
    const val FAILED = "FAILED"

    //  취소됨
    const val CANCELED = "CANCELED"
}

object ChangeRequestEntityType {
    const val TERM = "TERM"
    const val MORPHEME = "MORPHEME"

    val ALL = setOf(TERM, MORPHEME)
}

object ChangeRequestOperation {
    const val CREATE = "CREATE"
    const val UPDATE = "UPDATE"
    const val DELETE = "DELETE"

    val ALL = setOf(CREATE, UPDATE, DELETE)
}