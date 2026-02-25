package com.snurdy.backend.domain.admin.workflow.entity

object ChangeRequestStatus {
    const val REQUESTED = "REQUESTED"
    const val VALIDATED = "VALIDATED"
    const val APPLIED = "APPLIED"
    const val FAILED = "FAILED"
    const val CANCELED = "CANCELED"
    const val ROLLED_BACK = "ROLLED_BACK"
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